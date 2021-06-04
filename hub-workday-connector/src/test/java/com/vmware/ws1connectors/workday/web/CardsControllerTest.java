/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.stream.Collectors;

import static com.vmware.ws1connectors.workday.exceptions.ExceptionHandlers.INVALID_CONNECTOR_TOKEN;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.CARDS_REQUESTS_API;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TIME_OFF_REQUEST_API_PATH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static org.hamcrest.Matchers.any;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

public class CardsControllerTest extends ControllerTestUtils {
    private static final String UNAUTHORIZED_WORKDAY_AUTH_TOKEN = BEARER + "unauthorized-auth-token";
    private static final String NO_WORKDAY_AUTH_TOKEN = null;
    private static final String TIME_OFF_REQUEST_ID_1_API = TIME_OFF_REQUEST_API_PATH + "fc844b7a8f6f01580738a5ffd6115105";
    private static final String TIME_OFF_REQUEST_ID_2_API = TIME_OFF_REQUEST_API_PATH + "fc844b7a8f6f01223ce56358d5113e05";
    private static final String ERROR_JSON_PATH = "/error";
    private static final String BUSINESS_TITLE_CHANGE_URL = "/common/v1/businessTitleChanges/0db7bcdda1cd01fbdae2376a4844ab18";
    private static final String BUSINESS_PROCESS_URL = "/common/v1/businessProcesses/f387bb35571f017af0641c153a486314";
    private static final String BUSINESS_PROCESS_URL_1 = "/common/v1/businessProcesses/f387bb35571f013c819744b63a489314";
    private static final String BUSINESS_PROCESS_URL_WITH_STEP_TYPE_ACTION = "/common/v1/businessProcesses/4e7de5de24b7011d66c91c9a610cdb2d";
    private static final String BUSINESS_PROCESS_URL_WITH_STEP_TYPE_REVIEW_DOCUMENTS = "/common/v1/businessProcesses/318af1942cc001af54978e493e14fa0a";
    private static final String REQUEST_BODY_FILE_NAME = "get_cards_request_body.json";
    private static final String BUSINESS_PROCESS_URL_PRE_HIRE_1 = "/common/v1/businessProcesses/318af1942cc001af54978e493e14fa0a";
    private static final String BUSINESS_PROCESS_URL_PRE_HIRE_2 = "/common/v1/businessProcesses/318af1942cc001af76543e493e14fa0a";

    @Test public void testCardRequestsApiIsProtected() throws Exception {
        testProtectedResource(POST, CARDS_REQUESTS_API);
    }

    @Test public void testConnectorDiscoveryMetadata() throws IOException {
        testConnectorDiscovery();
    }

    @Test public void cardRequestFailsWhenUnauthorizedWorkdayAuthTokenIsProvided() throws IOException {
        mockBackend.expect(requestTo(any(String.class)))
            .andRespond(withUnauthorizedRequest());

        requestCards(UNAUTHORIZED_WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API, REQUEST_BODY_FILE_NAME, false)
            .expectStatus().isBadRequest()
            .expectBody().jsonPath(ERROR_JSON_PATH, INVALID_CONNECTOR_TOKEN);
    }

    @Test public void cardRequestFailsWhenWorkdayAuthTokenIsMissing() throws IOException {
        requestCards(NO_WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API, REQUEST_BODY_FILE_NAME, false)
            .expectStatus().isBadRequest();
    }

    @ParameterizedTest
    @ValueSource(strings = {"no_results.json", "inbox_task_without_status.json",
            "inbox_task_without_overall_process.json"})
    public void canGetEmptyCardsWhenNoTimeOffTasksFound(String inboxTaskFile) throws Exception {
        mockWorkdayApiResponse(getInboxTasksUri(), inboxTaskFile);

        requestCards(WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API, REQUEST_BODY_FILE_NAME, false)
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
            .expectBody().json(fromFile("cards/no_cards.json"));
    }

    @Test public void canGetCards() throws Exception {
        mockWorkdayApiResponse(getInboxTasksUri(), "mixed_inbox_tasks.json");
        mockWorkdayApiResponse(TIME_OFF_REQUEST_ID_1_API, "time_off_request_1.json");
        mockWorkdayApiResponse(TIME_OFF_REQUEST_ID_2_API, "time_off_request_2.json");

        final String actualResponseBody = requestCards(WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API,
                REQUEST_BODY_FILE_NAME, false)
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
            .returnResult(String.class)
            .getResponseBody()
            .collect(Collectors.joining())
            .block();

        assertThatJson(actualResponseBody).when(IGNORING_EXTRA_FIELDS, IGNORING_ARRAY_ORDER)
            .isEqualTo(fromFile("cards/cards.json"));
    }

