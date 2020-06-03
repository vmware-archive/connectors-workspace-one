/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings({"PMD.AvoidFieldNameMatchingTypeName", "squid:S1700"})
public class Descriptor {
    @JsonProperty("id")
    private String id;
    @JsonProperty("descriptor")
    private String descriptor;
    @JsonProperty("href")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String href;
}
