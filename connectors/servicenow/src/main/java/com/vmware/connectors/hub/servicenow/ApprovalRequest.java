/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.hub.servicenow;

import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

class ApprovalRequest {

    private final String requestSysId;
    private final String approvalSysId;
    private final String comments;
    private final String dueDate;
    private final String createdBy;

    /**
     * Construct an ApprovalRequest.
     *
     * @param requestSysId - The sys_id of the sysapproval_approval record.
     * @param approvalSysId The sys_id of the sc_request record and the
     *                      sysapproval.value of the sysapproval_approval
     *                      record.
     * @param comments The comments of the sysapproval_approval record. TODO: not working, maybe take it out
     * @param dueDate The due_date of the sysapproval_approval record.
     * @param createdBy The sys_created_by of the sysapproval_approval
     *                  record or user_name of the user who created the
     *                  sc_request record.
     */
    ApprovalRequest(
            String requestSysId,
            String approvalSysId,
            String comments,
            String dueDate,
            String createdBy
    ) {
        this.requestSysId = requestSysId;
        this.approvalSysId = approvalSysId;
        this.comments = comments;
        this.dueDate = dueDate;
        this.createdBy = createdBy;
    }

    /**
     * @return The sys_id of the sysapproval_approval record.
     */
    String getRequestSysId() {
        return requestSysId;
    }

    /**
     * @return The sys_id of the sc_request record and the sysapproval.value of
     * the sysapproval_approval record.
     */
    String getApprovalSysId() {
        return approvalSysId;
    }

    /**
     * @return The comments of the sysapproval_approval record.
     */
    String getComments() {
        return comments;
    }

    /**
     * @return The due_date of the sysapproval_approval record.
     */
    String getDueDate() {
        return dueDate;
    }

    /**
     * @return The sys_created_by of the sysapproval_approval record or
     * user_name of the user who created the sc_request record.
     */
    String getCreatedBy() {
        return createdBy;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
