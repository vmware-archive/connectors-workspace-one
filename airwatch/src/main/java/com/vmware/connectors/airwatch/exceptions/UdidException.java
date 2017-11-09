/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.exceptions;

/**
 * Created by harshas on 9/25/2017.
 */
public class UdidException extends RuntimeException {

    public UdidException(String msg) {
        super("AirWatch UDID exception, " + msg);
    }
}
