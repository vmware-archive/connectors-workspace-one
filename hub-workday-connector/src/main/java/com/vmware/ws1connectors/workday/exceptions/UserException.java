/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.exceptions;

import org.springframework.http.HttpStatus;

public class UserException extends BusinessSystemException {
    public static final String GET_USER_ERROR = "Error occurred in finding user details.";

    public UserException(HttpStatus businessSystemStatus) {
        super(GET_USER_ERROR, businessSystemStatus);
    }
}
