/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.domain.snow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogItem {

    private String name;

    private String id;

    private String shortDescription;

    private String description;

    private String picture;

    private String localizedPrice;

    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    @JsonProperty("sys_id")
    public void setId(String id) {
        this.id = id;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    @JsonProperty("short_description")
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    public String getPicture() {
        return picture;
    }

    @JsonProperty("picture")
    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getLocalizedPrice() {
        return localizedPrice;
    }

    @JsonProperty("localized_price")
    public void setLocalizedPrice(String localizedPrice) {
        this.localizedPrice = localizedPrice;
    }
}
