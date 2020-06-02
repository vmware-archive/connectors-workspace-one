/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.ws1connectors.workday.exceptions.InboxTaskException;
import com.vmware.ws1connectors.workday.exceptions.TimeOffTaskActionException;
import com.vmware.ws1connectors.workday.exceptions.TimeOffTaskNotFoundException;
import com.vmware.ws1connectors.workday.exceptions.UserException;
import com.vmware.ws1connectors.workday.exceptions.UserNotFoundException;
import com.vmware.ws1connectors.workday.models.Descriptor;
import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.models.WorkdayUser;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffTask;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import com.vmware.ws1connectors.workday.test.FileUtils;
import com.vmware.ws1connectors.workday.test.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static com.vmware.ws1connectors.workday.test.JsonUtils.convertToWorkdayResourceFromJson;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressFBWarnings(value = "WOC_WRITE_ONLY_COLLECTION_FIELD", justification = "Collections read by parameterized tests")
public class TimeOffTaskServiceTest extends ServiceTestsBase {
    private static final String NO_EMAIL = null;
    private static final Locale NO_LOCALE = null;
    private static final Locale NO_INBOX_TASK_ID = null;
    private static final Locale LOCALE = Locale.ENGLISH;
    private static final String EMAIL = "user1@example.com";
    private static final WorkdayUser WORKDAY_USER = getUser("user_info.json");
    private static final List<InboxTask> INBOX_TASKS = getInboxTasks("mixed_inbox_tasks.json");
    private static final List<InboxTask> NO_TIME_OFF_INBOX_TASKS = getInboxTasks("no_time_off_inbox_tasks.json");
    private static final String TIME_OFF_TASK = FileUtils.readFileAsString("time_off_request_1.json");
    private static final String TIME_OFF_TASK_2 = FileUtils.readFileAsString("time_off_request_2.json");
    private static final int TIME_OFF_DETAILS_API_INVOCATION_COUNT = 2;
    private static final String INBOX_TASK_ID = "fc844b7a8f6f813f79efb5ffd6115505";
    private static final String COMMENT = "comment";
    private static final String NO_COMMENT = null;
    private static final ClassPathResource TIMEOFF_TASK_ACTION_BODY_RESOURCE = new ClassPathResource("static/templates/time_off_task_action_body_template.json");
    private static final String HREF = "href";
    private static final String DESCRIPTOR = "Approval By Manager";
    private static final String DENIAL_DESCRIPTOR = "Denial By Manager";
    private static final String REASON = "reason";

    @Mock private UserService mockUserService;
    @Mock private InboxService mockInboxService;
    @Mock private Resource mockTimeOffTaskActionTemplate;
    @InjectMocks private TimeOffTaskService timeOffTaskService;

    @BeforeEach public void initialize() {
        setupRestClient(timeOffTaskService, "restClient");
    }

    @DisplayName("Get time off tasks tests")
    @Nested
    public class GetTimeOffTasks {

        @ParameterizedTest
        @MethodSource("com.vmware.ws1connectors.workday.services.TimeOffTaskServiceTest#invalidInputsForGetTimeOffTasks")
        public void whenGetTimeOffTasksProvidedWithInvalidInputs(final String baseUrl, final String workdayAuth,
                                                                 final String email, final Locale locale) {
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> timeOffTaskService.getTimeOffTasks(baseUrl, workdayAuth, email, locale));
            verifyWorkdayApiNeverInvoked();
            verifyGetUserApiNeverInvoked(baseUrl, workdayAuth, email);
            verifyGetTasksApiNeverInvoked(baseUrl, workdayAuth);
        }

        @Test public void timeOffTasksFound() {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.just(WORKDAY_USER));
            when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                .thenReturn(Flux.fromIterable(INBOX_TASKS));
            when(mockExchangeFunc.exchange(any()))
                .thenReturn(Mono.just(buildClientResponse(TIME_OFF_TASK)))
                .thenReturn(Mono.just(buildClientResponse(TIME_OFF_TASK_2)));

