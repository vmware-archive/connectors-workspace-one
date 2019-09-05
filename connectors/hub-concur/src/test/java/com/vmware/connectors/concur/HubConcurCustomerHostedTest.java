/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * Test cases with non empty concur service account auth header from configuration.
 */
@TestPropertySource(locations = "classpath:non-empty-concur-service-credential.properties")
class HubConcurCustomerHostedTest extends HubConcurControllerTestBase {

    private static final MultiValueMap<String, String> FORM_DATA_FROM_CONFIG;

    static  {
        FORM_DATA_FROM_CONFIG = getFormDataFromConfig();
    }

    static MultiValueMap<String, String> getFormDataFromConfig() {
        final String[] authValues = CONFIG_SERVICE_CREDS.split(":");

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
            ", success.json,",
            ", success.json, should-be-ignored",
            "xx, success_xx.json,"
    })
    void testCardsRequests(String lang, String expected, String authHeader) throws Exception {
        mockOAuthToken(FORM_DATA_FROM_CONFIG);
        mockConcurRequests(EXPECTED_AUTH_HEADER);
        cardsRequest(lang, expected, authHeader);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "should-be-ignored"
    })
    void testApproveRequests(String authHeader) throws Exception {
        mockOAuthToken(FORM_DATA_FROM_CONFIG);
        mockActionRequests(EXPECTED_AUTH_HEADER);

        approveRequest(authHeader)
                .expectStatus().isOk();
    }

    @Test
    void testUnauthorizedApproveRequest() throws Exception {
        // User tries to approve an expense report that isn't theirs
        mockOAuthToken(FORM_DATA_FROM_CONFIG);
        mockEmptyReportsDigest(EXPECTED_AUTH_HEADER);

        approveRequest("")
                .expectStatus().isNotFound();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "should-be-ignored"
    })
    void testRejectRequests(String authHeader) throws Exception {
        mockOAuthToken(FORM_DATA_FROM_CONFIG);
        mockActionRequests(EXPECTED_AUTH_HEADER);

        rejectRequest(authHeader)
                .expectStatus().isOk();
    }

    @Test
    void testUnauthorizedRejectRequest() throws Exception {
        // User tries to reject an expense report that isn't theirs
        mockOAuthToken(FORM_DATA_FROM_CONFIG);
        mockEmptyReportsDigest(EXPECTED_AUTH_HEADER);

        rejectRequest("")
                .expectStatus().isNotFound();
    }
}
