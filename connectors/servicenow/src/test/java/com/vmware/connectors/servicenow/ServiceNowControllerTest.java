/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow;

import com.vmware.connectors.mock.MockRestServiceServer;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonReplacementsBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.List;

import static com.vmware.connectors.test.JsonSchemaValidator.isValidHeroCardConnectorResponse;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ServiceNowControllerTest extends ControllerTestsBase {

    private static final String SNOW_AUTH_TOKEN = "test-GOOD-auth-token";

    private MockRestServiceServer mockServiceNow;

    @BeforeEach
    void init() throws Exception {
        super.setup();

        mockServiceNow = MockRestServiceServer.bindTo(requestHandlerHolder)
                .ignoreExpectOrder(true)
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/cards/requests",
            "/api/v1/tickets/1234/approve",
            "/api/v1/tickets/1234/reject"})
    void testProtectedResource(String uri) throws Exception {
        testProtectedResource(POST, uri);
    }

    @Test
    void testDiscovery() throws Exception {
        testConnectorDiscovery();
    }

    @Test
    void testRegex() throws Exception {
        List<String> expected = Arrays.asList(
                "REQ0010001",
                "REQ0010003",
//                "REQ0010000", // Should not match due to too many numbers
                "REQ0010005",
                "REQ0010007",
                "REQ0010009",
                "REQ0010010", // Should not match due to trailing "X"
                "REQ0010011",
                "REQ0010012", // Should not match due to leading "X"
                "REQ0010013",
                "REQ0010014", // Should not match due to leading "1"
                "REQ0010015",
                "REQ0010017",
                "REQ0010019",
                "REQ0010020", // Should not match due to trailing "X"
                "REQ0010021",
                "REQ0010023",
                "REQ0010024", // Should not match due to trailing "x"
                "REQ0010025",
                "REQ0010027",
                "REQ0010029"
//                "REQ0010030" // Should not match due to trailing "x"
        );
        testRegex("ticket_id", fromFile("/regex/email.txt"), expected);
    }

    private MockHttpServletRequestBuilder setupPostRequest(
            String path,
            MediaType contentType,
            String authToken,
            String requestFile
    ) throws Exception {
        return setupPostRequest(path, contentType, authToken, requestFile, null);
    }

    private MockHttpServletRequestBuilder setupPostRequest(
            String path,
            MediaType contentType,
            String authToken,
            String requestFile,
            String language
    ) throws Exception {

        MockHttpServletRequestBuilder builder = post(path)
                .with(token(accessToken()))
                .contentType(contentType)
                .accept(APPLICATION_JSON)
                .header("x-servicenow-base-url", "https://snow.acme.com")
                .header("x-routing-prefix", "https://hero/connectors/servicenow/")
                .content(fromFile("/servicenow/requests/" + requestFile));

        if (authToken != null) {
            builder = builder.header("x-servicenow-authorization", "Bearer " + authToken);
        }

        if (language != null) {
            builder = builder.header(ACCEPT_LANGUAGE, language);
        }

        return builder;
    }

    private ResultActions requestCards(String authToken, String requestFile) throws Exception {
        return requestCards(authToken, requestFile, null);
    }

    private ResultActions requestCards(String authToken, String requestFile, String language) throws Exception {
        return perform(
                setupPostRequest(
                        "/cards/requests",
                        APPLICATION_JSON,
                        authToken,
                        requestFile,
                        language
                )
        );
    }

    private ResultActions approve(String authToken, String requestFile) throws Exception {
        return perform(
                setupPostRequest(
                        "/api/v1/tickets/test-ticket-id/approve",
                        APPLICATION_FORM_URLENCODED,
                        authToken,
                        requestFile
                )
        );
    }

    private ResultActions reject(String authToken, String requestFile) throws Exception {
        return perform(
                setupPostRequest(
                        "/api/v1/tickets/test-ticket-id/reject",
                        APPLICATION_FORM_URLENCODED,
                        authToken,
                        requestFile
                )
        );
    }

    /////////////////////////////
    // Request Cards
    /////////////////////////////

    @Test
    void testRequestCardsUnauthorized() throws Exception {
        mockServiceNow.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        requestCards(SNOW_AUTH_TOKEN, "valid/cards/card.json")
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Backend-Status", "401"));

        mockServiceNow.verify();
    }

    @Test
    void testRequestCardsAuthHeaderMissing() throws Exception {
        requestCards(null, "valid/cards/card.json")
                .andExpect(status().isBadRequest());
    }

    @DisplayName("Card request success cases")
    @ParameterizedTest(name = "{index} ==> Language=''{0}''")
    @CsvSource({
            StringUtils.EMPTY + ", /servicenow/responses/success/cards/card.json",
            "xx, /servicenow/responses/success/cards/card_xx.json"})
    void testRequestCardsSuccess(String acceptLanguage, String responseFile) throws Exception {
        trainServiceNowForCards();

        requestCards(SNOW_AUTH_TOKEN, "valid/cards/card.json", acceptLanguage)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().string(isValidHeroCardConnectorResponse()))
                .andExpect(
                        content().string(
                                JsonReplacementsBuilder
                                        .from(fromFile(responseFile))
                                        .buildForCards()
                        )
                );
    }

    private void trainServiceNowForCards() throws Exception {
        mockServiceNow.expect(requestTo("https://snow.acme.com/api/now/table/sys_user?sysparm_fields=sys_id&sysparm_limit=1&email=jbard@vmware.com"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/user.json"), APPLICATION_JSON));

        mockServiceNow.expect(requestTo("https://snow.acme.com/api/now/table/sysapproval_approver?sysparm_fields=sys_id,sysapproval,comments,due_date,sys_created_by&sysparm_limit=10000&source_table=sc_request&state=requested&approver=test-user-id"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/approval-requests.json"), APPLICATION_JSON));

        mockServiceNow.expect(requestTo("https://snow.acme.com/api/now/table/sc_request/test-sc-request-id-1?sysparm_fields=sys_id,price,number"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/request-1.json"), APPLICATION_JSON));

        mockServiceNow.expect(requestTo("https://snow.acme.com/api/now/table/sc_request/test-sc-request-id-2?sysparm_fields=sys_id,price,number"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/request-2.json"), APPLICATION_JSON));

        mockServiceNow.expect(requestTo("https://snow.acme.com/api/now/table/sc_request/test-sc-request-id-3?sysparm_fields=sys_id,price,number"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/request-3.json"), APPLICATION_JSON));

        mockServiceNow.expect(requestTo("https://snow.acme.com/api/now/table/sc_req_item?sysparm_fields=sys_id,price,request,short_description,quantity&sysparm_limit=10000&request=test-sc-request-id-2"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/requested-items-2.json"), APPLICATION_JSON));

        mockServiceNow.expect(requestTo("https://snow.acme.com/api/now/table/sc_req_item?sysparm_fields=sys_id,price,request,short_description,quantity&sysparm_limit=10000&request=test-sc-request-id-3"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/requested-items-3.json"), APPLICATION_JSON));
    }

    @Test
    void testRequestCardsEmptyTicketsSuccess() throws Exception {
        requestCards(SNOW_AUTH_TOKEN, "valid/cards/empty-tickets.json")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().string(isValidHeroCardConnectorResponse()))
                .andExpect(
                        content().string(
                                JsonReplacementsBuilder
                                        .from(fromFile("/servicenow/responses/success/cards/empty-tickets.json"))
                                        .buildForCards()
                        )
                );
    }

    @DisplayName("Card request invalid token cases")
    @ParameterizedTest(name = "{index} ==> ''{0}''")
    @CsvSource({"valid/cards/missing-tickets.json, /servicenow/responses/success/cards/missing-tickets.json",
            "valid/cards/empty-email.json, /servicenow/responses/success/cards/empty-email.json"})
    void testRequestCardsInvalidTokens(String reqFile, String resFile) throws Exception {
        requestCards(SNOW_AUTH_TOKEN, reqFile)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(
                        content().string(
                                JsonReplacementsBuilder
                                        .from(fromFile(resFile))
                                        .buildForCards()
                        )
                );
    }

    @Test
    void testRequestCardsMissingEmailSuccess() throws Exception {
        requestCards(SNOW_AUTH_TOKEN, "valid/cards/missing-email.json")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(
                        content().string(
                                JsonReplacementsBuilder
                                        .from(fromFile("/servicenow/responses/success/cards/missing-email.json"))
                                        .buildForCards()
                        )
                );
    }

    @Test
    void testRequestCardsEmptyTokens() throws Exception {
        requestCards(SNOW_AUTH_TOKEN, "invalid/cards/empty-tokens.json")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(fromFile("/servicenow/responses/error/cards/empty-tokens.json"), false));
    }

    @Test
    void testRequestCardsMissingTokens() throws Exception {
        requestCards(SNOW_AUTH_TOKEN, "invalid/cards/missing-tokens.json")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(fromFile("/servicenow/responses/error/cards/missing-tokens.json"), false));
    }

    /////////////////////////////
    // Approve Action
    /////////////////////////////

    @Test
    void testApproveActionUnauthorized() throws Exception {
        mockServiceNow.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        approve(SNOW_AUTH_TOKEN, "valid/actions/approve.form")
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Backend-Status", "401"));

        mockServiceNow.verify();
    }

    @Test
    void testApproveAuthHeaderMissing() throws Exception {
        approve(null, "valid/actions/approve.form")
                .andExpect(status().isBadRequest());
    }

    @Test
    void testApproveActionSuccess() throws Exception {
        String fakeResponse = fromFile("/servicenow/fake/approve.json");

        String expected = fromFile("/servicenow/responses/success/actions/approve.json");

        mockServiceNow.expect(requestTo("https://snow.acme.com/api/now/table/sysapproval_approver/test-ticket-id?sysparm_fields=sys_id,state,comments"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(PATCH))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.state", is("approved")))
                .andRespond(withSuccess(fakeResponse, APPLICATION_JSON));

        approve(SNOW_AUTH_TOKEN, "valid/actions/approve.form")
                .andExpect(status().isOk())
                .andExpect(content().json(expected, false));

        mockServiceNow.verify();
    }

    /////////////////////////////
    // Reject Action
    /////////////////////////////

    @Test
    void testRejectActionUnauthorized() throws Exception {
        mockServiceNow.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        reject(SNOW_AUTH_TOKEN, "valid/actions/reject.form")
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Backend-Status", "401"));

        mockServiceNow.verify();
    }

    @Test
    void testRejectAuthHeaderMissing() throws Exception {
        reject(null, "valid/actions/reject.form")
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRejectActionSuccess() throws Exception {
        String fakeResponse = fromFile("/servicenow/fake/reject.json");

        String expected = fromFile("/servicenow/responses/success/actions/reject.json");

        mockServiceNow.expect(requestTo("https://snow.acme.com/api/now/table/sysapproval_approver/test-ticket-id?sysparm_fields=sys_id,state,comments"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(PATCH))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.state", is("rejected")))
                .andExpect(MockRestRequestMatchers.jsonPath("$.comments", is("because")))
                .andRespond(withSuccess(fakeResponse, APPLICATION_JSON));

        reject(SNOW_AUTH_TOKEN, "valid/actions/reject.form")
                .andExpect(status().isOk())
                .andExpect(content().json(expected, false));

        mockServiceNow.verify();
    }

    @Test
    void testRejectActionMissingReason() throws Exception {
        reject(SNOW_AUTH_TOKEN, "invalid/actions/reject/missing-reason.form")
                .andExpect(status().isBadRequest());
    }

}
