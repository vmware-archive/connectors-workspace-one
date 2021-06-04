/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.exceptions;

import org.springframework.http.HttpStatus;

public class BusinessProcessException extends BusinessSystemException {

    public BusinessProcessException(String message, HttpStatus businessSystemStatus) {
        super(message, businessSystemStatus);
    }
}
