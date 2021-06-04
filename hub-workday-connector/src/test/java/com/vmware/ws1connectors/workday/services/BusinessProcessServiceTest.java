/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.ws1connectors.workday.exceptions.BusinessProcessException;
import com.vmware.ws1connectors.workday.models.BusinessProcessTask;
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

public class BusinessProcessServiceTest extends ServiceTestsBase {
    private static final InboxTask INBOX_TASK =
            JsonUtils.convertFromJsonFile("Inbox_Task_Business_Process.json", InboxTask.class);
    private static final String BUSINESS_PROCESS_DETAILS = FileUtils.readFileAsString("Business_Process_Details.json");
    private static final InboxTask NO_INBOX_TASK = null;
    private static final Locale NO_LOCALE = null;
    private static final Locale LOCALE = Locale.ENGLISH;

    @InjectMocks private BusinessProcessService businessProcessService;

    @BeforeEach public void initialize() {
        setupRestClient(businessProcessService, "restClient");
    }

    @Test public void testGetBusinessProcessDetails() {
        when(mockExchangeFunc.exchange(any()))
                .thenReturn(Mono.just(buildClientResponse(BUSINESS_PROCESS_DETAILS)));
        final Mono<BusinessProcessTask> approvalTaskDetails =
                businessProcessService.getApprovalTaskDetails(BASE_URL, WORKDAY_TOKEN, INBOX_TASK, LOCALE);
        StepVerifier.create(approvalTaskDetails)
                .expectNextMatches(businessProcessTask -> isEquals(businessProcessTask, "Business_Process_Details.json"))
                .verifyComplete();
    }

    @Test public void whenWorkdayApiResultsInErrorThrowsException() {
        when(mockExchangeFunc.exchange(any()))
                .thenThrow(RuntimeException.class);
        final Mono<BusinessProcessTask> approvalTaskDetails =
                businessProcessService.getApprovalTaskDetails(BASE_URL, WORKDAY_TOKEN, INBOX_TASK, LOCALE);
        StepVerifier.create(approvalTaskDetails)
                .expectError(RuntimeException.class)
                .verify();
    }

    @ParameterizedTest
    @MethodSource("inputsWithHttpErrors")
    public void whenWorkdayApiHttpStatusIsErrorThrowsException(HttpStatus status) {
        mockWorkdayApiErrorResponse(status);
        final Mono<BusinessProcessTask> approvalTaskDetails =
                businessProcessService.getApprovalTaskDetails(BASE_URL, WORKDAY_TOKEN, INBOX_TASK, LOCALE);
        StepVerifier.create(approvalTaskDetails)
                .expectError(BusinessProcessException.class)
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
    public void whenGetBusinessProcessTaskProvidedWithInvalidInputs(final String baseUrl, final String workdayAuth,
                                                             final InboxTask inboxTask, final Locale locale) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> businessProcessService.getApprovalTaskDetails(baseUrl, workdayAuth, inboxTask, locale));
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
