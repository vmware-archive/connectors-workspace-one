package com.vmware.connectors.servicenow;

import com.google.common.collect.ImmutableList;
import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

import java.util.List;

@AutoProperty
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
        return Pojomatic.toString(this);
    }

}
