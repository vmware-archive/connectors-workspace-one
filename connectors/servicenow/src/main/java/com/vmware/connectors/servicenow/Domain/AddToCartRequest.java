/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow.Domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.util.Map;

/**
 * Payload for Get Items Requests
 *
 * @author Ravish Chawla
 */
public class AddToCartRequest {
    @NotNull(message = "Item Ids required")
    @Size(min = 1, message = "tokens should have at least one entry")
    private final Map<String, String> itemsAndQuantities;

    @JsonCreator
    public AddToCartRequest(@JsonProperty("items") Map<String, String> itemsAndQuantities) {
        this.itemsAndQuantities = itemsAndQuantities;
    }

    @JsonProperty("items")
    public Map<String, String> getItemsAndQuantities() {
        return this.itemsAndQuantities;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
