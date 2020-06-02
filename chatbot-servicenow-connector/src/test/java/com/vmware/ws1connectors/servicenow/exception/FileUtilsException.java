/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.exception;

public class FileUtilsException extends RuntimeException {

    private static final long serialVersionUID = 6254414813861679814L;

    public FileUtilsException(String message, Exception cause) {
        super(message, cause);
    }
}
