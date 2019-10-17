/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.web;

public class InvalidUserActionException extends RuntimeException {

    public InvalidUserActionException(String message) {
        super(message);
    }
}
