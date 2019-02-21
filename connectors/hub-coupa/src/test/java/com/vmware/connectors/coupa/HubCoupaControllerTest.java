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
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

class HubCoupaControllerTest extends ControllerTestsBase {

    private static final String CONNECTOR_AUTH = "X-Connector-Authorization";

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
        mockCoupaRequest();

        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(CONNECTOR_AUTH, "cloneetestauthtoken")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/coupa/")
                .headers(ControllerTestsBase::headers)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON);

        if (StringUtils.isNotBlank(lang)) {
            spec.header(ACCEPT_LANGUAGE, lang);
        }

        String body = spec.exchange()
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

    private void mockCoupaRequest() throws Exception {
        mockUserDetails();
        mockApproval();
        mockRequisitionDetails();
    }

    private void mockUserDetails() throws Exception {
        mockBackend.expect(requestTo("/api/users?email=admin%40acme.com"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(
                        fromFile("/fake/user-details.json").replace("${coupa_host}", mockBackend.url("")),
                        APPLICATION_JSON
                ));
    }

    private void mockApproval() throws Exception {
        mockBackend.expect(requestTo("/api/approvals?approver_id=15882&status=pending_approval"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(
                        fromFile("/fake/approvals.json").replace("${coupa_host}", mockBackend.url("")),
                        APPLICATION_JSON
                ));
    }

    private void mockRequisitionDetails() throws Exception {
        mockBackend.expect(requestTo("/api/requisitions?id=182964&status=pending_approval"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(
                        fromFile("/fake/requisition-details.json").replace("${coupa_host}", mockBackend.url("")),
                        APPLICATION_JSON
                ));
    }

    @Test
    void testApproveRequest() throws Exception {
        mockRequisitionDetails();
        mockApproveAction();

        webClient.post()
                .uri("/api/approve/{id}?comment=Approved", "182964")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(CONNECTOR_AUTH, "cloneetestauthtoken")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .contentType(APPLICATION_FORM_URLENCODED)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testRejectRequest() throws Exception {
        mockRequisitionDetails();
        mockRejectAction();

        webClient.post().uri("/api/decline/{id}?comment=Declined", "182964")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(CONNECTOR_AUTH, "cloneetestauthtoken")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .contentType(APPLICATION_FORM_URLENCODED)
                .exchange().expectStatus().isOk();
    }

    private void mockApproveAction() {
        mockBackend.expect(requestTo("/api/approvals/6609559/approve?reason=Approved"))
                .andExpect(method(PUT))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess());
    }

    private void mockRejectAction() {
        mockBackend.expect(requestTo("/api/approvals/6609559/reject?reason=Declined"))
                .andExpect(method(PUT))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess());
    }

}
