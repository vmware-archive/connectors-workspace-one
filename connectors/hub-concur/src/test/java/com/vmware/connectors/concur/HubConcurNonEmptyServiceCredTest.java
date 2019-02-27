/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

/**
 * Test cases with non empty concur service account auth header from configuration.
 */
@TestPropertySource(locations = "classpath:non-empty-concur-service-credential.properties")
class HubConcurNonEmptyServiceCredTest extends HubConcurControllerTestBase {

    @ParameterizedTest
    @MethodSource("cardRequests")
    void testCardsRequests(String lang, String expected, String serviceCredential, String authHeader) throws Exception {
        testCardsRequest(lang, expected, serviceCredential, authHeader);
    }

    private static Stream<Arguments> cardRequests() {
        return Stream.of(
                Arguments.of("", "success.json", CONFIG_SERVICE_CREDS, CONFIG_SERVICE_CREDS),
                Arguments.of("xx", "success_xx.json", CONFIG_SERVICE_CREDS, "")
        );
    }

    @ParameterizedTest
    @MethodSource("approveRequest")
    void testApproveRequests(final String serviceCredential, final String authHeader) throws Exception {
        testApproveRequest(serviceCredential, authHeader);
    }

    private static Stream<Arguments> approveRequest() {
        return Stream.of(
                Arguments.of(CONFIG_SERVICE_CREDS, CONFIG_SERVICE_CREDS),
                Arguments.of(CONFIG_SERVICE_CREDS, "")
        );
    }

    @ParameterizedTest
    @MethodSource("rejectRequest")
    void testRejectRequests(final String serviceCredential, final String authHeader) throws Exception {
        testRejectRequest(serviceCredential, authHeader);
    }

    private static Stream<Arguments> rejectRequest() {
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
