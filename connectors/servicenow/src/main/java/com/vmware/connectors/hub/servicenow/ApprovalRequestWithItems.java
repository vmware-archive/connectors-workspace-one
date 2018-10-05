package com.vmware.connectors.hub.servicenow;

import com.google.common.collect.ImmutableList;
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
        this.items = ImmutableList.copyOf(items);
    }

    List<RequestedItem> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
