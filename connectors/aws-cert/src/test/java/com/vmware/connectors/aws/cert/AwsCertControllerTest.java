/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.aws.cert;

import com.google.common.collect.ImmutableList;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

class AwsCertControllerTest extends ControllerTestsBase {

    @ParameterizedTest
    @ValueSource(strings = {
            "/cards/requests",
            "/api/v1/approve"})
    void testProtectedResource(String uri) throws Exception {
        testProtectedResource(POST, uri);
    }

    @Test
    void testDiscovery() throws IOException {
        testConnectorDiscovery();
    }

    @Test
    void testRegex() throws Exception {
        List<String> expected = ImmutableList.of(
                "https://test-aws-region.certificates.fake-amazon.com/approvals?code=test-auth-code&context=test-context"
        );
        testRegex("approval_urls", fromFile("awscert/fake/certificate-request-email.txt"), expected);
    }

    private WebTestClient.ResponseSpec doPostRequest(
            String path,
            MediaType contentType,
            String requestFile
    ) throws Exception {
        return doPostRequest(path, contentType, requestFile, null);
    }

    private WebTestClient.ResponseSpec doPostRequest(
            String path,
            MediaType contentType,
            String requestFile,
            String language
    ) throws Exception {

        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(path)
                 .header("x-routing-prefix", "https://hero/connectors/aws-cert/")
                .headers(headers -> headers(headers, path))
                .contentType(contentType)
                .accept(APPLICATION_JSON)
                .syncBody(fromFile("/awscert/requests/" + requestFile).replace("${backend_host}", mockBackend.url("")));

        if (StringUtils.isNotBlank(language)) {
            spec = spec.header(ACCEPT_LANGUAGE, language);
        }

        return spec.exchange();
    }

    private WebTestClient.ResponseSpec requestCards(String requestFile) throws Exception {
        return requestCards(requestFile, null);
    }

    private WebTestClient.ResponseSpec requestCards(String requestFile, String language) throws Exception {
        return doPostRequest(
                "/cards/requests",
                APPLICATION_JSON,
                requestFile,
                language
        );
    }

    /////////////////////////////
    // Request Cards
    /////////////////////////////

    @ParameterizedTest(name = "{index} ==> Language=''{0}''")
    @DisplayName("Card request success cases")
    @CsvSource({
            ", /awscert/responses/success/cards/card.json",
            "xx, /awscert/responses/success/cards/card_xx.json"})
    void testRequestCardsSuccess(String lang, String responseFile) throws Exception {
        trainAwsCertForCards();

        String body = requestCards("valid/cards/card.json", lang)
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block();
        body = body.replaceAll("[a-z0-9]{40,}", "test-hash");
        assertThat(body, sameJSONAs(fromFile(responseFile)
                .replace("${backend_host}", mockBackend.url(""))).allowingAnyArrayOrdering());
    }

    @ParameterizedTest
    @EnumSource(
            value = HttpStatus.class,
            names = {"BAD_REQUEST", "NOT_FOUND"
            })
    void testRequestCardsBackend4xx(HttpStatus backendStatus) throws Exception {
        mockBackend.expect(requestTo("/approvals?code=test-auth-code-1&context=test-context-1"))
                .andExpect(method(GET))
                .andRespond(withStatus(backendStatus));

        mockBackend.expect(requestTo("/approvals?code=test-auth-code-2&context=test-context-2"))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("awscert/fake/approval-page-2.html"), TEXT_HTML));

        String body = requestCards("valid/cards/card.json")
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block();
        body = body.replaceAll("[a-z0-9]{40,}", "test-hash");
        assertThat(body, sameJSONAs(fromFile("/awscert/responses/success/cards/single-card.json")
                .replace("${backend_host}", mockBackend.url(""))).allowingAnyArrayOrdering());
    }

    private void trainAwsCertForCards() throws Exception {
        mockBackend.expect(requestTo("/approvals?code=test-auth-code-1&context=test-context-1"))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("awscert/fake/approval-page-1.html"), TEXT_HTML));

        mockBackend.expect(requestTo("/approvals?code=test-auth-code-2&context=test-context-2"))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("awscert/fake/approval-page-2.html"), TEXT_HTML));
    }

    @Test
    void testRequestCardsEmptyApprovalUrlsSuccess() throws Exception {
        requestCards("valid/cards/empty-approval-urls.json")
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("/awscert/responses/success/cards/no-results.json"));
    }

    @ParameterizedTest
    @DisplayName("Bad card request cases")
    @CsvSource({
            "invalid/cards/empty-tokens.json, /awscert/responses/success/cards/no-results.json",
            "invalid/cards/missing-tokens.json, /awscert/responses/success/cards/no-results.json"
    })
    void testRequestCardsInsufficientInput(String reqFile, String resFile) throws Exception {
        requestCards(reqFile)
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile(resFile));
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

        mockBackend.expect(requestTo("/approvals?code=test-auth-code&context=test-context"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
                .andExpect(MockRestRequestMatchers.content().formData(expectedFormData))
                .andRespond(withSuccess(fakeResponse, TEXT_HTML));

        doPostRequest(
                "/api/v1/approve",
                APPLICATION_FORM_URLENCODED,
                "valid/actions/approve.form"
        ).expectStatus().isOk();
    }

    @Test
    void testApproveActionInvalidUrl() throws Exception {
        doPostRequest(
                "/api/v1/approve",
                APPLICATION_FORM_URLENCODED,
                "valid/actions/invalid-url-approve.form"
        ).expectStatus().isBadRequest();
    }

}
