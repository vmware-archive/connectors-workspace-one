/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Created by Rob Worsnop on 4/11/17.
 */
@ControllerAdvice
public class ExceptionHandlers {
    private final static Logger logger = LoggerFactory.getLogger(ExceptionHandlers.class);

    private final static String BACKEND_STATUS = "X-Backend-Status";


    //Handles  validation exceptions
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public Map<String, Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errorMap = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
        return Collections.singletonMap("errors", errorMap);
    }

    @ExceptionHandler
    @ResponseBody
    public ResponseEntity<Object> handleStatusCodeException(HttpStatusCodeException e) {
        // Map the status to a 500 unlesss it's a 401. If that's the case
        // then we know the client has passed in an invalid X-xxx-Authorization header and it's a 400.
        logger.error("Backend returned {} {} \n {}", e.getStatusCode(), e.getStatusCode().getReasonPhrase(), e.getResponseBodyAsString());
        String backendStatus = Integer.toString(e.getRawStatusCode());
        if (e.getStatusCode() == UNAUTHORIZED) {
            Map<String, String> body = Collections.singletonMap("error", "invalid_connector_token");
            return ResponseEntity.status(BAD_REQUEST)
                    .header(BACKEND_STATUS, backendStatus)
                    .contentType(APPLICATION_JSON)
                    .body(body);
        } else {
            BodyBuilder builder = ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .header(BACKEND_STATUS, backendStatus);
            if (e.getResponseHeaders().getContentType() != null) {
                builder.contentType(e.getResponseHeaders().getContentType());
            }
            return builder.body(e.getResponseBodyAsString());
        }
    }
}
