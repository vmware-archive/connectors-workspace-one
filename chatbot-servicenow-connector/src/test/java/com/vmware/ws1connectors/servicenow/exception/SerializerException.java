/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.exception;

public class SerializerException extends RuntimeException {

    private static final long serialVersionUID = 5523149995270758411L;

    public SerializerException(final Throwable throwable) {
        super(throwable);
    }
}
