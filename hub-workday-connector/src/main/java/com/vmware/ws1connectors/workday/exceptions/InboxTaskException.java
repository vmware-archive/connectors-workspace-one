/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.exceptions;

import org.springframework.http.HttpStatus;

public class InboxTaskException extends BusinessSystemException {
    static final String GET_INBOX_TASKS_ERROR_TEMPLATE = "Error occurred while retrieving inbox tasks";
    private static final long serialVersionUID = 3661470679423667328L;

    public InboxTaskException(HttpStatus businessSystemStatus) {
        super(GET_INBOX_TASKS_ERROR_TEMPLATE, businessSystemStatus);
    }

}
