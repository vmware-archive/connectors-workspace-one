/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ArgumentUtils {

    public static final String WORKDAY_BASE_URL = "Workday Base URL";
    public static final String WORKDAY_ACCESS_TOKEN = "Workday Access Token";
    public static final String TENANT_NAME = "Workday Tenant Name";

    public static void checkArgumentNotBlank(String value, String name) {
        checkArgument(isNotBlank(value), new StringBuilder("Cannot be blank: ").append(name).toString());
    }

    public static void checkArgumentNotNull(Object value, String name) {
        checkArgument(nonNull(value), new StringBuilder("Cannot be null: ").append(name).toString());
    }

    public static void checkBasicConnectorArgumentsNotBlank(final String baseUrl, final String workdayAccessToken, final String tenantName) {
        checkArgumentNotBlank(baseUrl, WORKDAY_BASE_URL);
        checkArgumentNotBlank(workdayAccessToken, WORKDAY_ACCESS_TOKEN);
        checkArgumentNotBlank(tenantName, TENANT_NAME);
    }

}
