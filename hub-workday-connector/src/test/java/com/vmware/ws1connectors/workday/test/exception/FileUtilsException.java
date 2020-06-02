/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.test.exception;

public class FileUtilsException extends RuntimeException {

    private static final long serialVersionUID = 3288662070402299500L;

    public FileUtilsException(String message, Exception cause) {
        super(message, cause);
    }
}
