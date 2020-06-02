/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.service.impl;

import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.ws1connectors.servicenow.catalog.category.api.response.vo.CatalogCategoryResponse;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowCategory;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.exception.CatalogReadException;
import com.vmware.ws1connectors.servicenow.utils.BotActionBuilder;
import com.vmware.ws1connectors.servicenow.utils.BotTextAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_BUTTON;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@Slf4j
public class DeviceCategoryListServiceImpl implements DeviceCategoryListService {

    private final WebClient restClient;
    private final BotActionBuilder botActionBuilder;

    @Autowired public DeviceCategoryListServiceImpl(WebClient rest, BotTextAccessor botTextAccessor, ServerProperties serverProperties) {
        this.restClient = rest;
        this.botActionBuilder = new BotActionBuilder(botTextAccessor, serverProperties);
    }

    @Override public Mono<List<BotItem>> getDeviceCategories(String catalogTitle, String baseUrl, String auth, String routingPrefix, Locale locale) {
        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return getCatalogId(catalogTitle, auth, baseUri)
                .flatMap(catalogId -> getCategoryList(catalogId, auth, baseUri))
                .map(responseVo ->
                        responseVo.getResult().getCategories().stream()
                                .filter(categoriesItem -> ServiceNowCategory.contains(categoriesItem.getTitle()))
                                .map(categoriesItem -> new BotItem.Builder().setTitle(categoriesItem.getTitle())
                                        .setDescription(categoriesItem.getDescription())
                                        .setWorkflowStep(WorkflowStep.INCOMPLETE)
                                        .addAction(botActionBuilder.buildBotActionForDeviceCategory(ServiceNowCategory.fromString(categoriesItem.getTitle()), routingPrefix, locale))
                                        .setType(UI_TYPE_BUTTON)
                                        .build())
                                .collect(Collectors.toList())
                );
    }

    Mono<String> getCatalogId(String catalogTitle, String auth, URI baseUri) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path(ServiceNowConstants.SNOW_CATALOG_ENDPOINT)
                        .build())
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .flatMap(doc -> {
                    List<String> sysIds = doc.read(String.format("$.result[?(@.title=~/.*%s/i)].sys_id", catalogTitle));
                    if (sysIds != null && sysIds.size() == 1) {
                        return Mono.just(sysIds.get(0));
                    }

                    LOGGER.debug("Couldn't find the sys_id for title:{}, endpoint:{}, baseUrl:{}",
                            catalogTitle, ServiceNowConstants.SNOW_CATALOG_ENDPOINT, baseUri);
                    return Mono.error(new CatalogReadException("Can't find " + catalogTitle));
                });
    }

    Mono<CatalogCategoryResponse> getCategoryList(String catalogId, String auth, URI baseUri) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path(ServiceNowConstants.SNOW_CATALOG_CATEGORY_ENDPOINT)
                        .build(Map.of(ServiceNowConstants.CATALOG_ID, catalogId))
                )
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(CatalogCategoryResponse.class);
    }
}
