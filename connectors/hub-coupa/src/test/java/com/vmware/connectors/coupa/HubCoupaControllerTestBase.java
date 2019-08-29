/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.connectors.coupa;

import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

class HubCoupaControllerTestBase extends ControllerTestsBase {

    static final String CALLER_SERVICE_CREDS = "service-creds-from-http-request";
    static final String CONFIG_SERVICE_CREDS = "service-creds-from-config";

    private static final String AUTHORIZATION_HEADER_NAME = "X-COUPA-API-KEY";

    @ParameterizedTest
    @ValueSource(strings = {
            "/cards/requests",
            "/api/approve/123",
            "/api/reject/123"
    })
    void testProtectedResources(String uri) throws Exception {
        testProtectedResource(POST, uri);
    }

    @Test
    void testDiscovery() throws Exception {
        testConnectorDiscovery();
    }

    WebTestClient.ResponseSpec approveRequest(String authHeader) {
        String uri = "/api/approve/182964?comment=Approved";
        return webClient.post()
                .uri(uri)
                .header(AUTHORIZATION, "Bearer " + accessToken(uri))
                .header(X_AUTH_HEADER, authHeader)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .contentType(APPLICATION_FORM_URLENCODED)
                .exchange();
    }

    WebTestClient.ResponseSpec rejectRequest(String authHeader) {
        String uri = "/api/decline/182964?comment=Declined";
        return webClient.post().uri("/api/decline/182964?comment=Declined")
                .header(AUTHORIZATION, "Bearer " + accessToken(uri))
                .header(X_AUTH_HEADER, authHeader)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .contentType(APPLICATION_FORM_URLENCODED)
                .exchange();
    }

    WebTestClient.ResponseSpec cardsRequest(String lang, String authHeader) {
        String uri = "/cards/requests";
        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(uri)
                 .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(X_AUTH_HEADER, authHeader)
                .header("x-routing-prefix", "https://hero/connectors/coupa/")
                .headers(headers -> headers(headers, uri))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON);

        if (StringUtils.isNotBlank(lang)) {
            spec.header(ACCEPT_LANGUAGE, lang);
        }

        return spec.exchange();
    }

    void cardsRequest(String lang, String expected, String authHeader) throws Exception {
        String body = cardsRequest(lang, authHeader)
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block()
                .replaceAll("[0-9]{4}[-][0-9]{2}[-][0-9]{2}T[0-9]{2}[:][0-9]{2}[:][0-9]{2}Z?", "1970-01-01T00:00:00Z")
                .replaceAll("[a-z0-9]{40,}", "test-hash");

        assertThat(
                body,
                sameJSONAs(fromFile("connector/responses/" + expected))
                        .allowingAnyArrayOrdering()
                        .allowingExtraUnexpectedFields()
        );
    }

    void mockRejectActions(String serviceCredential) throws Exception {
        mockRequisitionDetails(serviceCredential);
        mockRejectAction(serviceCredential);
    }

    void mockApproveActions(String serviceCredential) throws Exception {
        mockRequisitionDetails(serviceCredential);
        mockApproveAction(serviceCredential);
    }

    void mockCoupaRequest(String serviceCredential) throws Exception {
        mockUserDetails(serviceCredential);
        mockApproval(serviceCredential);
        mockRequisitionDetails(serviceCredential);
    }

    void mockApproval(String serviceCredential) throws Exception {
        mockBackend.expect(requestTo("/api/approvals?approver_id=15882&status=pending_approval"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION_HEADER_NAME, serviceCredential))
                .andRespond(withSuccess(
                        fromFile("/fake/approvals.json").replace("${coupa_host}", mockBackend.url("")),
                        APPLICATION_JSON
                ));
    }

    void mockUserDetails(String serviceCredential) throws Exception {
        mockBackend.expect(requestTo("/api/users?email=admin%40acme.com"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION_HEADER_NAME, serviceCredential))
                .andRespond(withSuccess(
                        fromFile("/fake/user-details.json").replace("${coupa_host}", mockBackend.url("")),
                        APPLICATION_JSON
                ));
    }

    void mockApproveAction(String serviceCredential) {
        mockBackend.expect(requestTo("/api/approvals/6609559/approve?reason=Approved"))
                .andExpect(method(PUT))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION_HEADER_NAME, serviceCredential))
                .andRespond(withSuccess());
    }

    void mockRejectAction(String serviceCredential) {
        mockBackend.expect(requestTo("/api/approvals/6609559/reject?reason=Declined"))
                .andExpect(method(PUT))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION_HEADER_NAME, serviceCredential))
                .andRespond(withSuccess());
    }

    void mockRequisitionDetails(String serviceCredential) throws Exception {
        mockBackend.expect(requestTo("/api/requisitions?id=182964&status=pending_approval"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION_HEADER_NAME, serviceCredential))
                .andRespond(withSuccess(
                        fromFile("/fake/requisition-details.json").replace("${coupa_host}", mockBackend.url("")),
                        APPLICATION_JSON
                ));
    }

    void mockOtherRequisitionDetails(String serviceCredential) throws Exception {
        mockBackend.expect(requestTo("/api/requisitions?id=182964&status=pending_approval"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION_HEADER_NAME, serviceCredential))
                .andRespond(withSuccess(
                        fromFile("/fake/not-for-admin-at-acme-requisition-details.json").replace("${coupa_host}", mockBackend.url("")),
                        APPLICATION_JSON
                ));
    }


}
