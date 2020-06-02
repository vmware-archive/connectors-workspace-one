/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.controllers;

import com.vmware.ws1connectors.servicenow.constants.ServiceNowCategory;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.service.impl.CategoryItemsService;
import com.vmware.ws1connectors.servicenow.service.impl.DeviceCategoryListService;
import com.vmware.ws1connectors.servicenow.utils.BotTextAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RestController
@Slf4j
public class SNowDeviceController {

    @Autowired DeviceCategoryListService deviceCategoryListService;
    @Autowired CategoryItemsService categoryItemsService;
    @Autowired BotTextAccessor botTextAccessor;

    @GetMapping(path = ServiceNowConstants.DEVICE_CATEGORY_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, List<Map<String, BotItem>>>> getDeviceCategory(
            @RequestHeader(ServiceNowConstants.AUTH_HEADER) String auth,
            @RequestHeader(ServiceNowConstants.ROUTING_PREFIX_TEMPLATE) String routingPrefixTemplate,
            @RequestHeader(ServiceNowConstants.BASE_URL_HEADER) String baseUrl,
            Locale locale) {
        String routingPrefix = routingPrefixTemplate.replace(ServiceNowConstants.INSERT_OBJECT_TYPE, ServiceNowConstants.OBJECT_TYPE_BOT_DISCOVERY);
        return deviceCategoryListService.getDeviceCategories(ServiceNowConstants.CATALOG_TITLE, baseUrl, auth, routingPrefix, locale).map(botItems -> {
            List<Map<String, BotItem>> deviceCategories = new ArrayList<>();
            botItems.forEach(botItem ->
                    deviceCategories.add(Map.of(ServiceNowConstants.ITEM_DETAILS, botItem))
            );
            return Map.of(ServiceNowConstants.OBJECTS, deviceCategories);
        });
    }

    @GetMapping(path = ServiceNowConstants.DEVICE_LIST_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, List<Map<String, BotItem>>>>> getCategoryItems(
            @RequestHeader(ServiceNowConstants.AUTH_HEADER) String auth,
            @RequestHeader(ServiceNowConstants.BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ServiceNowConstants.ROUTING_PREFIX_TEMPLATE) String routingPrefixTemplate,
            @RequestParam("device_category") String deviceCategory,
            @RequestParam(name = "limit", required = false, defaultValue = "10") String limit,
            @RequestParam(name = "offset", required = false, defaultValue = "0") String offset,
            Locale locale) {
        final ServiceNowCategory categoryEnum = ServiceNowCategory.fromString(deviceCategory);
        if (Objects.isNull(categoryEnum)) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(ServiceNowConstants.OBJECTS,
                    List.of(Map.of(ServiceNowConstants.ITEM_DETAILS, new BotItem.Builder().setTitle(botTextAccessor.getMessage("Invalid.device.category.msg", locale))
                            .setDescription(botTextAccessor.getMessage("Invalid.device.category.desc", locale))
                            .build())))));
        }
        String routingPrefix = routingPrefixTemplate.replace(ServiceNowConstants.INSERT_OBJECT_TYPE, ServiceNowConstants.OBJECT_TYPE_BOT_DISCOVERY);
        return categoryItemsService.getCategoryItems(categoryEnum, baseUrl, auth, limit, offset, routingPrefix, locale).map(botItems -> {
            List<Map<String, BotItem>> categoryItems = new ArrayList<>();
            botItems.forEach(botItem ->
                    categoryItems.add(Map.of(ServiceNowConstants.ITEM_DETAILS, botItem))
            );
            return ResponseEntity.ok().body(Map.of(ServiceNowConstants.OBJECTS, categoryItems));
        });
    }
}
