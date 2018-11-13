/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connector.hub.salesforce;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ApprovalRequests {

    @JsonProperty("requests")
    private List<ApprovalRequest> requests;

    public List<ApprovalRequest> getRequests() {
        return requests;
    }

    public void setRequests(final List<ApprovalRequest> requests) {
        this.requests = requests;
    }
}
