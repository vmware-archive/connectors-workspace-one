/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PendingApprovalResponse {

    private List<PendingApprovalsVO> items;

    @JsonProperty("Items")
    public List<PendingApprovalsVO> getPendingApprovals() {
        return items;
    }

    public void setPendingApprovals(List<PendingApprovalsVO> pendingApprovals) {
        this.items = pendingApprovals;
    }

}
