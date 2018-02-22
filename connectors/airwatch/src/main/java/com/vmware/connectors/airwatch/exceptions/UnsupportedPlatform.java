/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.exceptions;

public class UnsupportedPlatform extends RuntimeException {

    public UnsupportedPlatform(String message) {
        super(message);
    }
}
