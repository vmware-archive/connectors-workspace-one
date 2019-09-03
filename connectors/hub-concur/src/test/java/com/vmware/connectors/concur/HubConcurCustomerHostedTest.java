/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.TestPropertySource;

/**
 * Test cases with non empty concur service account auth header from configuration.
 */
@TestPropertySource(locations = "classpath:non-empty-concur-service-credential.properties")
class HubConcurCustomerHostedTest extends HubConcurControllerTestBase {

    @ParameterizedTest
    @CsvSource({
            ", success.json,",
            ", success.json, should-be-ignored",
            "xx, success_xx.json,"
    })
    void testCardsRequests(String lang, String expected, String authHeader) throws Exception {
        mockConcurRequests(CONFIG_SERVICE_CREDS);
        cardsRequest(lang, expected, authHeader);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "should-be-ignored"
    })
    void testApproveRequests(String authHeader) throws Exception {
        mockActionRequests(CONFIG_SERVICE_CREDS);

        approveRequest(authHeader)
                .expectStatus().isOk();
    }

    @Test
    void testUnauthorizedApproveRequest() throws Exception {
        // User tries to approve an expense report that isn't theirs
        mockEmptyReportsDigest(CONFIG_SERVICE_CREDS);

        approveRequest("")
                .expectStatus().isNotFound();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "should-be-ignored"
    })
    void testRejectRequests(String authHeader) throws Exception {
        mockActionRequests(CONFIG_SERVICE_CREDS);

        rejectRequest(authHeader)
                .expectStatus().isOk();
    }

    @Test
    void testUnauthorizedRejectRequest() throws Exception {
        // User tries to reject an expense report that isn't theirs
        mockEmptyReportsDigest(CONFIG_SERVICE_CREDS);

        rejectRequest("")
                .expectStatus().isNotFound();
    }

}
