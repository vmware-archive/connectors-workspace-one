/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.hub.servicenow;

import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

class HubServiceNowControllerTest extends ControllerTestsBase {

    private static final String SNOW_AUTH_TOKEN = "test-GOOD-auth-token";

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

    private WebTestClient.ResponseSpec doPost(
            String path,
            MediaType contentType,
            String authToken,
            String requestFile
    ) throws Exception {
        return doPost(path, contentType, authToken, requestFile, null);
    }

    private WebTestClient.ResponseSpec doPost(
            String path,
            MediaType contentType,
            String authToken,
            String requestFile,
            String language
    ) throws Exception {

        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(path)
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(contentType)
                .accept(APPLICATION_JSON)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/servicenow/")
                .headers(ControllerTestsBase::headers)
                .syncBody(fromFile("/servicenow/requests/" + requestFile));

        if (authToken != null) {
            spec = spec.header(X_AUTH_HEADER, "Bearer " + authToken);
        }

        if (language != null) {
            spec = spec.header(ACCEPT_LANGUAGE, language);
        }

        return spec.exchange();
    }

    private WebTestClient.ResponseSpec requestCards(String authToken, String requestFile) throws Exception {
        return requestCards(authToken, requestFile, null);
    }

    private WebTestClient.ResponseSpec requestCards(String authToken, String requestFile, String language) throws Exception {
        return doPost(
                        "/cards/requests",
                        APPLICATION_JSON,
                        authToken,
                        requestFile,
                        language
                );
    }

    private WebTestClient.ResponseSpec approve(String authToken, String requestFile) throws Exception {
        return doPost(
                        "/api/v1/tickets/test-ticket-id/approve",
                        APPLICATION_FORM_URLENCODED,
                        authToken,
                        requestFile
                );
    }

    private WebTestClient.ResponseSpec reject(String authToken, String requestFile) throws Exception {
        return doPost(
                        "/api/v1/tickets/test-ticket-id/reject",
                        APPLICATION_FORM_URLENCODED,
                        authToken,
                        requestFile
                );
    }

    /////////////////////////////
    // Request Cards
    /////////////////////////////

    @Test
    void testRequestCardsUnauthorized() throws Exception {
        mockBackend.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        requestCards(SNOW_AUTH_TOKEN, "valid/cards/card.json")
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401");
    }

    @Test
    void testRequestCardsAuthHeaderMissing() throws Exception {
        requestCards(null, "valid/cards/card.json")
                .expectStatus().isBadRequest();
    }

