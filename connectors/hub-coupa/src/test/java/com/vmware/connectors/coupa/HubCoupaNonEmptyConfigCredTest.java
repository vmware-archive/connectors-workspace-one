/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.coupa;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

/**
 * Test cases with non empty coupa api key from configuration.
 */
@TestPropertySource("classpath:non-empty-coupa-service-credential.properties")
class HubCoupaNonEmptyConfigCredTest extends HubCoupaControllerTestBase {

    @ParameterizedTest
    @MethodSource("cardRequests")
    void testCardsRequests(String authHeader) throws Exception {
        mockCoupaRequest(CONFIG_SERVICE_CREDS);

        testCardsRequest("", authHeader)
                .expectStatus().isOk();
    }

    private static Stream<Arguments> cardRequests() {
        return Stream.of(
                Arguments.of(CONFIG_SERVICE_CREDS),
                Arguments.of("")
        );
    }

    @ParameterizedTest
    @MethodSource("actionRequest")
    void testApproveRequests(final String authHeader) throws Exception {
        mockApproveActions(CONFIG_SERVICE_CREDS);

        testApproveRequest(authHeader)
                .expectStatus().isOk();
    }

    @ParameterizedTest
    @MethodSource("actionRequest")
    void testRejectRequests(final String authHeader) throws Exception {
        mockRejectActions(CONFIG_SERVICE_CREDS);

        testRejectRequest(authHeader)
                .expectStatus().isOk();
    }

    private static Stream<Arguments> actionRequest() {
        return Stream.of(
                Arguments.of(CONFIG_SERVICE_CREDS),
                Arguments.of("")
        );
    }
}
