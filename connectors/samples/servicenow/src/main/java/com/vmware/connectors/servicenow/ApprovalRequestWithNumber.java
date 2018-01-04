/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

@AutoProperty
class ApprovalRequestWithNumber extends ApprovalRequest {

    private final String number;

    /**
     * @param request The {@link ApprovalRequest} information from the sysapproval_approver record.
     * @param number The request number from the sc_request record.
     */
    ApprovalRequestWithNumber(
            ApprovalRequest request,
            String number
    ) {
        super(
                request.getRequestSysId(),
                request.getApprovalSysId(),
                request.getComments(),
                request.getDueDate(),
                request.getCreatedBy()
        );
        this.number = number;
    }

    /**
     * @return The request number from the sc_request record.
     */
    String getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return Pojomatic.toString(this);
    }

}
