/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

@Getter
@Setter
@Builder
public class RequestInfo {
    private String baseUrl;
    private String routingPrefix;
    private String connectorAuth;
    private String tenantName;
    private String tenantUrl;
    private boolean isPreHire;
    private Locale locale;
}
