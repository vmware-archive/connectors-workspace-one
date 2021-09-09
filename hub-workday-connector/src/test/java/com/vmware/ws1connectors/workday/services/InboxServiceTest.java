/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.ws1connectors.workday.exceptions.InboxTaskException;
import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import com.vmware.ws1connectors.workday.test.FileUtils;
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

class InboxServiceTest extends ServiceTestsBase {
    private static final String EMAIL = "user1@example.com";

    private static final String INBOX_TASKS = FileUtils.readFileAsString("inbox_tasks.json");

    @InjectMocks
    private InboxService inboxService;

    @BeforeEach
    void initialize() {
        setupRestClient(inboxService, "restClient");
    }

    private static Stream<Arguments> invalidInputsForGetTasks() {
        return new ArgumentsStreamBuilder()
                .add(NO_BASE_URL, WORKDAY_TOKEN)
                .add(BASE_URL, NO_WORKDAY_TOKEN)
                .build();
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForGetTasks")
    void whenGetTasksProvidedWithInvalidInputs(final String baseUrl, final String workdayAuth) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> inboxService.getTasks(baseUrl, workdayAuth, EMAIL));
        verifyWorkdayApiNeverInvoked();
    }

    @Test
    void tasksFoundInTheInbox() {
        mockWorkdayApiResponse(INBOX_TASKS);

        final Flux<InboxTask> inboxTasks = inboxService.getTasks(BASE_URL, WORKDAY_TOKEN, EMAIL);

        StepVerifier.create(inboxTasks)
                .expectNextMatches(inboxTask -> isEquals(inboxTask, "inbox_task.json"))
                .expectNextMatches(inboxTask -> isEquals(inboxTask, "inbox_task_2.json"))
                .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(strings = {"no_data_total_zero.json", "no_data_total_non_zero.json", "inbox_tasks_total_zero.json"})
    void noInboxTasksFound(final String inboxTasksResponseFile) {
        final String inboxTasksResponse = FileUtils.readFileAsString(inboxTasksResponseFile);
        mockWorkdayApiResponse(inboxTasksResponse);

        final Flux<InboxTask> inboxTasks = inboxService.getTasks(BASE_URL, WORKDAY_TOKEN, EMAIL);

        StepVerifier.create(inboxTasks)
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"INTERNAL_SERVER_ERROR", "BAD_REQUEST"})
    void errorOccursWhenGettingTheUser(HttpStatus httpStatus) {
        mockWorkdayApiErrorResponse(httpStatus);

        final Flux<InboxTask> inboxTasks = inboxService.getTasks(BASE_URL, WORKDAY_TOKEN, EMAIL);

        StepVerifier.create(inboxTasks)
                .expectError(InboxTaskException.class)
                .verify(DURATION_2_SECONDS);
    }

}
