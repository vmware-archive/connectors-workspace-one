/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow.forms;

import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@SuppressWarnings("PMD.MethodNamingConventions")
public class AddToCartForm {

    @NotBlank
    private String itemId;

    @NotNull
    @Range(min = 1)
    private Integer itemCount;

    public String getItemId() {
        return itemId;
    }

    public void setItem_id(String itemId) {
        this.itemId = itemId;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItem_count(Integer itemCount) {
        this.itemCount = itemCount;
    }
}
