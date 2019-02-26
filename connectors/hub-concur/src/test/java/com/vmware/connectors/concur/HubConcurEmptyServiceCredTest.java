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
public class HubConcurEmptyServiceCredTest extends HubConcurControllerTestBase {

    @ParameterizedTest
    @CsvSource({
            ", success.json",
            "xx, success_xx.json"
    })
    void testCardsRequests(String lang, String expected) throws Exception {
        testCardsRequests(lang, expected, CALLER_SERVICE_CREDS);
    }

    @Test
    void testApproveRequest() throws Exception {
        testApproveRequest(CALLER_SERVICE_CREDS);
    }

    @Test
    void testRejectRequest() throws Exception {
       testRejectRequest(CALLER_SERVICE_CREDS);
    }

    @Test
    void testUnauthorizedApproveRequest() throws Exception {
        testUnauthorizedApproveRequest(CALLER_SERVICE_CREDS);
    }
}
