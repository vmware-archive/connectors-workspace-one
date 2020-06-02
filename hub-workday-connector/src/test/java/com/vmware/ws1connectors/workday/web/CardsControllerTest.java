/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.web;

import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static com.vmware.ws1connectors.workday.exceptions.ExceptionHandlers.INVALID_CONNECTOR_TOKEN;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.CARDS_REQUESTS_API;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TIME_OFF_REQUEST_API_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.USER_INFO;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.WORKDAY_CONNECTOR_CONTEXT_PATH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static org.hamcrest.Matchers.any;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

public class CardsControllerTest extends ControllerTestUtils {
    private static final String UNAUTHORIZED_WORKDAY_AUTH_TOKEN = BEARER + "unauthorized-auth-token";
    private static final String NO_WORKDAY_AUTH_TOKEN = null;
    private static final String TIME_OFF_REQUEST_ID_1_API = TIME_OFF_REQUEST_API_PATH + "fc844b7a8f6f01580738a5ffd6115105";
    private static final String TIME_OFF_REQUEST_ID_2_API = TIME_OFF_REQUEST_API_PATH + "fc844b7a8f6f01223ce56358d5113e05";
    private static final String ERROR_JSON_PATH = "/error";
    private static final String USER_NOT_FOUND_ERROR_MSG = "User can not be found";

    @Test public void testCardRequestsApiIsProtected() throws Exception {
        testProtectedResource(POST, WORKDAY_CONNECTOR_CONTEXT_PATH + CARDS_REQUESTS_API);
    }

    @Test public void cardRequestFailsWhenUnauthorizedWorkdayAuthTokenIsProvided() {
        mockBackend.expect(requestTo(any(String.class)))
            .andRespond(withUnauthorizedRequest());

        requestCards(UNAUTHORIZED_WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API)
            .expectStatus().isBadRequest()
            .expectBody().jsonPath(ERROR_JSON_PATH, INVALID_CONNECTOR_TOKEN);
    }

    @Test public void cardRequestFailsWhenWorkdayAuthTokenIsMissing() {
        requestCards(NO_WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API)
            .expectStatus().isBadRequest();
    }

    @Test public void cardRequestFailsWhenUserNotFound() throws Exception {
        mockWorkdayApiResponse(USER_INFO, "no_results.json");

        requestCards(WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API)
            .expectStatus().isBadRequest()
            .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
            .expectBody().jsonPath(ERROR_JSON_PATH, USER_NOT_FOUND_ERROR_MSG);
    }

    @Test public void canGetEmptyCardsWhenNoTimeOffTasksFound() throws Exception {
        mockWorkdayApiResponse(USER_INFO, "fred_user_info.json");
        mockWorkdayApiResponse(getInboxTasksUri(), "no_results.json");

        requestCards(WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API)
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
            .expectBody().json(fromFile("cards/no_cards.json"));
    }

    @Test public void canGetCards() throws Exception {
        mockWorkdayApiResponse(USER_INFO, "fred_user_info.json");
        mockWorkdayApiResponse(getInboxTasksUri(), "mixed_inbox_tasks.json");
        mockWorkdayApiResponse(TIME_OFF_REQUEST_ID_1_API, "time_off_request_1.json");
        mockWorkdayApiResponse(TIME_OFF_REQUEST_ID_2_API, "time_off_request_2.json");

        final String actualResponseBody = requestCards(WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API)
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
            .returnResult(String.class)
            .getResponseBody()
            .collect(Collectors.joining())
            .block();

        assertThatJson(actualResponseBody).when(IGNORING_EXTRA_FIELDS, IGNORING_ARRAY_ORDER)
            .isEqualTo(fromFile("cards/cards.json"));
    }

    @Test public void canGetCardsWhenInboxTaskHasDifferentDescriptor() throws Exception {
        mockWorkdayApiResponse(USER_INFO, "fred_user_info.json");
        mockWorkdayApiResponse(getInboxTasksUri(), "mixed_inbox_tasks_1.json");
        mockWorkdayApiResponse(TIME_OFF_REQUEST_ID_1_API, "time_off_request_diff_desc_1.json");
        mockWorkdayApiResponse(TIME_OFF_REQUEST_ID_2_API, "time_off_request_diff_desc_2.json");

        final String actualResponseBody = requestCards(WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API)
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .block();

        assertThatJson(actualResponseBody).when(IGNORING_EXTRA_FIELDS, IGNORING_ARRAY_ORDER)
                .isEqualTo(fromFile("cards/cards_diff_desc.json"));
    }
}
