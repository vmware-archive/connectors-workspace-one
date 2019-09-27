/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.hub.servicenow;


import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

class ApprovalRequestWithInfo extends ApprovalRequest {

    private final Request info;

    /**
     * @param request The {@link ApprovalRequest} information from the sysapproval_approver record.
     * @param info The request infofrom the sc_request record.
     */
    ApprovalRequestWithInfo(
            ApprovalRequest request,
            Request info
    ) {
        super(
                request.getRequestSysId(),
                request.getApprovalSysId(),
                request.getComments(),
                request.getDueDate(),
                request.getCreatedBy()
        );
        this.info = info;
    }

    /**
     * @return The request info from the sc_request record.
     */
    Request getInfo() {
        return info;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
