/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.exceptions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Optional;

@SuppressFBWarnings("OPM_OVERLY_PERMISSIVE_METHOD")
public class LocalizedException extends RuntimeException {
    private final Object[] args;

    public LocalizedException(Object... args) {
        super();
        this.args = cloneArgs(args);
    }

    public LocalizedException(Throwable throwable, Object... args) {
        super(throwable);
        this.args = cloneArgs(args);
    }

    public Object[] getArgs() {
        return cloneArgs(args);
    }

    private Object[] cloneArgs(Object... args) {
        return Optional.ofNullable(args)
            .map(Object[]::clone)
            .orElse(null);
    }
}
