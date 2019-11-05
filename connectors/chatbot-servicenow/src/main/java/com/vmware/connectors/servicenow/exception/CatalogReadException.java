/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow.exception;

public class CatalogReadException extends RuntimeException {

    public CatalogReadException(String message) {
        super(message);
    }
}
