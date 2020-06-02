/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.web;

import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.ws1connectors.workday.test.FileUtils;
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

import static com.vmware.ws1connectors.workday.test.FileUtils.readFileAsString;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.ACTION_TYPE_QUERY_PARAM;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASKS_API_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASKS_SUMMARY;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASKS_VIEW_QUERY_PARAM_NAME;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.INBOX_TASK_ID_PATH_VARIABLE;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TASK_ACTION_APPROVAL;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TASK_ACTION_DENIAL;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TIMEOFF_TASK_APPROVE_ACTION_API_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TIMEOFF_TASK_DECLINE_ACTION_API_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.URL_PATH_SEPARATOR;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.USER_INFO;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.WORKDAY_CONNECTOR_CONTEXT_PATH;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.WORKERS_INBOX_TASKS_API;
import static com.vmware.ws1connectors.workday.utils.CardConstants.COMMENT_KEY;
import static com.vmware.ws1connectors.workday.utils.CardConstants.REASON_KEY;
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

public class TimeOffTaskActionControllerTest extends ControllerTestsBase {
    private static final String BEARER = "Bearer ";
    private static final String UNAUTHORIZED_WORKDAY_AUTH_TOKEN = BEARER + "unauthorized-auth-token";
    private static final String WORKDAY_AUTH_TOKEN = BEARER + "valid-auth-token";
    private static final String NO_WORKDAY_AUTH_TOKEN = null;
    private static final String ROUTING_PREFIX = "https://dev.hero.example.com/connectors/id";
    private static final String INBOX_TASK_ID = "fc844b7a8f6f813f79efb5ffd6115505";
    private static final String REASON_VALUE = "declined!";
    private static final String COMMENT_VALUE = "Approved!";

    @ParameterizedTest
    @ValueSource(strings = {TIMEOFF_TASK_APPROVE_ACTION_API_PATH, TIMEOFF_TASK_DECLINE_ACTION_API_PATH})
    public void testTimeOffTaskActionApiAreProtected(final String actionPath) throws Exception {
        testProtectedResource(POST, getActionUrl(actionPath));
    }

    @DisplayName("Time off Task approval integration tests")
    @Nested
    public class ApprovalActionTest {

        @Test public void approveActionFailsWhenUnauthorizedWorkdayAuthTokenIsProvided() {
            mockBackend.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

            executeApproveAction(UNAUTHORIZED_WORKDAY_AUTH_TOKEN, buildFormData(COMMENT_KEY, COMMENT_VALUE))
                .expectStatus().isBadRequest();
        }

        @Test public void approveActionFailsWhenWorkdayAuthTokenIsMissing() {
            executeApproveAction(NO_WORKDAY_AUTH_TOKEN, buildFormData(COMMENT_KEY, COMMENT_VALUE))
                .expectStatus().isBadRequest();
        }

        @Test public void approveActionFailsWhenUserNotFound() {
            mockWorkdayApiResponse(GET, USER_INFO, "no_results.json");

            executeApproveAction(WORKDAY_AUTH_TOKEN, buildFormData(COMMENT_KEY, COMMENT_VALUE))
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON);
        }

        @Test public void approveActionFailsWheTimeOffTaskNotFound() {
            mockWorkdayApiResponse(GET, USER_INFO, "fred_user_info.json");
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "no_results.json");

