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
        mockConcurRequests(SERVICE_CREDS);
        cardsRequest(lang, expected, SERVICE_CREDS);
    }

    @Test
    void testCardsRequestsWithMissingAuthHeader() throws Exception {
        cardsRequest("", "")
                .expectStatus().isBadRequest();
    }

    @Test
    void testApproveRequest() throws Exception {
        mockActionRequests(SERVICE_CREDS);

        approveRequest(SERVICE_CREDS)
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
        mockEmptyReportsDigest(SERVICE_CREDS);

        approveRequest(SERVICE_CREDS)
                .expectStatus().isNotFound();
    }

    @Test
    void testRejectRequest() throws Exception {
        mockActionRequests(SERVICE_CREDS);

        rejectRequest(SERVICE_CREDS)
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
        mockEmptyReportsDigest(SERVICE_CREDS);

        rejectRequest(SERVICE_CREDS)
                .expectStatus().isNotFound();
    }

    @Test
    void fetchAttachmentForValidUser() throws Exception {
        mockReport1(SERVICE_CREDS);
        mockFetchAttachment(SERVICE_CREDS);
        mockUserReportsDigest(SERVICE_CREDS);

        fetchAttachment(SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }

    @Test
    void fetchAttachmentForInvalidUserLoginID() throws Exception {
        // Invalid user tries to fetch an expense report attachment.
        mockUserDetailReport(SERVICE_CREDS, "/fake/invalid-user-details.json");
        mockReportsDigest(SERVICE_CREDS, "invalid%40acme.com");

        fetchAttachmentForInvalidDetails(SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }

    @Test
    void fetchAttachmentForInvalidAttachmentID() throws Exception {
        // Valid user tries to fetch an expense report attachment which does not belong to them.
        mockUserDetailReport(SERVICE_CREDS, "/fake/user-details.json");
        mockReportsDigest(SERVICE_CREDS, "admin%40acme.com");

        fetchAttachmentForInvalidDetails(SERVICE_CREDS, "invalid-attachment-id");
    }

    @Test
    void testAttachmentUrlNotFound() throws Exception {
        mockUserReportsDigest(SERVICE_CREDS);
        mockReportWithEmptyAttachmentURL(SERVICE_CREDS);

        fetchAttachmentForInvalidDetails(SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }

    @Test
    void testUnauthorizedError() throws Exception {
        mockReport1(SERVICE_CREDS);
        mockUserReportsDigest(SERVICE_CREDS);
        mockFetchAttachmentWithUnauthorized(SERVICE_CREDS);

        fetchAttachmentForUnauthorizedCredential(SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }

    @Test
    void testBadStatusCode() throws Exception {
        mockReport1(SERVICE_CREDS);
        mockUserReportsDigest(SERVICE_CREDS);
        mockFetchAttachmentWithInternalServerError(SERVICE_CREDS);

        fetchAttachmentWithBadStatusCode(SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }
}
