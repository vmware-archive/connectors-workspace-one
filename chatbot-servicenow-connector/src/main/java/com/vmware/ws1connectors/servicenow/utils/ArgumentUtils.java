/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArgumentUtils {

    private static final String VALIDATOR_MSG = "Cannot be Null or blank: ";

    public static void checkArgumentNotBlank(String value, String name) {
        checkArgument(!Strings.isNullOrEmpty(value), new StringBuilder(VALIDATOR_MSG).append(name).toString());
    }

    public static void checkArgumentNotNull(Object value, String name) {
        checkArgument(Objects.nonNull(value), new StringBuilder(VALIDATOR_MSG).append(name).toString());
    }
}
