/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.ws1connectors.servicenow.domain.snow.CartItem;

import java.util.List;
import java.util.Map;

public final class JsonSeralizationUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonSeralizationUtils() { }

    public static List<CartItem> getCartItems(List<Map> items) {
        return OBJECT_MAPPER.convertValue(items, new TypeReference<List<CartItem>>() { });
    }
}
