/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.exceptions;

/**
 * Created by harshas on 05/16/18.
 */
public class AppConfigException extends RuntimeException {

    public AppConfigException(String message) {
        super(message);
    }
}