            final Flux<TimeOffTask> timeOffTasks = timeOffTaskService.getTimeOffTasks(BASE_URL, WORKDAY_TOKEN, EMAIL, LOCALE);

            StepVerifier.create(timeOffTasks)
                .expectNextMatches(timeOffTask -> isEquals(timeOffTask, "time_off_task_1.json"))
                .expectNextMatches(timeOffTask -> isEquals(timeOffTask, "time_off_task_2.json"))
                .verifyComplete();
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verify(mockInboxService).getTasks(eq(BASE_URL), eq(WORKDAY_TOKEN), any(WorkdayUser.class));
            verify(mockExchangeFunc, times(TIME_OFF_DETAILS_API_INVOCATION_COUNT)).exchange(any(ClientRequest.class));
        }

        @ParameterizedTest
        @MethodSource("com.vmware.ws1connectors.workday.services.TimeOffTaskServiceTest#getParamsForNoTimeOffTasksFound")
        public void noTimeOffTasksFound(final WorkdayUser user, final List<InboxTask> inboxTasks,
                                        final int userApiInvocationCount, final int inboxApiInvocationCount) {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.justOrEmpty(user));
            Optional.ofNullable(user)
                .ifPresent(workdayUser -> when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                    .thenReturn(Flux.fromIterable(inboxTasks)));

            final Flux<TimeOffTask> timeOffTasks = timeOffTaskService.getTimeOffTasks(BASE_URL, WORKDAY_TOKEN, EMAIL, LOCALE);

            StepVerifier.create(timeOffTasks)
                .verifyComplete();
            verify(mockUserService, times(userApiInvocationCount)).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verify(mockInboxService, times(inboxApiInvocationCount)).getTasks(eq(BASE_URL), eq(WORKDAY_TOKEN), any(WorkdayUser.class));
            verifyWorkdayApiNeverInvoked();
        }

        @Test public void userDetailsNotFoundToGetTimeOffTasks() {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.empty());

            final Flux<TimeOffTask> timeOffTasks = timeOffTaskService.getTimeOffTasks(BASE_URL, WORKDAY_TOKEN, EMAIL, LOCALE);

            StepVerifier.create(timeOffTasks)
                .expectError(UserNotFoundException.class)
                .verify(DURATION_2_SECONDS);
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verifyGetTasksApiNeverInvoked(BASE_URL, WORKDAY_TOKEN);
            verifyWorkdayApiNeverInvoked();
        }

        @Test public void userErrorOccursWhenGettingTimeOffTasks() {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.error(() -> new UserException(HttpStatus.INTERNAL_SERVER_ERROR)));

            final Flux<TimeOffTask> timeOffTasks = timeOffTaskService.getTimeOffTasks(BASE_URL, WORKDAY_TOKEN, EMAIL, LOCALE);

            StepVerifier.create(timeOffTasks)
                .expectError(UserException.class)
                .verify(DURATION_2_SECONDS);
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verifyGetTasksApiNeverInvoked(BASE_URL, WORKDAY_TOKEN);
            verifyWorkdayApiNeverInvoked();
        }

        @Test public void inboxTasksErrorOccursWhenGettingTimeOffTasks() {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.just(WORKDAY_USER));
            when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                .thenReturn(Flux.error(() -> new InboxTaskException(HttpStatus.INTERNAL_SERVER_ERROR)));

            final Flux<TimeOffTask> timeOffTasks = timeOffTaskService.getTimeOffTasks(BASE_URL, WORKDAY_TOKEN, EMAIL, LOCALE);

            StepVerifier.create(timeOffTasks)
                .expectError(InboxTaskException.class)
                .verify(DURATION_2_SECONDS);
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verify(mockInboxService).getTasks(eq(BASE_URL), eq(WORKDAY_TOKEN), any(WorkdayUser.class));
            verifyWorkdayApiNeverInvoked();
        }

        @ParameterizedTest
        @EnumSource(value = HttpStatus.class, names = {"INTERNAL_SERVER_ERROR", "BAD_REQUEST"})
        public void workdayApiErrorOccursWhenGettingTimeOffTasks(HttpStatus httpStatus) {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.just(WORKDAY_USER));
            when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                .thenReturn(Flux.fromIterable(INBOX_TASKS));
            mockWorkdayApiErrorResponse(httpStatus);

            final Flux<TimeOffTask> timeOffTasks = timeOffTaskService.getTimeOffTasks(BASE_URL, WORKDAY_TOKEN, EMAIL, LOCALE);

            StepVerifier.create(timeOffTasks)
                .verifyComplete();
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verify(mockInboxService).getTasks(eq(BASE_URL), eq(WORKDAY_TOKEN), any(WorkdayUser.class));
            verify(mockExchangeFunc, times(TIME_OFF_DETAILS_API_INVOCATION_COUNT)).exchange(any(ClientRequest.class));

        }
    }

    @DisplayName("Tests for approval Action")
    @Nested
    public class ApprovalActionTest {

        @ParameterizedTest
        @MethodSource("com.vmware.ws1connectors.workday.services.TimeOffTaskServiceTest#invalidInputsForExecuteTimeOffTaskAction")
        public void whenTimeOffTaskActionProvidedWithInvalidInputs(final String baseUrl, final String workdayAuth,
                                                                   final String email, final String inboxTaskId) {
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> timeOffTaskService.approveTimeOffTask(baseUrl, workdayAuth, email, inboxTaskId, COMMENT));
            verifyWorkdayApiNeverInvoked();
            verifyGetUserApiNeverInvoked(baseUrl, workdayAuth, email);
            verifyGetTasksApiNeverInvoked(baseUrl, workdayAuth);
        }

        @ParameterizedTest
        @MethodSource("com.vmware.ws1connectors.workday.services.TimeOffTaskServiceTest#getParamsForNoTimeOffTasksFound")
        public void noTimeOffTaskFoundToApprove(final WorkdayUser user, final List<InboxTask> inboxTasks,
                                                final int userApiInvocationCount, final int inboxApiInvocationCount) {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.justOrEmpty(user));
            Optional.ofNullable(user)
                .ifPresent(workdayUser -> when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                    .thenReturn(Flux.fromIterable(inboxTasks)));

            final Mono<Descriptor> timeOffTasks = timeOffTaskService.approveTimeOffTask(BASE_URL, WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID, COMMENT);

            StepVerifier.create(timeOffTasks)
                .verifyError(TimeOffTaskNotFoundException.class);
            verify(mockUserService, times(userApiInvocationCount)).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verify(mockInboxService, times(inboxApiInvocationCount)).getTasks(eq(BASE_URL), eq(WORKDAY_TOKEN), any(WorkdayUser.class));
            verifyWorkdayApiNeverInvoked();
        }

        @Test public void userDetailsNotFoundToApproveTimeOffTask() {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.empty());

            final Mono<Descriptor> timeOffTasks = timeOffTaskService.approveTimeOffTask(BASE_URL, WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID, COMMENT);

            StepVerifier.create(timeOffTasks)
                .expectError(UserNotFoundException.class)
                .verify(DURATION_2_SECONDS);
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verifyGetTasksApiNeverInvoked(BASE_URL, WORKDAY_TOKEN);
            verifyWorkdayApiNeverInvoked();
        }

        @Test public void userErrorOccursWhenApprovingTimeOffTask() {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.error(() -> new UserException(HttpStatus.BAD_REQUEST)));

            final Mono<Descriptor> timeOffTasks = timeOffTaskService.approveTimeOffTask(BASE_URL, WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID, COMMENT);

            StepVerifier.create(timeOffTasks)
                .expectError(UserException.class)
                .verify(DURATION_2_SECONDS);
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verifyGetTasksApiNeverInvoked(BASE_URL, WORKDAY_TOKEN);
            verifyWorkdayApiNeverInvoked();
        }

        @Test public void inboxTasksErrorOccursWhenApprovingTimeOffTasks() {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.just(WORKDAY_USER));
            when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                .thenReturn(Flux.error(() -> new InboxTaskException(HttpStatus.INTERNAL_SERVER_ERROR)));

            final Mono<Descriptor> timeOffTasks = timeOffTaskService.approveTimeOffTask(BASE_URL, WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID, COMMENT);

            StepVerifier.create(timeOffTasks)
                .expectError(InboxTaskException.class)
                .verify(DURATION_2_SECONDS);
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verify(mockInboxService).getTasks(eq(BASE_URL), eq(WORKDAY_TOKEN), any(WorkdayUser.class));
            verifyWorkdayApiNeverInvoked();
        }

        @ParameterizedTest
        @EnumSource(value = HttpStatus.class, names = {"INTERNAL_SERVER_ERROR", "BAD_REQUEST"})
        public void workdayApiErrorOccursWhenApprovingTimeOffTask(HttpStatus httpStatus) throws IOException {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.just(WORKDAY_USER));
            when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                .thenReturn(Flux.fromIterable(INBOX_TASKS));
            when(mockTimeOffTaskActionTemplate.getInputStream())
                .thenReturn(TIMEOFF_TASK_ACTION_BODY_RESOURCE.getInputStream());
            mockWorkdayApiErrorResponse(httpStatus);

            final Mono<Descriptor> timeOffTasks = timeOffTaskService.approveTimeOffTask(BASE_URL, WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID, COMMENT);

            StepVerifier.create(timeOffTasks)
                .expectError(TimeOffTaskActionException.class)
                .verify(DURATION_2_SECONDS);
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verify(mockInboxService).getTasks(eq(BASE_URL), eq(WORKDAY_TOKEN), any(WorkdayUser.class));
            verify(mockExchangeFunc).exchange(any(ClientRequest.class));
        }

        @ParameterizedTest
        @MethodSource("com.vmware.ws1connectors.workday.services.TimeOffTaskServiceTest#getCommentInputs")
        public void canApproveTimeOffTask(final String comment) throws IOException {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.just(WORKDAY_USER));
            when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                .thenReturn(Flux.fromIterable(INBOX_TASKS));
            when(mockTimeOffTaskActionTemplate.getInputStream())
                .thenReturn(TIMEOFF_TASK_ACTION_BODY_RESOURCE.getInputStream());
            final Descriptor expectedResponseDesc = Descriptor.builder().id(INBOX_TASK_ID)
                .id(HREF)
                .descriptor(DESCRIPTOR)
                .build();
            when(mockExchangeFunc.exchange(any()))
                .thenReturn(Mono.just(buildClientResponse(JsonUtils.convertToJson(expectedResponseDesc))));

            final Mono<Descriptor> timeOffTasks = timeOffTaskService.approveTimeOffTask(BASE_URL, WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID, comment);

            StepVerifier.create(timeOffTasks)
                .expectNextMatches(actualDesc -> isEquals(actualDesc, expectedResponseDesc))
                .verifyComplete();
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verify(mockInboxService).getTasks(eq(BASE_URL), eq(WORKDAY_TOKEN), any(WorkdayUser.class));
            verify(mockExchangeFunc).exchange(any(ClientRequest.class));
        }
    }

    @DisplayName("Tests for decline Action")
    @Nested
    public class DeclineActionTest {

        @ParameterizedTest
        @MethodSource("com.vmware.ws1connectors.workday.services.TimeOffTaskServiceTest#invalidInputsForExecuteTimeOffTaskAction")
        public void whenTimeOffTaskDeclineActionProvidedWithInvalidInputs(final String baseUrl, final String workdayAuth,
                                                                          final String email, final String inboxTaskId) {
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> timeOffTaskService.declineTimeOffTask(baseUrl, workdayAuth, email, inboxTaskId, COMMENT));
            verifyWorkdayApiNeverInvoked();
            verifyGetUserApiNeverInvoked(baseUrl, workdayAuth, email);
            verifyGetTasksApiNeverInvoked(baseUrl, workdayAuth);
        }

        @ParameterizedTest
        @MethodSource("com.vmware.ws1connectors.workday.services.TimeOffTaskServiceTest#getParamsForNoTimeOffTasksFound")
        public void noTimeOffTaskFoundToDecline(final WorkdayUser user, final List<InboxTask> inboxTasks,
                                                final int userApiInvocationCount, final int inboxApiInvocationCount) {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.justOrEmpty(user));
            Optional.ofNullable(user)
                .ifPresent(workdayUser -> when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                    .thenReturn(Flux.fromIterable(inboxTasks)));

            final Mono<Descriptor> timeOffTasks = timeOffTaskService.declineTimeOffTask(BASE_URL, WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID, COMMENT);

            StepVerifier.create(timeOffTasks)
                .verifyError(TimeOffTaskNotFoundException.class);
            verify(mockUserService, times(userApiInvocationCount)).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verify(mockInboxService, times(inboxApiInvocationCount)).getTasks(eq(BASE_URL), eq(WORKDAY_TOKEN), any(WorkdayUser.class));
            verifyWorkdayApiNeverInvoked();
        }

        @Test public void userDetailsNotFoundToDeclineTimeOffTask() {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.empty());

            final Mono<Descriptor> timeOffTasks = timeOffTaskService.declineTimeOffTask(BASE_URL, WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID, COMMENT);

            StepVerifier.create(timeOffTasks)
                .expectError(UserNotFoundException.class)
                .verify(DURATION_2_SECONDS);
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verifyGetTasksApiNeverInvoked(BASE_URL, WORKDAY_TOKEN);
            verifyWorkdayApiNeverInvoked();
        }

        @Test public void userErrorOccursWhenDecliningTimeOffTask() {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.error(() -> new UserException(HttpStatus.INTERNAL_SERVER_ERROR)));

            final Mono<Descriptor> timeOffTasks = timeOffTaskService.declineTimeOffTask(BASE_URL, WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID, COMMENT);

            StepVerifier.create(timeOffTasks)
                .expectError(UserException.class)
                .verify(DURATION_2_SECONDS);
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verifyGetTasksApiNeverInvoked(BASE_URL, WORKDAY_TOKEN);
            verifyWorkdayApiNeverInvoked();
        }

        @Test public void inboxTasksErrorOccursWhenDecliningTimeOffTasks() {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.just(WORKDAY_USER));
            when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                .thenReturn(Flux.error(() -> new InboxTaskException(HttpStatus.BAD_REQUEST)));

            final Mono<Descriptor> timeOffTasks = timeOffTaskService.declineTimeOffTask(BASE_URL, WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID, COMMENT);

            StepVerifier.create(timeOffTasks)
                .expectError(InboxTaskException.class)
                .verify(DURATION_2_SECONDS);
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verify(mockInboxService).getTasks(eq(BASE_URL), eq(WORKDAY_TOKEN), any(WorkdayUser.class));
            verifyWorkdayApiNeverInvoked();
        }

        @ParameterizedTest
        @EnumSource(value = HttpStatus.class, names = {"INTERNAL_SERVER_ERROR", "BAD_REQUEST"})
        public void workdayApiErrorOccursWhenDecliningTimeOffTask(HttpStatus httpStatus) throws IOException {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.just(WORKDAY_USER));
            when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                .thenReturn(Flux.fromIterable(INBOX_TASKS));
            when(mockTimeOffTaskActionTemplate.getInputStream())
                .thenReturn(TIMEOFF_TASK_ACTION_BODY_RESOURCE.getInputStream());
            mockWorkdayApiErrorResponse(httpStatus);

            final Mono<Descriptor> timeOffTasks = timeOffTaskService.declineTimeOffTask(BASE_URL, WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID, COMMENT);

            StepVerifier.create(timeOffTasks)
                .expectError(TimeOffTaskActionException.class)
                .verify(DURATION_2_SECONDS);
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verify(mockInboxService).getTasks(eq(BASE_URL), eq(WORKDAY_TOKEN), any(WorkdayUser.class));
            verify(mockExchangeFunc).exchange(any(ClientRequest.class));
        }

        @Test public void canDeclineTimeOffTask() throws IOException {
            when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.just(WORKDAY_USER));
            when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                .thenReturn(Flux.fromIterable(INBOX_TASKS));
            when(mockTimeOffTaskActionTemplate.getInputStream())
                .thenReturn(TIMEOFF_TASK_ACTION_BODY_RESOURCE.getInputStream());
            final Descriptor expectedResponseDesc = Descriptor.builder().id(INBOX_TASK_ID)
                .id(HREF)
                .descriptor(DENIAL_DESCRIPTOR)
                .build();
            when(mockExchangeFunc.exchange(any()))
                .thenReturn(Mono.just(buildClientResponse(JsonUtils.convertToJson(expectedResponseDesc))));

            final Mono<Descriptor> timeOffTasks = timeOffTaskService.declineTimeOffTask(BASE_URL, WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID, REASON);

            StepVerifier.create(timeOffTasks)
                .expectNextMatches(actualDesc -> isEquals(actualDesc, expectedResponseDesc))
                .verifyComplete();
            verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
            verify(mockInboxService).getTasks(eq(BASE_URL), eq(WORKDAY_TOKEN), any(WorkdayUser.class));
            verify(mockExchangeFunc).exchange(any(ClientRequest.class));
        }
    }

    private static Stream<Arguments> invalidInputsForGetTimeOffTasks() {
        return new ArgumentsStreamBuilder()
            .add(NO_BASE_URL, WORKDAY_TOKEN, EMAIL, LOCALE)
            .add(BASE_URL, NO_WORKDAY_TOKEN, EMAIL, LOCALE)
            .add(BASE_URL, WORKDAY_TOKEN, NO_EMAIL, LOCALE)
            .add(BASE_URL, WORKDAY_TOKEN, EMAIL, NO_LOCALE)
            .build();
    }

    private static Stream<Arguments> invalidInputsForExecuteTimeOffTaskAction() {
        return new ArgumentsStreamBuilder()
            .add(NO_BASE_URL, WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID)
            .add(BASE_URL, NO_WORKDAY_TOKEN, EMAIL, INBOX_TASK_ID)
            .add(BASE_URL, WORKDAY_TOKEN, NO_EMAIL, INBOX_TASK_ID)
            .add(BASE_URL, WORKDAY_TOKEN, EMAIL, NO_INBOX_TASK_ID)
            .build();
    }

    private static Stream<Arguments> getParamsForNoTimeOffTasksFound() {
        return new ArgumentsStreamBuilder()
            .add(WORKDAY_USER, Collections.emptyList(), 1, 1)
            .add(WORKDAY_USER, NO_TIME_OFF_INBOX_TASKS, 1, 1)
            .build();
    }

    private static Stream<Arguments> getCommentInputs() {
        return new ArgumentsStreamBuilder()
            .add(COMMENT)
            .add(NO_COMMENT)
            .build();
    }

    private static List<InboxTask> getInboxTasks(final String inboxTasksFile) {
        final String inboxTasks = FileUtils.readFileAsString(inboxTasksFile);
        return convertToWorkdayResourceFromJson(inboxTasks, InboxTask.class).getData();
    }

    private static WorkdayUser getUser(final String userInfoFile) {
        final String userInfo = FileUtils.readFileAsString(userInfoFile);
        return convertToWorkdayResourceFromJson(userInfo, WorkdayUser.class).getData().get(0);
    }

    private Flux<InboxTask> verifyGetTasksApiNeverInvoked(String baseUrl, String workdayAuth) {
        return verify(mockInboxService, never()).getTasks(eq(baseUrl), eq(workdayAuth), any(WorkdayUser.class));
    }

    private void verifyGetUserApiNeverInvoked(String baseUrl, String workdayAuth, String email) {
        verify(mockUserService, never()).getUser(baseUrl, workdayAuth, email);
    }

}
