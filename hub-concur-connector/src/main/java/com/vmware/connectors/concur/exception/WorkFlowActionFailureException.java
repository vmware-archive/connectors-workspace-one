/*
 * Copyright © 2020 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur.exception;

import com.vmware.connectors.concur.ActionFailureResponse;

public class WorkFlowActionFailureException extends RuntimeException {
    private final ActionFailureResponse actionFailureResponse;

    public WorkFlowActionFailureException(String message, ActionFailureResponse actionFailureResponse) {
        super(message);

        this.actionFailureResponse = actionFailureResponse;
    }

    public ActionFailureResponse getActionFailureResponse() {
        return actionFailureResponse;
    }
}