    @Test
    void testRequestCardsEmailNotFoundInServiceNow() throws Exception {
        mockBackend.expect(requestTo("/api/now/table/sys_user?sysparm_fields=sys_id&sysparm_limit=1&email=jbard@vmware.com"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/user-not-found.json"), APPLICATION_JSON));

        requestCards(SNOW_AUTH_TOKEN, "valid/cards/card.json")
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("/servicenow/responses/success/cards/email-not-found.json"));
     }

    @DisplayName("Card request success cases")
    @ParameterizedTest(name = "{index} ==> Language=''{0}''")
    @CsvSource({
            StringUtils.EMPTY + ", /servicenow/responses/success/cards/card.json",
            "xx, /servicenow/responses/success/cards/card_xx.json"})
    void testRequestCardsSuccess(String acceptLanguage, String responseFile) throws Exception {
        trainServiceNowForCards();

        String body = requestCards(SNOW_AUTH_TOKEN, "valid/cards/card.json", acceptLanguage)
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block();
        body = body.replaceAll("[a-z0-9]{40,}", "test-hash");

        assertThat(body, sameJSONAs(fromFile(responseFile)).allowingAnyArrayOrdering());
    }

    private void trainServiceNowForCards() throws Exception {
        mockBackend.expect(requestTo("/api/now/table/sys_user?sysparm_fields=sys_id&sysparm_limit=1&email=jbard@vmware.com"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/user.json"), APPLICATION_JSON));

        mockBackend.expect(requestTo("/api/now/table/sysapproval_approver?sysparm_fields=sys_id,sysapproval,comments,due_date,sys_created_by&sysparm_limit=10000&source_table=sc_request&state=requested&approver=test-user-id"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/approval-requests.json"), APPLICATION_JSON));

        mockBackend.expect(requestTo("/api/now/table/sc_request/test-sc-request-id-1?sysparm_fields=sys_id,price,number"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/request-1.json"), APPLICATION_JSON));

        mockBackend.expect(requestTo("/api/now/table/sc_request/test-sc-request-id-2?sysparm_fields=sys_id,price,number"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/request-2.json"), APPLICATION_JSON));

        mockBackend.expect(requestTo("/api/now/table/sc_request/test-sc-request-id-3?sysparm_fields=sys_id,price,number"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/request-3.json"), APPLICATION_JSON));

        mockBackend.expect(requestTo("/api/now/table/sc_req_item?sysparm_fields=sys_id,price,request,short_description,quantity&sysparm_limit=10000&request=test-sc-request-id-1"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/requested-items-1.json"), APPLICATION_JSON));

        mockBackend.expect(requestTo("/api/now/table/sc_req_item?sysparm_fields=sys_id,price,request,short_description,quantity&sysparm_limit=10000&request=test-sc-request-id-2"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/requested-items-2.json"), APPLICATION_JSON));

        mockBackend.expect(requestTo("/api/now/table/sc_req_item?sysparm_fields=sys_id,price,request,short_description,quantity&sysparm_limit=10000&request=test-sc-request-id-3"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("/servicenow/fake/requested-items-3.json"), APPLICATION_JSON));
    }

    @Test
    void testRequestCardsMissingEmailSuccess() throws Exception {
        requestCards(SNOW_AUTH_TOKEN, "valid/cards/missing-email.json")
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("/servicenow/responses/success/cards/missing-email.json"));
    }

    @Test
    void testRequestCardsEmptyTokens() throws Exception {
        requestCards(SNOW_AUTH_TOKEN, "invalid/cards/empty-tokens.json")
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("/servicenow/responses/error/cards/empty-tokens.json"));
    }

    @Test
    void testRequestCardsMissingTokens() throws Exception {
        requestCards(SNOW_AUTH_TOKEN, "invalid/cards/missing-tokens.json")
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("/servicenow/responses/error/cards/missing-tokens.json"));
    }

    /////////////////////////////
    // Approve Action
    /////////////////////////////

    @Test
    void testApproveActionUnauthorized() throws Exception {
        mockBackend.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        approve(SNOW_AUTH_TOKEN, "valid/actions/approve.form")
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401");
    }

    @Test
    void testApproveAuthHeaderMissing() throws Exception {
        approve(null, "valid/actions/approve.form")
                .expectStatus().isBadRequest();
    }

    @Test
    void testApproveActionSuccess() throws Exception {
        String fakeResponse = fromFile("/servicenow/fake/approve.json");

        String expected = fromFile("/servicenow/responses/success/actions/approve.json");

        mockBackend.expect(requestTo("/api/now/table/sysapproval_approver/test-ticket-id?sysparm_fields=sys_id,state,comments"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(PATCH))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.state", is("approved")))
                .andRespond(withSuccess(fakeResponse, APPLICATION_JSON));

        approve(SNOW_AUTH_TOKEN, "valid/actions/approve.form")
                .expectStatus().isOk()
                .expectBody().json(expected);
    }

    /////////////////////////////
    // Reject Action
    /////////////////////////////

    @Test
    void testRejectActionUnauthorized() throws Exception {
        mockBackend.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        reject(SNOW_AUTH_TOKEN, "valid/actions/reject.form")
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401");
    }

    @Test
    void testRejectAuthHeaderMissing() throws Exception {
        reject(null, "valid/actions/reject.form")
                .expectStatus().isBadRequest();
    }

    @Test
    void testRejectActionSuccess() throws Exception {
        String fakeResponse = fromFile("/servicenow/fake/reject.json");

        String expected = fromFile("/servicenow/responses/success/actions/reject.json");

        mockBackend.expect(requestTo("/api/now/table/sysapproval_approver/test-ticket-id?sysparm_fields=sys_id,state,comments"))
                .andExpect(header(AUTHORIZATION, "Bearer " + SNOW_AUTH_TOKEN))
                .andExpect(method(PATCH))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.state", is("rejected")))
                .andExpect(jsonPath("$.comments", is("because")))
                .andRespond(withSuccess(fakeResponse, APPLICATION_JSON));

        reject(SNOW_AUTH_TOKEN, "valid/actions/reject.form")
                .expectStatus().isOk()
                .expectBody().json(expected);
    }

    @Test
    void testRejectActionMissingReason() throws Exception {
        reject(SNOW_AUTH_TOKEN, "invalid/actions/reject/missing-reason.form")
                .expectStatus().isBadRequest();
    }

}
