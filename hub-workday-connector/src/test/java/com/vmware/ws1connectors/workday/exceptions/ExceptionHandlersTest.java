/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.exceptions;

import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static com.vmware.connectors.common.utils.CommonUtils.BACKEND_STATUS;
import static com.vmware.ws1connectors.workday.exceptions.ExceptionHandlers.ERROR;
import static com.vmware.ws1connectors.workday.exceptions.ExceptionHandlers.MSG_KEY_NOT_FOUND;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FAILED_DEPENDENCY;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class ExceptionHandlersTest {
    private static final String URI = "https://example.com/workday-connector/card/request";
    private static final String API_ERROR_MESSAGE = "Workday API failed";
    private static final String TASK_ID = "taskId";
    private static final String TIME_OFF_TASK_NOT_FOUND = "Time off task not found, id: " + TASK_ID;
    private static final String FILE_NOT_FOUND = "File not found";
    private static final String USER_NOT_FOUND_ERROR_MSG = "User can not be found";

    private final ExceptionHandlers exceptionHandlers = new ExceptionHandlers();
    private ServerHttpRequest mockServerHttpRequest;

    @BeforeEach public void setup() {
        mockServerHttpRequest = MockServerHttpRequest.get(URI)
            .build();
        ReloadableResourceBundleMessageSource errorMessages = new ReloadableResourceBundleMessageSource();
        errorMessages.setBasename("classpath:ErrorMessages");
        errorMessages.setDefaultEncoding(StandardCharsets.UTF_8.name());
        setField(exceptionHandlers, "errorMessageSource", errorMessages);
    }

    public static List<BusinessSystemException> getBusinessSystemExceptions() {
        return Arrays.asList(new UserException(INTERNAL_SERVER_ERROR),
            new BusinessSystemException(API_ERROR_MESSAGE, BAD_REQUEST));
    }

    @ParameterizedTest
    @MethodSource("getBusinessSystemExceptions")
    public void canHandleBusinessSystemException(final BusinessSystemException bse) {
        ResponseEntity<Map<String, String>> errorResponse = exceptionHandlers.handleBusinessSystemException(mockServerHttpRequest, bse);
        assertThat(errorResponse.getStatusCode()).isEqualTo(FAILED_DEPENDENCY);
        assertThat(errorResponse.getBody()).containsEntry(ERROR, bse.getMessage());
        assertThat(errorResponse.getHeaders())
            .containsEntry(BACKEND_STATUS, singletonList(String.valueOf(bse.getBusinessSystemStatus().value())));

    }

    public static Stream<Arguments> getLocalizedExceptionArguments() {
        return new ArgumentsStreamBuilder()
            .add(new UserNotFoundException(), BAD_REQUEST, USER_NOT_FOUND_ERROR_MSG)
            .add(new TimeOffTaskNotFoundException(TASK_ID), NOT_FOUND, TIME_OFF_TASK_NOT_FOUND)
            .build();
    }

    @ParameterizedTest
    @MethodSource("getLocalizedExceptionArguments")
    public void canHandleLocalizedException(final LocalizedException ex, final HttpStatus expectedStatus, final String expectedErrorMsg) {
        ResponseEntity<Map<String, String>> errorResponse = exceptionHandlers.handleLocalizedException(mockServerHttpRequest, ex);
        assertThat(errorResponse.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(errorResponse.getBody()).containsEntry(ERROR, expectedErrorMsg);
        assertThat(errorResponse.getHeaders().get(BACKEND_STATUS)).isNull();
    }

    @Test public void canGetErrorMessageInDefaultLocale() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAcceptLanguageAsLocales(Arrays.asList(Locale.FRANCE));
        mockServerHttpRequest = MockServerHttpRequest.get(URI)
            .headers(headers)
            .build();
        ResponseEntity<Map<String, String>> errorResponse = exceptionHandlers.handleLocalizedException(mockServerHttpRequest, new UserNotFoundException());
        assertThat(errorResponse.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(errorResponse.getBody()).containsEntry(ERROR, USER_NOT_FOUND_ERROR_MSG);
        assertThat(errorResponse.getHeaders().get(BACKEND_STATUS)).isNull();
    }

    @Test public void getsDefaultLocalizedErrorMessageWhenKeyIsNotFound() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAcceptLanguageAsLocales(Arrays.asList(Locale.FRANCE));
        mockServerHttpRequest = MockServerHttpRequest.get(URI)
            .headers(headers)
            .build();
        ResponseEntity<Map<String, String>> errorResponse = exceptionHandlers.handleLocalizedException(
            mockServerHttpRequest, new DiscoveryMetaDataReadFailedException(new IOException(FILE_NOT_FOUND)));
        assertThat(errorResponse.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(errorResponse.getBody()).containsEntry(ERROR, MSG_KEY_NOT_FOUND);
        assertThat(errorResponse.getHeaders().get(BACKEND_STATUS)).isNull();
    }
}
