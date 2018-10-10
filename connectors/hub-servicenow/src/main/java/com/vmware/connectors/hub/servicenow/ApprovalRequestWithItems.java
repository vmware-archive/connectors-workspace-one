/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.hub.servicenow;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

class ApprovalRequestWithItems extends ApprovalRequestWithInfo {

    private final List<RequestedItem> items;

    ApprovalRequestWithItems(
            ApprovalRequestWithInfo request,
            List<RequestedItem> items
    ) {
        super(request, request.getInfo());
        this.items = List.copyOf(items);
    }

    List<RequestedItem> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
