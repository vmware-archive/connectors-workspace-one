/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur.exception;

public class InvalidServiceAccountCredentialException extends RuntimeException {

    public InvalidServiceAccountCredentialException(String message) {
        super(message);
    }
}
