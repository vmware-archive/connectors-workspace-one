/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.catalog.category.api.response.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CategoriesItem {
    @JsonProperty("header_image")
    private String headerImage;
    @JsonProperty("sys_id")
    private String sysId;
    @JsonProperty("description")
    private String description;
    @JsonProperty("title")
    private String title;
}