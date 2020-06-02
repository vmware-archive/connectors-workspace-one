/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.service.impl;

import com.vmware.ws1connectors.servicenow.domain.BotItem;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

public interface DeviceCategoryListService {
    Mono<List<BotItem>> getDeviceCategories(String catalogTitle, String baseUrl, String auth, String routingPrefix, Locale locale);
}
