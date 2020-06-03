/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.exceptions;

import com.vmware.ws1connectors.workday.annotations.MessageKey;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
@MessageKey("time.off.task.not.found")
public class TimeOffTaskNotFoundException extends LocalizedException {

    private static final long serialVersionUID = 6182664806708375886L;

    public TimeOffTaskNotFoundException(Object... args) {
        super(args);
    }
}
