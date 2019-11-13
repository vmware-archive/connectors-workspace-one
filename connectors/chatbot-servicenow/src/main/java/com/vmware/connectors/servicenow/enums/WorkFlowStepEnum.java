package com.vmware.connectors.servicenow.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkFlowStepEnum {
    COMPLETE("Complete"),
    INCOMPLETE("Incomplete");

    private final String step;

    WorkFlowStepEnum(final String step) {
        this.step = step;
    }

    @JsonValue
    public String getStep() {
        return this.step;
    }
}


