/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.exceptions;

import com.google.common.collect.Maps;
import com.vmware.ws1connectors.workday.annotations.MessageKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.vmware.connectors.common.utils.CommonUtils.BACKEND_STATUS;
import static java.lang.String.valueOf;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RestControllerAdvice
@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ExceptionHandlers {
    static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred while processing request";
    static final String ERROR = "error";
    private static final Optional<HttpStatus> EMPTY_STATUS = Optional.empty();
    public static final String INVALID_CONNECTOR_TOKEN = "invalid_connector_token";
    private static final String SERVER_UNMAPPED_ERROR_MSG_KEY = "server.unmapped.error";
    static final String MSG_KEY_NOT_FOUND = "Message Key Not Found";
    private static final Locale DEFAULT_LOCALE = Locale.US;

    @Autowired
    @Qualifier("errorMessageSource")
    private MessageSource errorMessageSource;

    @ExceptionHandler(LocalizedException.class)
    public ResponseEntity<Map<String, String>> handleLocalizedException(final ServerHttpRequest request, final LocalizedException ex) {
        final String localizedErrorMsg = getLocalizedMessage(request, ex);
        final HttpStatus responseStatus = Optional.ofNullable(findAnnotation(ex.getClass(), ResponseStatus.class))
            .map(ResponseStatus::code)
            .orElse(HttpStatus.INTERNAL_SERVER_ERROR);

        return handleAllExceptions(request, responseStatus, EMPTY_STATUS, localizedErrorMsg, ex);
    }

    private String getLocalizedMessage(ServerHttpRequest request, LocalizedException ex) {
        final String errorCode = Optional.ofNullable(findAnnotation(ex.getClass(), MessageKey.class))
            .map(MessageKey::value)
            .orElse(SERVER_UNMAPPED_ERROR_MSG_KEY);
        final Locale locale = request.getHeaders().getAcceptLanguageAsLocales()
            .stream()
            .findFirst()
            .orElse(DEFAULT_LOCALE);
        return getLocalizedMessage(ex.getArgs(), errorCode, locale);
    }

    private String getLocalizedMessage(final Object[] args, final String errorCode, final Locale locale) {
        try {
            return errorMessageSource.getMessage(errorCode, args, locale);
        } catch (NoSuchMessageException e) {
            LOGGER.warn("No such key {} in locale: {}", errorCode, locale);
            if (!locale.equals(DEFAULT_LOCALE)) {
                return getLocalizedMessage(args, errorCode, DEFAULT_LOCALE);
            }
        }
        return MSG_KEY_NOT_FOUND;
    }

    @ExceptionHandler(BusinessSystemException.class)
    public ResponseEntity<Map<String, String>> handleBusinessSystemException(final ServerHttpRequest request, final BusinessSystemException ex) {
        final Optional<HttpStatus> businessSystemResponseCodeOpt = Optional.ofNullable(ex.getBusinessSystemStatus());
        return Optional.ofNullable(findAnnotation(ex.getClass(), ResponseStatus.class))
            .map(responseStatus -> handleAllExceptions(request, responseStatus.code(), businessSystemResponseCodeOpt, ex.getMessage(), ex))
            .orElseGet(() -> handleAllExceptions(request, HttpStatus.FAILED_DEPENDENCY, businessSystemResponseCodeOpt, GENERIC_ERROR_MESSAGE, ex));
    }

    private ResponseEntity<Map<String, String>> handleAllExceptions(final ServerHttpRequest request,
                                                                    final HttpStatus responseCode,
                                                                    final Optional<HttpStatus> businessSystemResponseCodeOpt,
                                                                    final String errorMessage,
                                                                    final Exception ex) {
        logError(request, responseCode, ex);
        final Map<String, String> apiErrorResponse = Maps.newHashMap();
        final ResponseEntity.BodyBuilder errorResponseBuilder = businessSystemResponseCodeOpt.filter(businessSystemStatus -> businessSystemStatus == HttpStatus.UNAUTHORIZED)
            .map(errorResponseCode -> {
                apiErrorResponse.put(ERROR, INVALID_CONNECTOR_TOKEN);
                return jsonResponseWithStatus(HttpStatus.BAD_REQUEST);
            })
            .orElseGet(() -> {
                apiErrorResponse.put(ERROR, errorMessage);
                return jsonResponseWithStatus(responseCode);
            });
        businessSystemResponseCodeOpt.ifPresent(
            businessSystemResponseCode -> errorResponseBuilder.header(BACKEND_STATUS, valueOf(businessSystemResponseCode.value())));
        return errorResponseBuilder.body(apiErrorResponse);
    }

    private void logError(final ServerHttpRequest request, final HttpStatus responseCode, final Exception ex) {
        LOGGER.error("Request for URI {} returned with error response code {} and error is: ", valueOf(request.getURI()), responseCode.toString(), ex);
    }

    private ResponseEntity.BodyBuilder jsonResponseWithStatus(HttpStatus status) {
        return ResponseEntity.status(status).contentType(APPLICATION_JSON);
    }
}
