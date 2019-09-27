/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.coupa.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Approval {

    @JsonProperty("id")
    private String id;

    @JsonProperty("position")
    private String position;

    @JsonProperty("status")
    private String status;

    @JsonProperty("approval-date")
    private String approvalDate;

    @JsonProperty("approvable-id")
    private String approvableId;

    @JsonProperty("approver")
    private UserDetails approver;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(String approvalDate) {
        this.approvalDate = approvalDate;
    }

    public UserDetails getApprover() {
        return approver;
    }

    public void setApprover(UserDetails approver) {
        this.approver = approver;
    }

    public String getApprovableId() {
        return approvableId;
    }

    public void setApprovableId(String approvableId) {
        this.approvableId = approvableId;
    }

}
