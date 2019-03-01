/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Test cases with empty concur service account auth header from configuration.
 */
@TestPropertySource(locations = "classpath:empty-concur-service-credential.properties")
public class HubConcurEmptyServiceCredTest extends HubConcurControllerTestBase {

    @Test
    void testCardRequest() throws Exception {
        mockConcurRequests(CALLER_SERVICE_CREDS);

        testCardsRequest("", CALLER_SERVICE_CREDS)
                .expectStatus().isOk();
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
