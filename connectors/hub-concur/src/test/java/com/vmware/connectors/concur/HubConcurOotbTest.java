/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.TestPropertySource;

/**
 * Test cases with empty concur service account auth header from configuration.
 */
@TestPropertySource(locations = "classpath:empty-concur-service-credential.properties")
class HubConcurOotbTest extends HubConcurControllerTestBase {

    @ParameterizedTest
    @CsvSource({
            ", success.json",
            "xx, success_xx.json"
    })
    void testCardsRequests(String lang, String expected) throws Exception {
        mockConcurRequests(CALLER_SERVICE_CREDS);
        cardsRequest(lang, expected, CALLER_SERVICE_CREDS);
    }

    @Test
    void testCardsRequestsWithMissingAuthHeader() throws Exception {
        cardsRequest("", "")
                .expectStatus().isBadRequest();
    }

    @Test
    void testApproveRequest() throws Exception {
        mockActionRequests(CALLER_SERVICE_CREDS);

        approveRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isOk();
    }

    @Test
    void testApprovedReqWithMissingAuthHeader() {
        approveRequest("")
                .expectStatus().isBadRequest();
    }

    @Test
    void testUnauthorizedApproveRequest() throws Exception {
        // User tries to approve an expense report that isn't theirs
        mockEmptyReportsDigest(CALLER_SERVICE_CREDS);

        approveRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isNotFound();
    }

    @Test
    void testRejectRequest() throws Exception {
        mockActionRequests(CALLER_SERVICE_CREDS);

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
        // User tries to reject an expense report that isn't theirs
        mockEmptyReportsDigest(CALLER_SERVICE_CREDS);

        rejectRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isNotFound();
    }

    @Test
    void fetchAttachmentForValidUser() throws Exception {
        mockReport1(CALLER_SERVICE_CREDS);
        mockFetchAttachment(CALLER_SERVICE_CREDS);
        mockUserReportsDigest(CALLER_SERVICE_CREDS);

        fetchAttachment(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }

    @Test
    void fetchAttachmentForInvalidUserLoginID() throws Exception {
        // Invalid user tries to fetch an expense report attachment.
        mockUserDetailReport(CALLER_SERVICE_CREDS, "/fake/invalid-user-details.json");
        mockReportsDigest(CALLER_SERVICE_CREDS, "invalid%40acme.com");

        fetchAttachmentForInvalidDetails(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }

    @Test
    void fetchAttachmentForInvalidAttachmentID() throws Exception {
        // Valid user tries to fetch an expense report attachment which does not belong to them.
        mockUserDetailReport(CALLER_SERVICE_CREDS, "/fake/user-details.json");
        mockReportsDigest(CALLER_SERVICE_CREDS, "admin%40acme.com");

        fetchAttachmentForInvalidDetails(CALLER_SERVICE_CREDS, "invalid-attachment-id");
    }

    @Test
    void testAttachmentUrlNotFound() throws Exception {
        mockUserReportsDigest(CALLER_SERVICE_CREDS);
        mockReportWithEmptyAttachmentURL(CALLER_SERVICE_CREDS);

        fetchAttachmentForInvalidDetails(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }

    @Test
    void testUnauthorizedError() throws Exception {
        mockReport1(CALLER_SERVICE_CREDS);
        mockUserReportsDigest(CALLER_SERVICE_CREDS);
        mockFetchAttachmentWithUnauthorized(CALLER_SERVICE_CREDS);

        fetchAttachmentForUnauthorizedCredential(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }

    @Test
    void testBadStatusCode() throws Exception {
        mockReport1(CALLER_SERVICE_CREDS);
        mockUserReportsDigest(CALLER_SERVICE_CREDS);
        mockFetchAttachmentWithInternalServerError(CALLER_SERVICE_CREDS);

        fetchAttachmentWithBadStatusCode(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }
}
