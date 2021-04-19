/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.coupa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

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
    public void testMissingBaseUrlRequestHeader() throws IOException {
        cardsRequestMissingBaseUrl()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Missing request header 'X-Connector-Base-Url' for method parameter of type String");
    }

    @Test
    void testApproveRequest() throws Exception {
        mockUserDetails(CALLER_SERVICE_CREDS);
        mockApproveActionsWithComment(CALLER_SERVICE_CREDS);

        approveRequestWithComment(CALLER_SERVICE_CREDS)
                .expectStatus().isOk();
    }

    @Test
    void testApproveRequestWithMissingAuthHeader() {
        approveRequestWithComment("")
                .expectStatus().isBadRequest();
    }

    @Test
    void testUnauthorizedApproveRequest() throws Exception {
        // User tries to approve a report that isn't theirs
        mockUserDetails(CALLER_SERVICE_CREDS);
        mockOtherRequisitionDetails(CALLER_SERVICE_CREDS);

        approveRequestWithComment(CALLER_SERVICE_CREDS)
                .expectStatus().isUnauthorized()
                .expectBody().json(fromFile("connector/responses/invalid_user_action.json"));
    }

    @Test
    void testRejectRequest() throws Exception {
        mockUserDetails(CALLER_SERVICE_CREDS);
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
    void testApproveWithoutComment() throws Exception {
        mockUserDetails(CALLER_SERVICE_CREDS);
        mockApproveActionsWithoutComment(CALLER_SERVICE_CREDS);

        approveRequestWithoutComment(CALLER_SERVICE_CREDS)
                .expectStatus().isOk();
    }

    @Test
    void testDeclineWithoutComment() {
        rejectRequestWithoutComment(CALLER_SERVICE_CREDS)
                .expectStatus().isBadRequest();
    }

    @Test
    void testUnauthorizedRejectRequest() throws Exception {
        // User tries to reject a report that isn't theirs
        mockUserDetails(CALLER_SERVICE_CREDS);
        mockOtherRequisitionDetails(CALLER_SERVICE_CREDS);

        rejectRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isUnauthorized()
                .expectBody().json(fromFile("connector/responses/invalid_user_action.json"));
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

    @Test
    void testInvalidAttachmentId() throws Exception {
        mockUserDetails(CALLER_SERVICE_CREDS);
        mockApproval(CALLER_SERVICE_CREDS);
        mockRequisitionDetails(CALLER_SERVICE_CREDS);

        fetchInvalidAttachmentId(CALLER_SERVICE_CREDS);
    }

    @Test
    void testCardReqWhenUserNotFound() throws IOException {
        mockEmptyUserDetails(CALLER_SERVICE_CREDS);

        cardsRequest("", CALLER_SERVICE_CREDS)
                .expectStatus().isOk()
                .expectBody().json(fromFile("connector/responses/empty_card_response.json"));
    }

    @Test
    void testCardReqWithEmptyRequisitionDetails() throws Exception {
        mockUserDetails(CALLER_SERVICE_CREDS);
        mockApproval(CALLER_SERVICE_CREDS);
        mockEmptyRequisitionDetails(CALLER_SERVICE_CREDS);

        cardsRequest("", CALLER_SERVICE_CREDS)
                .expectStatus().isOk()
                .expectBody().json(fromFile("connector/responses/empty_card_response.json"));
    }

    @Test
    void testCardReqWithEmptyApprovals() throws Exception {
        mockUserDetails(CALLER_SERVICE_CREDS);
        mockEmptyApproval(CALLER_SERVICE_CREDS);

        cardsRequest("", CALLER_SERVICE_CREDS)
                .expectStatus().isOk()
                .expectBody().json(fromFile("connector/responses/empty_card_response.json"));
    }

    @Test
    void testApproveWhenUserNotFound() throws IOException {
        mockEmptyUserDetails(CALLER_SERVICE_CREDS);

        approveRequestWithComment(CALLER_SERVICE_CREDS)
                .expectStatus().isNotFound()
                .expectBody().json(fromFile("connector/responses/user_not_found_error.json"));
    }

    @Test
    void testRejectWhenUserNotFound() throws Exception {
        mockEmptyUserDetails(CALLER_SERVICE_CREDS);

        rejectRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isNotFound()
                .expectBody().json(fromFile("connector/responses/user_not_found_error.json"));
    }
}
