/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.test.ControllerTestsBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.BodyInserters;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Test cases with empty concur service account auth header from configuration.
 */
@TestPropertySource(locations = "classpath:empty-concur-service-credential.properties")
public class HubConcurEmptyServiceCredTest extends HubConcurControllerTestBase {

    @ParameterizedTest
    @CsvSource({
            ", success.json",
            "xx, success_xx.json"
    })
    void testCardsRequests(String lang, String expected) throws Exception {
        testCardsRequest(lang, expected, CALLER_SERVICE_CREDS, CALLER_SERVICE_CREDS);
    }

    @Test
    void testCardsRequestsWithEmptyAuthHeader() throws Exception {
        // Missing X-Connector-Authorization header.
        webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/concur/")
                .headers(ControllerTestsBase::headers)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .syncBody(fromFile("/connector/requests/request.json"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testApproveRequest() throws Exception {
        testApproveRequest(CALLER_SERVICE_CREDS, CALLER_SERVICE_CREDS);
    }

    @Test
    void testApprovedReqWithEmptyAuthHeader() {
        // Missing X-Connector-Authorization header.
        webClient.post().uri("/api/expense/{id}/approve", "1D3BD2E14D144508B05F")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("comment", "Approval Done"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testRejectRequest() throws Exception {
       testRejectRequest(CALLER_SERVICE_CREDS, CALLER_SERVICE_CREDS);
    }

    @Test
    void testRejectRequestWithEmptyAuthHeader() {
        // Missing X-Connector-Authorization header.
        webClient.post().uri("/api/expense/{id}/decline", "1D3BD2E14D144508B05F")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("reason", "Decline Done"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testUnauthorizedApproveRequest() throws Exception {
        testUnauthorizedApproveRequest(CALLER_SERVICE_CREDS);
    }
}