    @Test public void canGetCardsWhenTimeOffDetailsResponseMissingTimeOffDescriptor() throws Exception {
        mockWorkdayApiResponse(getInboxTasksUri(), "inbox_task_timeOff.json");
        mockWorkdayApiResponse(TIME_OFF_REQUEST_ID_1_API, "time_off_response_missing_timeoff_descriptor.json");

        final String actualResponseBody = requestCards(WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API,
                REQUEST_BODY_FILE_NAME, false)
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .block();

        assertThatJson(actualResponseBody).when(IGNORING_EXTRA_FIELDS, IGNORING_ARRAY_ORDER)
                .isEqualTo(fromFile("cards/cards_without_type_attribute.json"));
    }

    @Test public void canGetCardsWithBusinessTitleChange() throws Exception {
        mockWorkdayApiResponse(getInboxTasksUri(), "mixed_inbox_tasks_with_businessTitleChange.json");
        mockWorkdayApiResponse(TIME_OFF_REQUEST_ID_1_API, "time_off_request_1.json");
        mockWorkdayApiResponse(TIME_OFF_REQUEST_ID_2_API, "time_off_request_2.json");
        mockBusinessTitleChangeDetails();

        final String actualResponseBody = requestCards(WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API, REQUEST_BODY_FILE_NAME, false)
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .block();

        assertThatJson(actualResponseBody).when(IGNORING_EXTRA_FIELDS, IGNORING_ARRAY_ORDER)
                .isEqualTo(fromFile("cards/cards_with_business_title_change.json"));
    }

    private void mockBusinessTitleChangeDetails() throws IOException {
        mockBackend.expect(requestTo(BUSINESS_TITLE_CHANGE_URL))
                .andExpect(header(ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, WORKDAY_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("business_title_change.json"), APPLICATION_JSON));
    }

    @Test public void canGetCardsWithBusinessProcess() throws Exception {
        mockWorkdayApiResponse(getInboxTasksUri(), "mixed_inbox_tasks_with_business_process.json");
        mockWorkdayApiResponse(TIME_OFF_REQUEST_ID_1_API, "time_off_request_1.json");
        mockWorkdayApiResponse(TIME_OFF_REQUEST_ID_2_API, "time_off_request_2.json");
        mockWorkdayApiResponse(BUSINESS_TITLE_CHANGE_URL, "business_title_change.json");
        mockWorkdayApiResponse(BUSINESS_PROCESS_URL, "Business_Process_Details.json");
        mockWorkdayApiResponse(BUSINESS_PROCESS_URL_1, "Business_Process_Details_1.json");
        mockWorkdayApiResponse(BUSINESS_PROCESS_URL_WITH_STEP_TYPE_ACTION, "Business_Process_Details_Step_Type_Action.json");
        mockWorkdayApiResponse(BUSINESS_PROCESS_URL_WITH_STEP_TYPE_REVIEW_DOCUMENTS, "Business_Process_Details_Step_Type_Review_Documents.json");

        final String actualResponseBody = requestCards(WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API, REQUEST_BODY_FILE_NAME, false)
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .block();

        assertThatJson(actualResponseBody).when(IGNORING_EXTRA_FIELDS, IGNORING_ARRAY_ORDER)
                .isEqualTo(fromFile("cards/cards_with_business_process.json"));
    }

    @Test public void canGetCardsWithBusinessProcessForPreHire() throws Exception {
        mockWorkdayApiResponse(getInboxTasksUri(), "inbox_task_pre_hire.json");
        mockWorkdayApiResponse(BUSINESS_PROCESS_URL_PRE_HIRE_1, "Business_Process_Details_Pre_Hire_1.json");
        mockWorkdayApiResponse(BUSINESS_PROCESS_URL_PRE_HIRE_2, "Business_Process_Details_Pre_Hire_2.json");

        final String actualResponseBody = requestCards(WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API, REQUEST_BODY_FILE_NAME, true)
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .block();

        assertThatJson(actualResponseBody).when(IGNORING_EXTRA_FIELDS, IGNORING_ARRAY_ORDER)
                .isEqualTo(fromFile("cards/cards_with_business_process_pre_hire.json"));
    }

    @Test public void testMissingRequestHeaders() throws IOException {
        String uri = "/cards/requests";
        webClient.post()
                .uri(uri)
                .header(AUTHORIZATION, "Bearer " + accessToken(uri))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header(X_AUTH_HEADER, "Bearer abc")
                .bodyValue(fromFile(REQUEST_BODY_FILE_NAME))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Missing request header 'X-Connector-Base-Url' for method parameter of type String");
    }

    @Test public void canGetCardsWhenInboxTaskHasDifferentDescriptor() throws Exception {
        mockWorkdayApiResponse(getInboxTasksUri(), "mixed_inbox_tasks_1.json");
        mockWorkdayApiResponse(TIME_OFF_REQUEST_ID_1_API, "time_off_request_diff_desc_1.json");
        mockWorkdayApiResponse(TIME_OFF_REQUEST_ID_2_API, "time_off_request_diff_desc_2.json");

        final String actualResponseBody = requestCards(WORKDAY_AUTH_TOKEN, CARDS_REQUESTS_API, REQUEST_BODY_FILE_NAME, false)
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