            executeApproveAction(WORKDAY_AUTH_TOKEN, buildFormData(COMMENT_KEY, COMMENT_VALUE))
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "yep")
        public void canApproveTask(final String comment) {
            mockWorkdayApiResponse(GET, USER_INFO, "fred_user_info.json");
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "mixed_inbox_tasks.json");
            mockWorkdayApiResponse(PUT, getWorkdayInboxTasksApprovalUri(), "task_action_success.json");

            executeApproveAction(WORKDAY_AUTH_TOKEN, buildFormData(COMMENT_KEY, comment))
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
        }

        @ParameterizedTest
        @EnumSource(value = HttpStatus.class, names = {"INTERNAL_SERVER_ERROR", "BAD_REQUEST"})
        public void whenApproveTaskFails(final HttpStatus status) {
            mockWorkdayApiResponse(GET, USER_INFO, "fred_user_info.json");
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "mixed_inbox_tasks.json");
            mockWorkdayApiErrorResponse(PUT, getWorkdayInboxTasksApprovalUri(), status);

            executeApproveAction(WORKDAY_AUTH_TOKEN, buildFormData(COMMENT_KEY, COMMENT_VALUE))
                .expectStatus().isEqualTo(HttpStatus.FAILED_DEPENDENCY)
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON);
        }
    }

    @DisplayName("Time off Task decline integration tests")
    @Nested
    public class DeclineActionTest {

        @Test public void declineActionFailsWhenUnauthorizedWorkdayAuthTokenIsProvided() {
            mockBackend.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

            executeDeclineAction(UNAUTHORIZED_WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, REASON_VALUE))
                .expectStatus().isBadRequest();
        }

        @Test public void declineActionFailsWhenWorkdayAuthTokenIsMissing() {
            executeDeclineAction(NO_WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, REASON_VALUE))
                .expectStatus().isBadRequest();
        }

        @Test public void declineActionFailsWhenUserNotFound() {
            mockWorkdayApiResponse(GET, USER_INFO, "no_results.json");

            executeDeclineAction(WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, REASON_VALUE))
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON);
        }

        @Test public void declineActionFailsWheTimeOffTaskNotFound() {
            mockWorkdayApiResponse(GET, USER_INFO, "fred_user_info.json");
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "no_results.json");

            executeDeclineAction(WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, REASON_VALUE))
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON);
        }

        @Test public void canDeclineTask() {
            mockWorkdayApiResponse(GET, USER_INFO, "fred_user_info.json");
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "mixed_inbox_tasks.json");
            mockWorkdayApiResponse(PUT, getWorkdayInboxTasksDenialUri(), "task_action_success.json");

            executeDeclineAction(WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, REASON_VALUE))
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {SPACE, LF, CR})
        public void canNotDeclineTaskWithEmptyReason(final String reason) {
            executeDeclineAction(WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, reason))
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(FileUtils.readFileAsString("decline_action_validation_error_response.json"));
        }

        @ParameterizedTest
        @EnumSource(value = HttpStatus.class, names = {"INTERNAL_SERVER_ERROR", "BAD_REQUEST"})
        public void whenDeclineTaskFails(final HttpStatus status) {
            mockWorkdayApiResponse(GET, USER_INFO, "fred_user_info.json");
            mockWorkdayApiResponse(GET, getWorkdayInboxTasksUri(), "mixed_inbox_tasks.json");
            mockWorkdayApiErrorResponse(PUT, getWorkdayInboxTasksDenialUri(), status);

            executeDeclineAction(WORKDAY_AUTH_TOKEN, buildFormData(REASON_KEY, REASON_VALUE))
                .expectStatus().isEqualTo(HttpStatus.FAILED_DEPENDENCY)
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON);
        }
    }

    private String getWorkdayInboxTasksApprovalUri() {
        return getWorkdayInboxTasksActionUri(TASK_ACTION_APPROVAL);
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
        return UriComponentsBuilder.fromPath(WORKERS_INBOX_TASKS_API)
            .queryParam(INBOX_TASKS_VIEW_QUERY_PARAM_NAME, INBOX_TASKS_SUMMARY)
            .build()
            .toUriString();
    }

    private void mockWorkdayApiResponse(final HttpMethod method, final String workdayApi, final String responseFile) {
        mockBackend.expect(requestTo(workdayApi))
            .andExpect(header(AUTHORIZATION, WORKDAY_AUTH_TOKEN))
            .andExpect(method(method))
            .andRespond(withSuccess(readFileAsString(responseFile), APPLICATION_JSON));
    }

    private void mockWorkdayApiErrorResponse(final HttpMethod method, final String workdayApi, final HttpStatus status) {
        mockBackend.expect(requestTo(workdayApi))
            .andExpect(header(AUTHORIZATION, WORKDAY_AUTH_TOKEN))
            .andExpect(method(method))
            .andRespond(withStatus(status));
    }

    private WebTestClient.ResponseSpec executeApproveAction(final String authToken, final MultiValueMap<String, String> formdata) {
        return doPost(getApproveActionUrl(), authToken, formdata);
    }

    private String getApproveActionUrl() {
        return getActionUrl(TIMEOFF_TASK_APPROVE_ACTION_API_PATH);
    }

    private String getActionUrl(final String actionPath) {
        return UriComponentsBuilder.fromPath(WORKDAY_CONNECTOR_CONTEXT_PATH)
            .path(URL_PATH_SEPARATOR)
            .path(actionPath)
            .build(singletonMap(INBOX_TASK_ID_PATH_VARIABLE, INBOX_TASK_ID))
            .toString();
    }

    private String getDeclineActionUrl() {
        return getActionUrl(TIMEOFF_TASK_DECLINE_ACTION_API_PATH);
    }

    private WebTestClient.ResponseSpec executeDeclineAction(final String authToken, final MultiValueMap<String, String> formData) {
        return doPost(getDeclineActionUrl(), authToken, formData);
    }

    private MultiValueMap<String, String> buildFormData(final String fieldName, final String value) {
        return new LinkedMultiValueMap<>(singletonMap(fieldName, singletonList(value)));
    }

}
