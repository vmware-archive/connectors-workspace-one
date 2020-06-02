/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.domain.snow;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CartItem {

    private String entryId;

    private String name;

    private String shortDescription;

    private String picture;

    public String getEntryId() {
        return entryId;
    }

    @JsonProperty("cart_item_id")
    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    @JsonProperty("short_description")
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getPicture() {
        return picture;
    }

    @JsonProperty("picture")
    public void setPicture(String picture) {
        this.picture = picture;
    }
}
