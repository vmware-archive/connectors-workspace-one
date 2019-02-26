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
public class HubCoupaEmptyConfigCredTest extends HubCoupaControllerTestBase {

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
}
