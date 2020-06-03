/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.ws1connectors.workday.exceptions.InboxTaskException;
import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.models.WorkdayUser;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import com.vmware.ws1connectors.workday.test.FileUtils;
import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class InboxServiceTest extends ServiceTestsBase {
    private static final String NO_USER = null;

    private static final WorkdayUser USER = JsonUtils.convertFromJsonFile("user_info.json", WorkdayUser.class);
    private static final String INBOX_TASKS = FileUtils.readFileAsString("inbox_tasks.json");

    @InjectMocks private InboxService inboxService;

    @BeforeEach public void initialize() {
        setupRestClient(inboxService, "restClient");
    }

    private static Stream<Arguments> invalidInputsForGetTasks() {
        return new ArgumentsStreamBuilder()
            .add(NO_BASE_URL, WORKDAY_TOKEN, USER)
            .add(BASE_URL, NO_WORKDAY_TOKEN, USER)
            .add(BASE_URL, WORKDAY_TOKEN, NO_USER)
            .build();
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForGetTasks")
    public void whenGetTasksProvidedWithInvalidInputs(final String baseUrl, final String workdayAuth, final WorkdayUser user) {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> inboxService.getTasks(baseUrl, workdayAuth, user));
        verifyWorkdayApiNeverInvoked();
    }

    @Test public void tasksFoundInTheInbox() {
        mockWorkdayApiResponse(INBOX_TASKS);

        final Flux<InboxTask> inboxTasks = inboxService.getTasks(BASE_URL, WORKDAY_TOKEN, USER);

        StepVerifier.create(inboxTasks)
            .expectNextMatches(inboxTask -> isEquals(inboxTask, "inbox_task.json"))
            .expectNextMatches(inboxTask -> isEquals(inboxTask, "inbox_task_2.json"))
            .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(strings = {"no_data_total_zero.json", "no_data_total_non_zero.json", "inbox_tasks_total_zero.json"})
    public void noInboxTasksFound(final String inboxTasksResponseFile) {
        final String inboxTasksResponse = FileUtils.readFileAsString(inboxTasksResponseFile);
        mockWorkdayApiResponse(inboxTasksResponse);

        final Flux<InboxTask> inboxTasks = inboxService.getTasks(BASE_URL, WORKDAY_TOKEN, USER);

        StepVerifier.create(inboxTasks)
            .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"INTERNAL_SERVER_ERROR", "BAD_REQUEST"})
    public void errorOccursWhenGettingTheUser(HttpStatus httpStatus) {
        mockWorkdayApiErrorResponse(httpStatus);

        final Flux<InboxTask> inboxTasks = inboxService.getTasks(BASE_URL, WORKDAY_TOKEN, USER);

        StepVerifier.create(inboxTasks)
            .expectError(InboxTaskException.class)
            .verify(DURATION_2_SECONDS);
    }

}
