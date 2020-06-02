/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FAILED_DEPENDENCY)
@Getter
public class BusinessSystemException extends RuntimeException {
    private static final String BUSINESS_SYSTEM_STATUS = "Business System Status";
    private final HttpStatus businessSystemStatus;

    public BusinessSystemException(String message, HttpStatus businessSystemStatus) {
        super(message);
        this.businessSystemStatus = businessSystemStatus;
    }
}
