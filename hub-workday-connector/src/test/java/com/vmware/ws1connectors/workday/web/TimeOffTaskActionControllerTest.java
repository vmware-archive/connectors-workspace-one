/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.web;

import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.ws1connectors.workday.test.FileUtils;
import com.vmware.ws1connectors.workday.utils.CardConstants;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import static com.vmware.ws1connectors.workday.test.FileUtils.readFileAsString;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.ACTION_TYPE_QUERY_PARAM;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.APPROVE_EVENT_STEP_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.BUSINESS_PROCESS_API_V1;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.COMMUNITY_COMMON_API_V1;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.DECLINE_EVENT_STEP_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASKS_API_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASKS_SUMMARY;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASKS_VIEW_QUERY_PARAM_NAME;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASK_ID_PATH_VARIABLE;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TASK_ACTION_APPROVAL;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TASK_ACTION_DENIAL;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TIMEOFF_TASK_APPROVE_ACTION_API_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TIMEOFF_TASK_DECLINE_ACTION_API_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.URL_PATH_SEPARATOR;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.WORKERS_INBOX_TASKS_API;
import static com.vmware.ws1connectors.workday.utils.CardConstants.COMMENT_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.REASON_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TENANT_NAME;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.ROUTING_PREFIX_HEADER;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.StringUtils.CR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.hamcrest.Matchers.any;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class TimeOffTaskActionControllerTest extends ControllerTestsBase {
    private static final String BEARER = "Bearer ";
    private static final String UNAUTHORIZED_WORKDAY_AUTH_TOKEN = BEARER + "unauthorized-auth-token";
    private static final String WORKDAY_AUTH_TOKEN = BEARER + "valid-auth-token";
    private static final String NO_WORKDAY_AUTH_TOKEN = null;
    private static final String ROUTING_PREFIX = "https://dev.hero.example.com/connectors/id";
    private static final String INBOX_TASK_ID = "fc844b7a8f6f813f79efb5ffd6115505";
    private static final String BUSINESS_PROCESS_INBOX_ID = "f387bb35571f81be3262cd2c3a488314";
    private static final String REASON_VALUE = "declined!";
    private static final String COMMENT_VALUE = "Approved!";
    private static final String TENANT_NAME = "vmware_gms";

    @ParameterizedTest
    @ValueSource(strings = {TIMEOFF_TASK_APPROVE_ACTION_API_PATH, TIMEOFF_TASK_DECLINE_ACTION_API_PATH})
    void testTimeOffTaskActionApiAreProtected(final String actionPath) throws Exception {
        testProtectedResource(POST, getActionUrl(actionPath, INBOX_TASK_ID));
    }

    @DisplayName("Time off Task approval integration tests")
    @Nested
    class ApprovalActionTest {

        @Test
        void approveActionFailsWhenUnauthorizedWorkdayAuthTokenIsProvided() {
            mockBackend.expect(requestTo(any(String.class)))
                    .andRespond(withUnauthorizedRequest());

            executeApproveAction(UNAUTHORIZED_WORKDAY_AUTH_TOKEN, buildFormData(COMMENT_KEY, COMMENT_VALUE, TENANT_NAME), INBOX_TASK_ID)
                    .expectStatus().isBadRequest();
        }

        @Test
        void approveActionFailsWhenWorkdayAuthTokenIsMissing() {
            executeApproveAction(NO_WORKDAY_AUTH_TOKEN, buildFormData(COMMENT_KEY, COMMENT_VALUE, TENANT_NAME), INBOX_TASK_ID)
                    .expectStatus().isBadRequest();
        }

        @Test
        void approveActionFailsWheTimeOffTaskNotFound() {
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "no_results.json");
            executeApproveAction(WORKDAY_AUTH_TOKEN, buildFormData(COMMENT_KEY, COMMENT_VALUE, TENANT_NAME), INBOX_TASK_ID)
                    .expectStatus().isNotFound()
                    .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "yep")
        void canApproveTask(final String comment) {
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "mixed_inbox_tasks.json");
            mockWorkdayApiResponse(PUT, getWorkdayInboxTasksApprovalUri(), "task_action_success.json");

            executeApproveAction(WORKDAY_AUTH_TOKEN, buildFormData(COMMENT_KEY, comment, TENANT_NAME), INBOX_TASK_ID)
                    .expectStatus().isNoContent()
                    .expectBody().isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "yep")
        void canApproveBusinessProcessTask(final String comment) {
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "mixed_inbox_tasks_with_business_process.json");
            mockWorkdayApiResponse(POST, getBusinessProcessTaskApprovalUrl(), "task_action_success.json");
            executeApproveAction(WORKDAY_AUTH_TOKEN, buildFormData(COMMENT_KEY, comment, TENANT_NAME), BUSINESS_PROCESS_INBOX_ID)
                    .expectStatus().isNoContent()
                    .expectBody().isEmpty();
        }

        @Test
        void canDeclineBusinessProcessTask() {
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "mixed_inbox_tasks_with_business_process.json");
            mockWorkdayApiResponse(POST, getBusinessProcessTaskDeclineUrl(), "task_action_success.json");
            executeDeclineAction(WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, REASON_VALUE, TENANT_NAME), BUSINESS_PROCESS_INBOX_ID)
                    .expectStatus().isNoContent()
                    .expectBody().isEmpty();
        }

        @ParameterizedTest
        @EnumSource(value = HttpStatus.class, names = {"INTERNAL_SERVER_ERROR", "BAD_REQUEST"})
        void whenApproveTaskFails(final HttpStatus status) {
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "mixed_inbox_tasks.json");
            mockWorkdayApiErrorResponse(PUT, getWorkdayInboxTasksApprovalUri(), status);

            executeApproveAction(WORKDAY_AUTH_TOKEN, buildFormData(COMMENT_KEY, COMMENT_VALUE, TENANT_NAME), INBOX_TASK_ID)
                    .expectStatus().isEqualTo(HttpStatus.FAILED_DEPENDENCY)
                    .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON);
        }
    }

    @DisplayName("Time off Task decline integration tests")
    @Nested
    class DeclineActionTest {

        @Test
        void declineActionFailsWhenUnauthorizedWorkdayAuthTokenIsProvided() {
            mockBackend.expect(requestTo(any(String.class)))
                    .andRespond(withUnauthorizedRequest());

            executeDeclineAction(UNAUTHORIZED_WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, REASON_VALUE, TENANT_NAME), INBOX_TASK_ID)
                    .expectStatus().isBadRequest();
        }

        @Test
        void declineActionFailsWhenWorkdayAuthTokenIsMissing() {
            executeDeclineAction(NO_WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, REASON_VALUE, TENANT_NAME), INBOX_TASK_ID)
                    .expectStatus().isBadRequest();
        }

        @Test
        void declineActionFailsWheTimeOffTaskNotFound() {
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "no_results.json");

            executeDeclineAction(WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, REASON_VALUE, TENANT_NAME), INBOX_TASK_ID)
                    .expectStatus().isNotFound()
                    .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON);
        }

        @Test
        void canDeclineTask() {
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "mixed_inbox_tasks.json");
            mockWorkdayApiResponse(PUT, getWorkdayInboxTasksDenialUri(), "task_action_success.json");

            executeDeclineAction(WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, REASON_VALUE, TENANT_NAME), INBOX_TASK_ID)
                    .expectStatus().isNoContent()
                    .expectBody().isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {SPACE, LF, CR})
        void canNotDeclineTaskWithEmptyReason(final String reason) {
            executeDeclineAction(WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, reason, TENANT_NAME), INBOX_TASK_ID)
                    .expectStatus().isBadRequest()
                    .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                    .expectBody().json(FileUtils.readFileAsString("decline_action_validation_error_response.json"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {SPACE, LF, CR})
        void canNotDeclineTaskWithEmptyTenantName(final String tenantName) {
            executeDeclineAction(WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, REASON_VALUE, tenantName), INBOX_TASK_ID)
                    .expectStatus().isBadRequest()
                    .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                    .expectBody().json(FileUtils.readFileAsString("tenant_name_validation_error_response.json"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {SPACE, LF, CR})
        void canNotApproveTaskWithEmptyTenantName(final String tenantName) {
            executeApproveAction(WORKDAY_AUTH_TOKEN, buildFormData(COMMENT_KEY, COMMENT_VALUE, tenantName), INBOX_TASK_ID)
                    .expectStatus().isBadRequest()
                    .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                    .expectBody().json(FileUtils.readFileAsString("tenant_name_validation_error_response.json"));
        }

        @ParameterizedTest
        @EnumSource(value = HttpStatus.class, names = {"INTERNAL_SERVER_ERROR", "BAD_REQUEST"})
        void whenDeclineTaskFails(final HttpStatus status) {
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "mixed_inbox_tasks.json");
            mockWorkdayApiErrorResponse(PUT, getWorkdayInboxTasksDenialUri(), status);

            executeDeclineAction(WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, REASON_VALUE, TENANT_NAME), INBOX_TASK_ID)
                    .expectStatus().isEqualTo(HttpStatus.FAILED_DEPENDENCY)
                    .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON);
        }
    }

    private String getWorkdayInboxTasksApprovalUri() {
        return getWorkdayInboxTasksActionUri(TASK_ACTION_APPROVAL);
    }

    private String getBusinessProcessTaskApprovalUrl() {
        return UriComponentsBuilder.fromPath(BUSINESS_PROCESS_API_V1 + TENANT_NAME + APPROVE_EVENT_STEP_PATH)
                .build(Map.of("ID", BUSINESS_PROCESS_INBOX_ID)).toString();
    }

    private String getBusinessProcessTaskDeclineUrl() {
        return UriComponentsBuilder.fromPath(BUSINESS_PROCESS_API_V1 + TENANT_NAME + DECLINE_EVENT_STEP_PATH)
                .build(Map.of("ID", BUSINESS_PROCESS_INBOX_ID)).toString();
    }

    private String getWorkdayInboxTasksDenialUri() {
        return getWorkdayInboxTasksActionUri(TASK_ACTION_DENIAL);
    }

    private String getWorkdayInboxTasksActionUri(final String action) {
        return UriComponentsBuilder.fromPath(INBOX_TASKS_API_PATH)
                .path(URL_PATH_SEPARATOR)
                .path(INBOX_TASK_ID)
                .queryParam(ACTION_TYPE_QUERY_PARAM, singletonList(action))
                .build()
                .toUriString();
    }

    private WebTestClient.ResponseSpec doPost(final String path, final String authToken, final MultiValueMap<String, String> formData) {
        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(path)
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .accept(APPLICATION_JSON)
                .header(X_BASE_URL_HEADER, mockBackend.url(EMPTY))
                .header(ROUTING_PREFIX_HEADER, ROUTING_PREFIX)
                .headers(headers -> headers(headers, path));
        if (StringUtils.isNotBlank(authToken)) {
            spec = spec.header(X_AUTH_HEADER, authToken);
        }
        return spec.exchange();
    }

    private String getWorkdayInboxTasksUri() {
        return UriComponentsBuilder.fromPath(COMMUNITY_COMMON_API_V1 + TENANT_NAME + WORKERS_INBOX_TASKS_API)
                .queryParam(INBOX_TASKS_VIEW_QUERY_PARAM_NAME, INBOX_TASKS_SUMMARY)
                .build()
                .toUriString();
    }

    private void mockWorkdayApiResponse(final HttpMethod method, final String workdayApi, final String responseFile) {
        mockBackend.expect(requestTo(workdayApi))
                .andExpect(header(AUTHORIZATION, WORKDAY_AUTH_TOKEN))
                .andExpect(method(method))
                .andRespond(withSuccess(readFileAsString(responseFile).replace("{MOCK_BACKEND}/", mockBackend.url("/")), APPLICATION_JSON));
    }

    private void mockWorkdayApiErrorResponse(final HttpMethod method, final String workdayApi, final HttpStatus status) {
        mockBackend.expect(requestTo(workdayApi))
                .andExpect(header(AUTHORIZATION, WORKDAY_AUTH_TOKEN))
                .andExpect(method(method))
                .andRespond(withStatus(status));
    }

    private WebTestClient.ResponseSpec executeApproveAction(final String authToken, final MultiValueMap<String, String> formdata, String inboxTaskId) {
        return doPost(getApproveActionUrl(inboxTaskId), authToken, formdata);
    }

    private String getApproveActionUrl(String inboxTaskId) {
        return getActionUrl(TIMEOFF_TASK_APPROVE_ACTION_API_PATH, inboxTaskId);
    }

    private String getActionUrl(final String actionPath, String inboxTaskId) {
        return UriComponentsBuilder.fromPath(URL_PATH_SEPARATOR)
                .path(actionPath)
                .build(singletonMap(INBOX_TASK_ID_PATH_VARIABLE, inboxTaskId))
                .toString();
    }

    private String getDeclineActionUrl(String inboxTaskId) {
        return getActionUrl(TIMEOFF_TASK_DECLINE_ACTION_API_PATH, inboxTaskId);
    }

    private WebTestClient.ResponseSpec executeDeclineAction(final String authToken, final MultiValueMap<String, String> formData, String inboxTaskId) {
        return doPost(getDeclineActionUrl(inboxTaskId), authToken, formData);
    }

    private MultiValueMap<String, String> buildFormData(final String fieldName, final String value, String tenantName) {
        final MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<>();
        bodyMap.add(fieldName, value);
        bodyMap.add(CardConstants.TENANT_NAME, tenantName);
        return bodyMap;
    }

}
