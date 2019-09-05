/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * Test cases with empty concur service account auth header from configuration.
 */
@TestPropertySource(locations = "classpath:empty-concur-service-credential.properties")
class HubConcurOotbTest extends HubConcurControllerTestBase {

    private static final MultiValueMap<String, String> FORM_DATA_FROM_HTTP_REQUEST;

    static  {
        FORM_DATA_FROM_HTTP_REQUEST = getFormDataFromHttpRequest();
    }

    static MultiValueMap<String, String> getFormDataFromHttpRequest() {
        final String[] authValues = CALLER_SERVICE_CREDS.split(":");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.put(USERNAME, List.of(authValues[0]));
        formData.put(PASSWORD, List.of(authValues[1]));
        formData.put(CLIENT_ID, List.of(authValues[2]));
        formData.put(CLIENT_SECRET, List.of(authValues[3]));
        formData.put(GRANT_TYPE, List.of(PASSWORD));

        return formData;
    }

    @ParameterizedTest
    @CsvSource({
            ", success.json",
            "xx, success_xx.json"
    })
    void testCardsRequests(String lang, String expected) throws Exception {
        mockOAuthToken(FORM_DATA_FROM_HTTP_REQUEST);
        mockConcurRequests(EXPECTED_AUTH_HEADER);
        cardsRequest(lang, expected, CALLER_SERVICE_CREDS);
    }

    @Test
    void testCardsRequestsWithMissingAuthHeader() throws Exception {
        cardsRequest("", "")
                .expectStatus().isBadRequest();
    }

    @Test
    void testApproveRequest() throws Exception {
        mockOAuthToken(FORM_DATA_FROM_HTTP_REQUEST);
        mockActionRequests(EXPECTED_AUTH_HEADER);

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
        mockOAuthToken(FORM_DATA_FROM_HTTP_REQUEST);
        mockEmptyReportsDigest(EXPECTED_AUTH_HEADER);

        approveRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isNotFound();
    }

    @Test
    void testRejectRequest() throws Exception {
        mockOAuthToken(FORM_DATA_FROM_HTTP_REQUEST);
        mockActionRequests(EXPECTED_AUTH_HEADER);

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
        mockOAuthToken(FORM_DATA_FROM_HTTP_REQUEST);
        mockEmptyReportsDigest(EXPECTED_AUTH_HEADER);

        rejectRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isNotFound();
    }

    @Test
    void fetchAttachmentForValidUser() throws Exception {
        mockOAuthToken(FORM_DATA_FROM_HTTP_REQUEST);
        mockReport1(EXPECTED_AUTH_HEADER);
        mockFetchAttachment(EXPECTED_AUTH_HEADER);
        mockUserReportsDigest(EXPECTED_AUTH_HEADER);

        fetchAttachment(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }

    @Test
    void fetchAttachmentForInvalidUserLoginID() throws Exception {
        // Invalid user tries to fetch an expense report attachment.
        mockOAuthToken(FORM_DATA_FROM_HTTP_REQUEST);
        mockUserDetailReport(EXPECTED_AUTH_HEADER, "/fake/invalid-user-details.json");
        mockReportsDigest(EXPECTED_AUTH_HEADER, "invalid%40acme.com");

        fetchAttachmentForInvalidDetails(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }

    @Test
    void fetchAttachmentForInvalidAttachmentID() throws Exception {
        // Valid user tries to fetch an expense report attachment which does not belong to them.
        mockOAuthToken(FORM_DATA_FROM_HTTP_REQUEST);
        mockUserDetailReport(EXPECTED_AUTH_HEADER, "/fake/user-details.json");
        mockReportsDigest(EXPECTED_AUTH_HEADER, "admin%40acme.com");

        fetchAttachmentForInvalidDetails(CALLER_SERVICE_CREDS, "invalid-attachment-id");
    }

    @Test
    void testAttachmentUrlNotFound() throws Exception {
        mockOAuthToken(FORM_DATA_FROM_HTTP_REQUEST);
        mockUserReportsDigest(EXPECTED_AUTH_HEADER);
        mockReportWithEmptyAttachmentURL(EXPECTED_AUTH_HEADER);

        fetchAttachmentForInvalidDetails(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }

    @Test
    void testUnauthorizedError() throws Exception {
        mockOAuthToken(FORM_DATA_FROM_HTTP_REQUEST);
        mockReport1(EXPECTED_AUTH_HEADER);
        mockUserReportsDigest(EXPECTED_AUTH_HEADER);
        mockFetchAttachmentWithUnauthorized(EXPECTED_AUTH_HEADER);

        fetchAttachmentForUnauthorizedCredential(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }

    @Test
    void testBadStatusCode() throws Exception {
        mockOAuthToken(FORM_DATA_FROM_HTTP_REQUEST);
        mockReport1(EXPECTED_AUTH_HEADER);
        mockUserReportsDigest(EXPECTED_AUTH_HEADER);
        mockFetchAttachmentWithInternalServerError(EXPECTED_AUTH_HEADER);

        fetchAttachmentWithBadStatusCode(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F");
    }

    @ParameterizedTest
    @CsvSource({
            // Service account credential length < 4
            "username:client-id:client-secret",
            // Service account credential length > 4
            "username:password:client-id:client-secret:junk"
    })
    void testInvalidServiceAccountCredential(String serviceCredential) {
        testServiceAccountCredential(serviceCredential);
    }

    @Test
    void testUnauthorizedServiceAccountCredential() {
        unauthorizedServiceAccountCredential();
    }
}
