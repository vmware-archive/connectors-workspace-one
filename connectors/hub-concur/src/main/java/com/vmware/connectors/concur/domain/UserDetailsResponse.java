package com.vmware.connectors.concur.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDetailsResponse {

    private List<UserDetailsVO> items;

    @JsonProperty("Items")
    public List<UserDetailsVO> getItems() {
        return items;
    }

    public void setItems(List<UserDetailsVO> items) {
        this.items = items;
    }

}
