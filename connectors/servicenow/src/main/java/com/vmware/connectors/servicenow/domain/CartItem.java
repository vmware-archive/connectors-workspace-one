/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CartItem {

    private String entryId;

    private String name;

    @JsonProperty("entry_id")
    public String getEntryId() {
        return entryId;
    }

    @JsonProperty("cart_item_id")
    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }
}
