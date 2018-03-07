/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.aws.cert;

import com.google.common.collect.ImmutableList;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonReplacementsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.List;

import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AwsCertControllerTest extends ControllerTestsBase {

    @Autowired
    private AsyncRestTemplate rest;

    private MockRestServiceServer mockAws;

    @BeforeEach
    public void setup() throws Exception {
        super.setup();

        mockAws = MockRestServiceServer.bindTo(rest)
                .ignoreExpectOrder(true)
                .build();
    }

    @Test
    void testProtectedResource() throws Exception {
        testProtectedResource(POST, "/cards/requests");
        testProtectedResource(POST, "/api/v1/approve");
    }

    @Test
    void testDiscovery() throws Exception {
        testConnectorDiscovery();
    }

    @Test
    void testRegex() throws Exception {
        List<String> expected = ImmutableList.of(
                "https://test-aws-region.certificates.fake-amazon.com/approvals?code=test-auth-code&context=test-context"
        );
        testRegex("approval_urls", fromFile("awscert/fake/certificate-request-email.txt"), expected);
    }

    private MockHttpServletRequestBuilder setupPostRequest(
            String path,
            MediaType contentType,
            String requestFile
    ) throws Exception {
        return setupPostRequest(path, contentType, requestFile, null);
    }

    private MockHttpServletRequestBuilder setupPostRequest(
            String path,
            MediaType contentType,
            String requestFile,
            String language
    ) throws Exception {

        MockHttpServletRequestBuilder builder = post(path)
                .with(token(accessToken()))
                .contentType(contentType)
                .accept(APPLICATION_JSON)
                .header("x-routing-prefix", "https://hero/connectors/aws-cert/")
                .content(fromFile("/awscert/requests/" + requestFile));

        if (language != null) {
            builder = builder.header(ACCEPT_LANGUAGE, language);
        }

        return builder;
    }

    private ResultActions requestCards(String requestFile) throws Exception {
        return requestCards(requestFile, null);
    }

    private ResultActions requestCards(String requestFile, String language) throws Exception {
        return perform(
                setupPostRequest(
                        "/cards/requests",
                        APPLICATION_JSON,
                        requestFile,
                        language
                )
        );
    }

    /////////////////////////////
    // Request Cards
    /////////////////////////////

    @Test
    void testRequestCardsSuccess() throws Exception {
        trainAwsCertForCards();

        requestCards("valid/cards/card.json")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(
                        content().string(
                                JsonReplacementsBuilder
                                        .from(fromFile("/awscert/responses/success/cards/card.json"))
                                        .buildForCards()
                        )
                );

        mockAws.verify();
    }

    @Test
    void testRequestCards404DoesNotError() throws Exception {
        mockAws.expect(requestTo("https://test-aws-region-1.certificates.fake-amazon.com/approvals?code=test-auth-code-1&context=test-context-1"))
                .andExpect(method(GET))
                .andRespond(withStatus(NOT_FOUND));

        mockAws.expect(requestTo("https://test-aws-region-2.certificates.fake-amazon.com/approvals?code=test-auth-code-2&context=test-context-2"))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("awscert/fake/approval-page-2.html"), TEXT_HTML));

        mockAws.expect(requestTo("https://certificates.FAKE-amazon.com/approvals?code=test-auth-code-3&context=test-context-3"))
                .andExpect(method(GET))
                .andRespond(withStatus(NOT_FOUND));

        requestCards("valid/cards/card.json")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(
                        content().string(
                                JsonReplacementsBuilder
                                        .from(fromFile("/awscert/responses/success/cards/single-card.json"))
                                        .buildForCards()
                        )
                );

        mockAws.verify();
    }

    @Test
    void testRequestCards400DoesNotError() throws Exception {
        mockAws.expect(requestTo("https://test-aws-region-1.certificates.fake-amazon.com/approvals?code=test-auth-code-1&context=test-context-1"))
                .andExpect(method(GET))
                .andRespond(withBadRequest());

        mockAws.expect(requestTo("https://test-aws-region-2.certificates.fake-amazon.com/approvals?code=test-auth-code-2&context=test-context-2"))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("awscert/fake/approval-page-2.html"), TEXT_HTML));

        mockAws.expect(requestTo("https://certificates.FAKE-amazon.com/approvals?code=test-auth-code-3&context=test-context-3"))
                .andExpect(method(GET))
                .andRespond(withBadRequest());

        requestCards("valid/cards/card.json")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(
                        content().string(
                                JsonReplacementsBuilder
                                        .from(fromFile("/awscert/responses/success/cards/single-card.json"))
                                        .buildForCards()
                        )
                );

        mockAws.verify();
    }

    private void trainAwsCertForCards() throws Exception {
        mockAws.expect(requestTo("https://test-aws-region-1.certificates.fake-amazon.com/approvals?code=test-auth-code-1&context=test-context-1"))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("awscert/fake/approval-page-1.html"), TEXT_HTML));

        mockAws.expect(requestTo("https://test-aws-region-2.certificates.fake-amazon.com/approvals?code=test-auth-code-2&context=test-context-2"))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("awscert/fake/approval-page-2.html"), TEXT_HTML));

        /*
         * This one isn't as realistic for AWS (you are requesting a
         * certificate for a specific region, however, I wanted to test that
         * we handle the situation where they configure the exact host.
         */
        mockAws.expect(requestTo("https://certificates.FAKE-amazon.com/approvals?code=test-auth-code-3&context=test-context-3"))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("awscert/fake/approval-page-3.html"), TEXT_HTML));
    }

    @Test
    void testRequestCardsLanguageXxSuccess() throws Exception {
        trainAwsCertForCards();

        requestCards("valid/cards/card.json", "xx")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(
                        content().string(
                                JsonReplacementsBuilder
                                        .from(fromFile("/awscert/responses/success/cards/card_xx.json"))
                                        .buildForCards()
                        )
                );

        mockAws.verify();
    }

    @Test
    void testRequestCardsEmptyApprovalUrlsSuccess() throws Exception {
        requestCards("valid/cards/empty-approval-urls.json")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(
                        content().string(
                                JsonReplacementsBuilder
                                        .from(fromFile("/awscert/responses/success/cards/empty-approval-urls.json"))
                                        .buildForCards()
                        )
                );
    }

    @Test
    void testRequestCardsEmptyTokens() throws Exception {
        requestCards("invalid/cards/empty-tokens.json")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(fromFile("/awscert/responses/error/cards/empty-tokens.json"), false));
    }

    @Test
    void testRequestCardsMissingTokens() throws Exception {
        requestCards("invalid/cards/missing-tokens.json")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(fromFile("/awscert/responses/error/cards/missing-tokens.json"), false));
    }

    /////////////////////////////
    // Approve Action
    /////////////////////////////

    @Test
    void testApproveActionSuccess() throws Exception {
        String fakeResponse = fromFile("/awscert/fake/approval-confirmation-page.html");

        MultiValueMap<String, String> expectedFormData = new LinkedMultiValueMap<>();
        expectedFormData.set("utf8", "\u2713");
        expectedFormData.set("authenticity_token", "test-csrf-token");
        expectedFormData.set("authenticity_token", "test-csrf-token");
        expectedFormData.set("validation_token", "test-validation-token");
        expectedFormData.set("context", "test-context");
        expectedFormData.set("commit", "I Approve");

        mockAws.expect(requestTo("https://test-aws-region.certificates.fake-amazon.com/approvals?code=test-auth-code&context=test-context"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_FORM_URLENCODED))
                .andExpect(MockRestRequestMatchers.content().formData(expectedFormData))
                .andRespond(withSuccess(fakeResponse, TEXT_HTML));

        perform(
                setupPostRequest(
                        "/api/v1/approve",
                        APPLICATION_FORM_URLENCODED,
                        "valid/actions/approve.form"
                )
        ).andExpect(status().isOk());

        mockAws.verify();
    }

    @Test
    void testApproveActionInvalidUrl() throws Exception {
        perform(
                setupPostRequest(
                        "/api/v1/approve",
                        APPLICATION_FORM_URLENCODED,
                        "valid/actions/invalid-url-approve.form"
                )
        ).andExpect(status().isBadRequest());

        mockAws.verify();
    }

}
