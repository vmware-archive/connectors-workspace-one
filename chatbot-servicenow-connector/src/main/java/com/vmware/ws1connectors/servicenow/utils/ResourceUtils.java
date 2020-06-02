/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;

public final class ResourceUtils {

    private ResourceUtils() { }

    public static Link getAddToCartActionUrl(String routingPrefix) {
        return new Link(routingPrefix + ServiceNowConstants.SERVICE_NOW_CONNECTOR_CONTEXT_PATH + ServiceNowConstants.URL_PATH_SEPERATOR + ServiceNowConstants.CART_API_URL);
    }
}
