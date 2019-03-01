/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.coupa;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Test cases with empty service api key from configuration.
 */
@TestPropertySource("classpath:empty-coupa-service-credential.properties")
public class HubCoupaEmptyConfigCredTest extends HubCoupaControllerTestBase {

    @Test
    void testCardRequest() throws Exception {
        mockCoupaRequest(CALLER_SERVICE_CREDS);

        testCardsRequest("", CALLER_SERVICE_CREDS)
                .expectStatus().isOk();
    }

    @Test
    void testCardsRequestsWithMissingAuthHeader() {
        testCardsRequest("", "")
                .expectStatus().isBadRequest();
    }

    @Test
    void testApproveRequest() throws Exception {
        mockApproveActions(CALLER_SERVICE_CREDS);

        testApproveRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isOk();
    }

    @Test
    void testApproveRequestWithMissingAuthHeader() {
        testApproveRequest("")
                .expectStatus().isBadRequest();
    }

    @Test
    void testRejectRequest() throws Exception {
        mockRejectActions(CALLER_SERVICE_CREDS);

        testRejectRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isOk();
    }

    @Test
    void testRejectRequestWithMissingAuthHeader() {
        testRejectRequest("")
                .expectStatus().isBadRequest();
    }
}
