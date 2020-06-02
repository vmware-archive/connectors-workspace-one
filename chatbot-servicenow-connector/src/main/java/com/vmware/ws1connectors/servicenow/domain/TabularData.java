/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter @Builder @EqualsAndHashCode
public class TabularData {

    @EqualsAndHashCode.Include
    private String title;
    @EqualsAndHashCode.Include
    private String description;
    @EqualsAndHashCode.Include
    private String type;
    @EqualsAndHashCode.Include
    @JsonProperty("data")
    private List<TabularDataItem> tabularDataItems;

}
