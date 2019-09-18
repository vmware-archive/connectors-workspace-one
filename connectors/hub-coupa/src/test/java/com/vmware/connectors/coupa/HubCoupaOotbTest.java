/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.coupa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.TestPropertySource;

/**
 * Test cases with empty service api key from configuration.
 */
@TestPropertySource("classpath:empty-coupa-service-credential.properties")
class HubCoupaOotbTest extends HubCoupaControllerTestBase {

    @ParameterizedTest
    @CsvSource({
            ", success.json",
            "xx, success_xx.json"
    })
    void testCardsRequests(String lang, String expected) throws Exception {
        mockCoupaRequest(CALLER_SERVICE_CREDS);
        cardsRequest(lang, expected, CALLER_SERVICE_CREDS);
    }

    @Test
    void testCardsRequestsWithMissingAuthHeader() {
        cardsRequest("", "")
                .expectStatus().isBadRequest();
    }

    @Test
    void testApproveRequest() throws Exception {
        mockApproveActions(CALLER_SERVICE_CREDS);

        approveRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isOk();
    }

    @Test
    void testApproveRequestWithMissingAuthHeader() {
        approveRequest("")
                .expectStatus().isBadRequest();
    }

    @Test
    void testUnauthorizedApproveRequest() throws Exception {
        // User tries to approve a report that isn't theirs
        mockOtherRequisitionDetails(CALLER_SERVICE_CREDS);

        approveRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isNotFound();
    }

    @Test
    void testRejectRequest() throws Exception {
        mockRejectActions(CALLER_SERVICE_CREDS);

        rejectRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isOk();
    }

    @Test
    void testRejectRequestWithMissingAuthHeader() {
        rejectRequest("")
                .expectStatus().isBadRequest();
    }

    @Test
    void testUnauthorizedRejectRequest() throws Exception {
        // User tries to reject a report that isn't theirs
        mockOtherRequisitionDetails(CALLER_SERVICE_CREDS);

        rejectRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isNotFound();
    }

    @Test
    void testFetchAttachmentForValidDetails() throws Exception {
        mockCoupaRequest(CALLER_SERVICE_CREDS);
        mockFetchAttachment(CALLER_SERVICE_CREDS);

        fetchAttachment(CALLER_SERVICE_CREDS);
    }

    @Test
    void testFetchAttachmentForInvalidDetails() throws Exception {
        mockUserDetails(CALLER_SERVICE_CREDS);
        mockApproval(CALLER_SERVICE_CREDS);
        mockOtherRequisitionDetails(CALLER_SERVICE_CREDS);

        fetchAttachmentForInvalidDetails(CALLER_SERVICE_CREDS);
    }

    @Test
    void testUnauthorizedAttachmentRequest() throws Exception {
        mockUserDetails(CALLER_SERVICE_CREDS);
        mockApproval(CALLER_SERVICE_CREDS);
        mockRequisitionDetails(CALLER_SERVICE_CREDS);
        mockUnauthorizedAttachmentReq(CALLER_SERVICE_CREDS);

        unauthorizedAttachmentRequest(CALLER_SERVICE_CREDS);
    }

    @Test
    void testAttachmentWithServerError() throws Exception {
        mockUserDetails(CALLER_SERVICE_CREDS);
        mockApproval(CALLER_SERVICE_CREDS);
        mockRequisitionDetails(CALLER_SERVICE_CREDS);
        mockAttachmentServerError(CALLER_SERVICE_CREDS);

        fetchAttachmentWithServerError(CALLER_SERVICE_CREDS);
    }
}
