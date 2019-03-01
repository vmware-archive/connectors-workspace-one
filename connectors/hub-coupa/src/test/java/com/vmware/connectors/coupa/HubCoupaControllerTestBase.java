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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

public class HubCoupaControllerTestBase extends ControllerTestsBase {

    protected static final String CALLER_SERVICE_CREDS = "service-creds-from-http-request";
    protected static final String CONFIG_SERVICE_CREDS = "service-creds-from-config";

    protected static final String AUTHORIZATION_HEADER_NAME = "X-COUPA-API-KEY";

    @Value("${coupa.api-key}")
    private String serviceCredential;

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

    @Test
    void testGetImage() {
        webClient.get()
                .uri("/images/connector.png")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentLength(16375)
                .expectHeader().contentType(IMAGE_PNG_VALUE)
                .expectBody()
                .consumeWith(body -> assertThat(body.getResponseBody(), equalTo(bytesFromFile("/static/images/connector.png"))));
    }

    @ParameterizedTest
    @CsvSource({
            ", success.json",
            "xx, success_xx.json"
    })
    void testCardsRequests(String lang, String expected) throws Exception {
        if (StringUtils.isBlank(serviceCredential)) {
            serviceCredential = CALLER_SERVICE_CREDS;
        }

        mockCoupaRequest(serviceCredential);

        final String body = testCardsRequest(lang, serviceCredential)
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

    protected WebTestClient.ResponseSpec testApproveRequest(final String authHeader) {
        WebTestClient.RequestBodySpec spec = webClient.post()
                .uri("/api/approve/{id}?comment=Approved", "182964")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_BASE_URL_HEADER, mockBackend.url(""));

        return setAuthHeaderAndExchange(authHeader, spec);
    }

    protected WebTestClient.ResponseSpec testRejectRequest(final String authHeader) {
        WebTestClient.RequestBodySpec spec = webClient.post().uri("/api/decline/{id}?comment=Declined", "182964")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_BASE_URL_HEADER, mockBackend.url(""));

        return setAuthHeaderAndExchange(authHeader, spec);
    }

    private WebTestClient.ResponseSpec setAuthHeaderAndExchange(String authHeader, WebTestClient.RequestBodySpec spec) {
        if (StringUtils.isNotBlank(authHeader)) {
            spec.header(X_AUTH_HEADER, authHeader);
        }

        return spec.contentType(APPLICATION_FORM_URLENCODED)
                .exchange();
    }

    protected WebTestClient.ResponseSpec testCardsRequest(final String lang, final String authHeader) {
        WebTestClient.RequestBodySpec spec = webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/coupa/")
                .headers(ControllerTestsBase::headers)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON);

        if (StringUtils.isNotBlank(authHeader)) {
            spec.header(X_AUTH_HEADER, authHeader);
        }

        if (StringUtils.isNotBlank(lang)) {
            spec.header(ACCEPT_LANGUAGE, lang);
        }

        return spec.exchange();
    }

    protected void mockRejectActions(final String serviceCredential) throws Exception {
        mockRequisitionDetails(serviceCredential);
        mockRejectAction(serviceCredential);
    }

    protected void mockApproveActions(final String serviceCredential) throws Exception {
        mockRequisitionDetails(serviceCredential);
        mockApproveAction(serviceCredential);
    }

    protected void mockCoupaRequest(final String serviceCredential) throws Exception {
        mockUserDetails(serviceCredential);
        mockApproval(serviceCredential);
        mockRequisitionDetails(serviceCredential);
    }

    protected void mockApproval(final String serviceCredential) throws Exception {
        mockBackend.expect(requestTo("/api/approvals?approver_id=15882&status=pending_approval"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION_HEADER_NAME, serviceCredential))
                .andRespond(withSuccess(
                        fromFile("/fake/approvals.json").replace("${coupa_host}", mockBackend.url("")),
                        APPLICATION_JSON
                ));
    }

    protected void mockUserDetails(final String serviceCredential) throws Exception {
        mockBackend.expect(requestTo("/api/users?email=admin%40acme.com"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION_HEADER_NAME, serviceCredential))
                .andRespond(withSuccess(
                        fromFile("/fake/user-details.json").replace("${coupa_host}", mockBackend.url("")),
                        APPLICATION_JSON
                ));
    }

    protected void mockApproveAction(final String serviceCredential) {
        mockBackend.expect(requestTo("/api/approvals/6609559/approve?reason=Approved"))
                .andExpect(method(PUT))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION_HEADER_NAME, serviceCredential))
                .andRespond(withSuccess());
    }

    protected void mockRejectAction(final String serviceCredential) {
        mockBackend.expect(requestTo("/api/approvals/6609559/reject?reason=Declined"))
                .andExpect(method(PUT))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION_HEADER_NAME, serviceCredential))
                .andRespond(withSuccess());
    }

    protected void mockRequisitionDetails(final String serviceCredential) throws Exception {
        mockBackend.expect(requestTo("/api/requisitions?id=182964&status=pending_approval"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION_HEADER_NAME, serviceCredential))
                .andRespond(withSuccess(
                        fromFile("/fake/requisition-details.json").replace("${coupa_host}", mockBackend.url("")),
                        APPLICATION_JSON
                ));
    }
}
