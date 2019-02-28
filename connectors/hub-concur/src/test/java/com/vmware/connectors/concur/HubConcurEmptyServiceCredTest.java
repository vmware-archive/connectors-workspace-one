/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.test.JsonNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

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
        mockConcurRequests(CALLER_SERVICE_CREDS);

        final String body = testCardsRequest(lang, CALLER_SERVICE_CREDS)
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

    @Test
    void testCardsRequestsWithEmptyAuthHeader() throws Exception {
        testCardsRequest("", "")
                .expectStatus().isBadRequest();
    }

    @Test
    void testApproveRequest() throws Exception {
        mockActionRequests(CALLER_SERVICE_CREDS);

        testApproveRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isOk();
    }

    @Test
    void testApprovedReqWithEmptyAuthHeader() {
        testApproveRequest("")
                .expectStatus().isBadRequest();
    }

    @Test
    void testRejectRequest() throws Exception {
        mockActionRequests(CALLER_SERVICE_CREDS);

        testRejectRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isOk();
    }

    @Test
    void testRejectRequestWithEmptyAuthHeader() {
        testRejectRequest("")
                .expectStatus().isBadRequest();
    }

    @Test
    void testUnauthorizedApproveRequest() throws Exception {
        testUnauthorizedApproveRequest(CALLER_SERVICE_CREDS);
    }
}
