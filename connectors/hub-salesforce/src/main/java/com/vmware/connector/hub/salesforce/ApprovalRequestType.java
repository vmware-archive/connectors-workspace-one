package com.vmware.connector.hub.salesforce;

public enum ApprovalRequestType {

    APPROVE("Approve"),

    REJECT("Reject");

    private final String type;

    ApprovalRequestType(final String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}
