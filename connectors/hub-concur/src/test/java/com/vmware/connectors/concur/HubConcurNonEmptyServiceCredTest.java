/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.test.JsonNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

/**
 * Test cases with non empty concur service account auth header from configuration.
 */
@TestPropertySource(locations = "classpath:non-empty-concur-service-credential.properties")
class HubConcurNonEmptyServiceCredTest extends HubConcurControllerTestBase {

    @ParameterizedTest
    @MethodSource("cardRequests")
    void testCardsRequests(String lang, String expected, String serviceCredential, String authHeader) throws Exception {
        mockConcurRequests(serviceCredential);
        final String body = testCardsRequest(lang, authHeader)
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

    private static Stream<Arguments> cardRequests() {
        return Stream.of(
                Arguments.of("", "success.json", CONFIG_SERVICE_CREDS, CONFIG_SERVICE_CREDS),
                Arguments.of("xx", "success_xx.json", CONFIG_SERVICE_CREDS, "")
        );
    }

    @ParameterizedTest
    @MethodSource("actionRequest")
    void testApproveRequests(final String serviceCredential, final String authHeader) throws Exception {
        mockActionRequests(serviceCredential);

        testApproveRequest(authHeader)
                .expectStatus().isOk();
    }

    @ParameterizedTest
    @MethodSource("actionRequest")
    void testRejectRequests(final String serviceCredential, final String authHeader) throws Exception {
        mockActionRequests(serviceCredential);

        testRejectRequest(authHeader)
                .expectStatus().isOk();
    }

    private static Stream<Arguments> actionRequest() {
        return Stream.of(
                Arguments.of(CONFIG_SERVICE_CREDS, CONFIG_SERVICE_CREDS),
                Arguments.of(CONFIG_SERVICE_CREDS, "")
        );
    }

    @Test
    void testUnauthorizedApproveRequest() throws Exception {
        testUnauthorizedApproveRequest(CONFIG_SERVICE_CREDS);
    }
}
