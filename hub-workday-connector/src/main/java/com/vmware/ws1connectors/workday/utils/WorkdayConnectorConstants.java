/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkdayConnectorConstants {
    public static final String CONNECTOR_AUTH_HEADER = "X-Connector-Authorization";
    public static final String BASE_URL_HEADER = "X-Connector-Base-Url";
    public static final String ROUTING_PREFIX_HEADER = "X-Routing-Prefix";
    public static final String LOCALE = "Locale";
    public static final String CARDS_CONFIG = "Cards Config";
    public static final String ROUTING_PREFIX = "Routing Prefix";
    public static final String TENANT_NAME = "Tenant Name";
    public static final String TENANT_URL = "Tenant Url";
}
