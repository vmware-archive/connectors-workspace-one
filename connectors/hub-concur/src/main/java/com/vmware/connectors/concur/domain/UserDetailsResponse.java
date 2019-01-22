/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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
