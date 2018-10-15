package com.vmware.connectors.concur.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomDetailsVO {

    private String type;
    private String value;
    private String code;

    @JsonProperty("Type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("Value")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @JsonProperty("Code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
