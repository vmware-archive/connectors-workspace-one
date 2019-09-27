/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow.domain.snow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogItemResults {

    private final List<CatalogItem> result;

    public CatalogItemResults(@JsonProperty("result") List<CatalogItem> result) {
        this.result = result;
    }

    @JsonProperty("result")
    public List<CatalogItem> getResult() {
        return result;
    }

}
