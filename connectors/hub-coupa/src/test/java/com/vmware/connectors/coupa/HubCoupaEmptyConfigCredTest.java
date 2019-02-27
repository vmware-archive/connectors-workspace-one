/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.coupa;

import com.vmware.connectors.test.ControllerTestsBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Test cases with empty service api key from configuration.
 */
@TestPropertySource("classpath:empty-coupa-service-credential.properties")
public class HubCoupaEmptyConfigCredTest extends HubCoupaControllerTestBase {

    @ParameterizedTest
    @CsvSource({
            ", success.json",
            "xx, success_xx.json"
    })
    void testCardsRequests(String lang, String expected) throws Exception {
        testCardsRequest(lang, expected, CALLER_SERVICE_CREDS, CALLER_SERVICE_CREDS);
    }

    @Test
    void testCardsRequestsWithMissingAuthHeader() {
        // Missing X-Connector-Authorization header.
        webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/coupa/")
                .headers(ControllerTestsBase::headers)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testApproveRequest() throws Exception {
        testApproveRequest(CALLER_SERVICE_CREDS, CALLER_SERVICE_CREDS);
    }

    @Test
    void testApproveRequestWithMissingAuthHeader() {
        // Missing X-Connector-Authorization header.
        webClient.post()
                .uri("/api/approve/{id}?comment=Approved", "182964")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .contentType(APPLICATION_FORM_URLENCODED)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testRejectRequest() throws Exception {
        testRejectRequest(CALLER_SERVICE_CREDS, CALLER_SERVICE_CREDS);
    }

    @Test
    void testRejectRequestWithMissingAuthHeade() {
        // Missing X-Connector-Authorization header.
        webClient.post().uri("/api/decline/{id}?comment=Declined", "182964")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .contentType(APPLICATION_FORM_URLENCODED)
                .exchange().expectStatus().isBadRequest();
    }
}
