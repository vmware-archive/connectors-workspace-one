/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.service.impl;

import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.ws1connectors.servicenow.catalog.category.api.response.vo.CategoryItem;
import com.vmware.ws1connectors.servicenow.catalog.category.api.response.vo.CategoryItemsResponse;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowCategory;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.domain.BotAction;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.domain.TabularData;
import com.vmware.ws1connectors.servicenow.domain.TabularDataItem;
import com.vmware.ws1connectors.servicenow.utils.ResourceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.PRICE;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_ITEM;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@Slf4j
public class CategoryItemsServiceImpl implements CategoryItemsService {

    private static final String EMPTY_DEVICE_LIST = "empty.device.list";
    private final DeviceCategoryListServiceImpl deviceCategoryListService;
    private final WebClient webClient;
    private final ConnectorTextAccessor connectorTextAccessor;

    @Autowired public CategoryItemsServiceImpl(DeviceCategoryListServiceImpl deviceCategoryListServiceImpl, WebClient webClient, ConnectorTextAccessor connectorTextAccessor) {
        this.deviceCategoryListService = deviceCategoryListServiceImpl;
        this.webClient = webClient;
        this.connectorTextAccessor = connectorTextAccessor;
    }

    @Override public Mono<List<BotItem>> getCategoryItems(ServiceNowCategory categoryEnum, String baseUrl, String auth,
                                                          String limit, String offset, String routingPrefix,
                                                          Locale locale) {
        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return deviceCategoryListService.getCatalogId(ServiceNowConstants.CATALOG_TITLE, auth, baseUri)
                .flatMap(catalogId -> deviceCategoryListService.getCategoryList(catalogId, auth, baseUri))
                .flatMap(catalogCategoryResponse -> Flux
                        .fromIterable(catalogCategoryResponse.getResult().getCategories())
                        .filter(categoriesItem -> categoriesItem.getTitle()
                                .equalsIgnoreCase(categoryEnum.getCategoryName()))
                        .flatMap(categoriesItem -> getItems(categoriesItem.getSysId(), auth, baseUri, limit,
                                offset)).next())
                .flatMap(categoryItemsResponse -> Flux.fromIterable(categoryItemsResponse.getResult())
                        .map(categoryItem -> getBotItem(routingPrefix, locale, categoryItem, baseUrl))
                        .collectList()
                        .flatMap(botItems -> {
                            if (botItems.isEmpty()) {
                                return getBotItemsForDeviceCategories(baseUrl, auth, routingPrefix, locale);
                            }
                            return Mono.just(botItems);
                        }));
    }

    private Mono<List<BotItem>> getBotItemsForDeviceCategories(String baseUrl, String auth, String routingPrefix, Locale locale) {
        return deviceCategoryListService
                .getDeviceCategories(ServiceNowConstants.CATALOG_TITLE, baseUrl, auth, routingPrefix, locale)
                .flatMap(botItemsWithCategories -> {
                    BotItem botItemWithEmptyListMsg = getBotItemIfEmpty(locale);
                    List<BotItem> botItemsIfEmptyList = new ArrayList<>();
                    botItemsIfEmptyList.add(botItemWithEmptyListMsg);
                    botItemsIfEmptyList.addAll(botItemsWithCategories);
                    return Mono.just(botItemsIfEmptyList);
                });
    }

    private BotItem getBotItemIfEmpty(Locale locale) {
        return new BotItem.Builder()
                .setTitle(connectorTextAccessor.getTitle(EMPTY_DEVICE_LIST, locale))
                .setDescription(connectorTextAccessor.getDescription(EMPTY_DEVICE_LIST, locale))
                .setType(ServiceNowConstants.TEXT)
                .setWorkflowStep(WorkflowStep.INCOMPLETE)
                .build();
    }

    private BotItem getBotItem(String routingPrefix, Locale locale,
                               CategoryItem categoryItem, String baseUrl) {
        return new BotItem.Builder()
                .setTitle(categoryItem.getShortDescription())
                .setDescription(categoryItem.getDescription())
                .addAction(getAddToCartAction(categoryItem.getSysId(), routingPrefix, locale))
                .setWorkflowStep(WorkflowStep.INCOMPLETE)
                .addTabularData(addTabularDataForPrice(categoryItem))
                .setType(UI_TYPE_ITEM)
                .setImage(new Link(UriComponentsBuilder.fromHttpUrl(baseUrl).path(categoryItem.getPicture()).toUriString()))
                .build();
    }

    private TabularData addTabularDataForPrice(CategoryItem categoryItem) {
        return TabularData.builder().tabularDataItems(List.of(TabularDataItem.builder()
                .title(PRICE)
                .shortDescription(categoryItem.getPrice())
                .build()))
                .build();
    }

    private BotAction getAddToCartAction(String itemId, String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(connectorTextAccessor.getTitle(ServiceNowConstants.ADD_TO_CART, locale))
                .setDescription(connectorTextAccessor.getDescription(ServiceNowConstants.ADD_TO_CART, locale))
                .setType(HttpMethod.PUT)
                .setRequestParam(ServiceNowConstants.ITEM_ID, itemId)
                .setRequestHeaders(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .setUrl(ResourceUtils.getAddToCartActionUrl(routingPrefix))
                .build();
    }

    private Mono<CategoryItemsResponse> getItems(String categoryId, String auth, URI baseUri,
                                                 String limit, String offset) {
        LOGGER.trace("getItems categoryId:{}, baseUrl={}.", categoryId, baseUri);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path(ServiceNowConstants.SNOW_CATEGORY_ITEM_URL)
                        .queryParam(ServiceNowConstants.SNOW_SYS_PARAM_CAT, categoryId)
                        .queryParam(ServiceNowConstants.SNOW_SYS_PARAM_LIMIT, limit)
                        .queryParam(ServiceNowConstants.SNOW_SYS_PARAM_OFFSET, offset)
                        .build())
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(CategoryItemsResponse.class);
    }
}
