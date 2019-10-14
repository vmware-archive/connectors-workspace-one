/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.coupa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.TestPropertySource;

/**
 * Test cases with non empty coupa api key from configuration.
 */
@TestPropertySource("classpath:non-empty-coupa-service-credential.properties")
class HubCoupaCustomerHostedTest extends HubCoupaControllerTestBase {

    @ParameterizedTest
    @CsvSource({
            ", success.json,",
            ", success.json, should-be-ignored",
            "xx, success_xx.json, "
    })
    void testCardsRequests(String lang, String expected, String authHeader) throws Exception {
        mockCoupaRequest(CONFIG_SERVICE_CREDS);
        cardsRequest(lang, expected, authHeader);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "should-be-ignored"
    })
    void testApproveRequests(String authHeader) throws Exception {
        mockApproveActions(CONFIG_SERVICE_CREDS);

        approveRequest(authHeader)
                .expectStatus().isOk();
    }

    @Test
    void testUnauthorizedApproveRequest() throws Exception {
        // User tries to approve a report that isn't theirs
        mockOtherRequisitionDetails(CONFIG_SERVICE_CREDS);

        approveRequest("")
                .expectStatus().isNotFound();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "should-be-ignored"
    })
    void testRejectRequests(String authHeader) throws Exception {
        mockRejectActions(CONFIG_SERVICE_CREDS);

        rejectRequest(authHeader)
                .expectStatus().isOk();
    }

    @Test
    void testUnauthorizedRejectRequest() throws Exception {
        // User tries to reject a report that isn't theirs
        mockOtherRequisitionDetails(CONFIG_SERVICE_CREDS);

        rejectRequest("")
                .expectStatus().isNotFound();
    }

    @Test
    void testFetchAttachmentForValidDetails() throws Exception {
        mockCoupaRequest(CONFIG_SERVICE_CREDS);
        mockFetchAttachment(CONFIG_SERVICE_CREDS);

        fetchAttachment("");
    }
}
