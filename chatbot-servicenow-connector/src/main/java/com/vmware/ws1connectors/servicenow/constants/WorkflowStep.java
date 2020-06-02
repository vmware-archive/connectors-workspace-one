/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.constants;

import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkflowStep {
    INCOMPLETE("Incomplete"), COMPLETE("Complete");
    private String status;

    WorkflowStep(String status) {
        this.status = status;
    }

    @JsonValue public String getStatus() {
        return status;
    }

}
