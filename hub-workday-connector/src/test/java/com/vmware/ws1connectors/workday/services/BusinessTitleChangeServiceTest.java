/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.ws1connectors.workday.exceptions.BusinessTitleChangeException;
import com.vmware.ws1connectors.workday.models.BusinessTitleChangeTask;
import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import com.vmware.ws1connectors.workday.test.FileUtils;
import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BusinessTitleChangeServiceTest extends ServiceTestsBase {

    private static final InboxTask INBOX_TASK =
            JsonUtils.convertFromJsonFile("Inbox_Task_Business_Title_Change.json", InboxTask.class);
    private static final String BUSINESS_TITLE_CHANGE = FileUtils.readFileAsString("business_title_change.json");
    private static final InboxTask NO_INBOX_TASK = null;
    private static final Locale NO_LOCALE = null;
    private static final Locale LOCALE = Locale.ENGLISH;

    @InjectMocks
    private BusinessTitleChangeService businessTitleChangeService;

    @BeforeEach
    void initialize() {
        setupRestClient(businessTitleChangeService, "restClient");
    }

    @Test
    void testGetBusinessTitleChangesDetails() {
        when(mockExchangeFunc.exchange(any()))
                .thenReturn(Mono.just(buildClientResponse(BUSINESS_TITLE_CHANGE)));
        final Mono<BusinessTitleChangeTask> approvalTaskDetails =
                businessTitleChangeService.getApprovalTaskDetails(BASE_URL, WORKDAY_TOKEN, INBOX_TASK, LOCALE);
        StepVerifier.create(approvalTaskDetails)
                .expectNextMatches(businessTitleChangeTask -> isEquals(businessTitleChangeTask, "business_title_change.json"))
                .verifyComplete();
    }

    @Test
    void whenWorkdayApiResultsInErrorThrowsException() {
        when(mockExchangeFunc.exchange(any()))
                .thenThrow(RuntimeException.class);
        final Mono<BusinessTitleChangeTask> approvalTaskDetails =
                businessTitleChangeService.getApprovalTaskDetails(BASE_URL, WORKDAY_TOKEN, INBOX_TASK, LOCALE);
        StepVerifier.create(approvalTaskDetails)
                .expectError(RuntimeException.class)
                .verify();
    }

    @ParameterizedTest
    @MethodSource("inputsWithHttpErrors")
    void whenWorkdayApiHttpStatusIsErrorThrowsException(HttpStatus status) {
        mockWorkdayApiErrorResponse(status);
        final Mono<BusinessTitleChangeTask> approvalTaskDetails =
                businessTitleChangeService.getApprovalTaskDetails(BASE_URL, WORKDAY_TOKEN, INBOX_TASK, LOCALE);
        StepVerifier.create(approvalTaskDetails)
                .expectError(BusinessTitleChangeException.class)
                .verify();
    }

    private static Stream<Arguments> inputsWithHttpErrors() {
        return new ArgumentsStreamBuilder()
                .add(HttpStatus.BAD_REQUEST)
                .add(HttpStatus.UNAUTHORIZED)
                .add(HttpStatus.FAILED_DEPENDENCY)
                .build();
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForGetApprovalTaskDetails")
    void whenGetTimeOffTasksProvidedWithInvalidInputs(final String baseUrl, final String workdayAuth,
                                                      final InboxTask inboxTask, final Locale locale) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> businessTitleChangeService.getApprovalTaskDetails(baseUrl, workdayAuth, inboxTask, locale));
        verifyWorkdayApiNeverInvoked();
    }

    private static Stream<Arguments> invalidInputsForGetApprovalTaskDetails() {
        return new ArgumentsStreamBuilder()
                .add(NO_BASE_URL, WORKDAY_TOKEN, INBOX_TASK, LOCALE)
                .add(BASE_URL, NO_WORKDAY_TOKEN, INBOX_TASK, LOCALE)
                .add(BASE_URL, WORKDAY_TOKEN, NO_INBOX_TASK, LOCALE)
                .add(BASE_URL, WORKDAY_TOKEN, INBOX_TASK, NO_LOCALE)
                .build();
    }
}
