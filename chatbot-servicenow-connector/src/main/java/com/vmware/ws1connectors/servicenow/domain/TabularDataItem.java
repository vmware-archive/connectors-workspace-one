/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.vmware.connectors.common.payloads.response.Link;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter @Builder @EqualsAndHashCode
public class TabularDataItem {
    @EqualsAndHashCode.Include
    private String id;
    @EqualsAndHashCode.Include
    private String title;
    @EqualsAndHashCode.Include
    private String subtitle;
    @EqualsAndHashCode.Include
    private String shortDescription;
    @EqualsAndHashCode.Include
    private String description;
    @EqualsAndHashCode.Exclude
    private Link image;
    @EqualsAndHashCode.Exclude
    private Link url;
}
