/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.test.exception;

public class SerializerException extends RuntimeException {

    private static final long serialVersionUID = 5142427452035888362L;

    public SerializerException(final Throwable throwable) {
        super(throwable);
    }
}
