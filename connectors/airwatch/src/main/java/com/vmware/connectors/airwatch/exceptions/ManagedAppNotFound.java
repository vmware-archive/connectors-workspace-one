/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch.exceptions;

/**
 * Created by harshas on 01/31/18.
 */
public class ManagedAppNotFound extends RuntimeException {

    public ManagedAppNotFound(String message){
        super(message);
    }
}
