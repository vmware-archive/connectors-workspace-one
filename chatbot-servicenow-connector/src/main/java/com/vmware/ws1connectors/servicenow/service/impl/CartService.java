/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.service.impl;

import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.domain.BotObjects;
import com.vmware.ws1connectors.servicenow.domain.snow.CartItem;
import com.vmware.ws1connectors.servicenow.utils.BotActionBuilder;
import com.vmware.ws1connectors.servicenow.utils.BotObjectBuilderUtils;
import com.vmware.ws1connectors.servicenow.utils.JsonSeralizationUtils;
import com.vmware.ws1connectors.servicenow.utils.UriBuilderUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.CART_RESPONSE_JSON_PATH;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SNOW_ADD_TO_CART_ENDPOINT;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SNOW_CART_URL;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.TEXT;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_CONFIRMATION;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.UI_TYPE_TEXT;
import static com.vmware.ws1connectors.servicenow.utils.ArgumentUtils.checkArgumentNotBlank;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class CartService {

    private static final String AUTH = "auth";
    private static final String BASE_URL = "baseUrl";
    private static final String CART_ACTION_ITEM = "cart.action.item";
    private static final String CATALOG_ITEM_ID = "catalog_item_id";
    private static final String SHORT_DESCRIPTION = "short_description";
    private static final String CART_PROMPT_MSG = "cart.prompt.msg";

    private final WebClient rest;
    private final ConnectorTextAccessor connectorTextAccessor;
    private final BotActionBuilder botActionBuilder;

    @Autowired public CartService(WebClient rest, ConnectorTextAccessor connectorTextAccessor) {
        this.rest = rest;
        this.connectorTextAccessor = connectorTextAccessor;
        this.botActionBuilder = new BotActionBuilder(connectorTextAccessor);
    }

    public Mono<BotObjects> addToCart(String itemId, Integer itemQuantity, String auth, URI baseUri, String routingPrefix, Locale locale) {
        validateParametersForAddToCart(itemId, itemQuantity, auth, baseUri, routingPrefix);
        return rest.post()
                .uri(UriBuilderUtils.buildUri(baseUri, SNOW_ADD_TO_CART_ENDPOINT, Map.of(ServiceNowConstants.ITEM_ID_STR, itemId)))
                .header(AUTHORIZATION, auth)
                .bodyValue(Map.of(ServiceNowConstants.SNOW_SYS_PARAM_QUAN, itemQuantity))
                .retrieve()
                .bodyToMono(Void.class)
                .then(this.lookupCart(auth, baseUri.toString(), routingPrefix, locale, getCardRequest(), Optional.of(itemId)));
    }

    private CardRequest getCardRequest() {
        return new CardRequest(null, null);
    }

    private void validateParametersForAddToCart(String itemId, Integer itemQuantity, String auth, URI baseUri, String routingPrefix) {
        checkNotNull(itemId, "itemId can't be null");
        checkNotNull(itemQuantity, "itemQuantity can't be null");
        checkNotNull(auth, "auth can't be null");
        checkNotNull(baseUri, "baseUri can't be null");
        checkNotNull(routingPrefix, "routingPrefix can't be null");
    }

    public Mono<BotObjects> lookupCart(String auth, String baseUrl, String routingPrefix, Locale locale, CardRequest cardRequest, Optional<String> itemId) {
        validateParametersForLookupCart(auth, baseUrl, routingPrefix, cardRequest);
        String contextId = cardRequest.getTokenSingleValue(ServiceNowConstants.CONTEXT_ID);
        return retrieveUserCart(baseUrl, auth)
                .map(cartDocument -> toCartBotObj(baseUrl, cartDocument, routingPrefix, contextId, locale, itemId));
    }

    private void validateParametersForLookupCart(String auth, String baseUrl, String routingPrefix, CardRequest cardRequest) {
        checkNotNull(auth, "auth can't be null");
        checkNotNull(baseUrl, "baseUrl can't be null");
        checkNotNull(routingPrefix, "routingPrefix can't be null");
        checkNotNull(cardRequest, "cardRequest can't be null");
    }

    private Mono<JsonDocument> retrieveUserCart(String baseUrl, String auth) {
        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return rest.get()
                .uri(UriBuilderUtils.buildUri(baseUri, SNOW_CART_URL))
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(JsonDocument.class);
    }

    private BotObjects toCartBotObj(String baseUrl, JsonDocument cartResponse, String routingPrefix, String contextId, Locale locale, Optional<String> itemIdOptional) {
        List<Map> items = cartResponse.read(CART_RESPONSE_JSON_PATH);
        final BotObjects.Builder botObjects = new BotObjects.Builder();
        itemIdOptional.ifPresent(itemId -> {
            Optional<Map> cartItemOpt = items.stream().filter(item -> hasItemAddedToCart(item, itemId)).findFirst();
            cartItemOpt.filter(cartItem -> !cartItem.isEmpty())
                    .ifPresent(cartItem -> botObjects.addObject(buildAddToCartItemDetails(contextId, locale, StringUtils.removeEnd(cartItem.get(SHORT_DESCRIPTION).toString(), "\"")).build()));
        });
        botObjects.addObject(buildAddToCartPromptMsg(locale));
        List<CartItem> cartItems = JsonSeralizationUtils.getCartItems(items);
        cartItems.forEach(cartItem -> botObjects.addObject(getCartItemChildObject(baseUrl, cartItem, contextId)));
        BotItem botItemWithActions = buildBotItemForAddToCartActions(routingPrefix, locale);
        botObjects.addObject(botItemWithActions);
        return botObjects.build();
    }

    private boolean hasItemAddedToCart(Map item, String itemId) {
        return item.get(CATALOG_ITEM_ID).equals(itemId);
    }

    private BotItem buildAddToCartPromptMsg(Locale locale) {
        return new BotItem.Builder()
                .setTitle(connectorTextAccessor.getTitle(CART_PROMPT_MSG, locale))
                .setDescription(connectorTextAccessor.getDescription(CART_PROMPT_MSG, locale))
                .setType(TEXT)
                .setWorkflowStep(WorkflowStep.INCOMPLETE)
                .build();
    }

    private BotItem.Builder buildAddToCartItemDetails(String contextId, Locale locale, String shortDescription) {
        return new BotItem.Builder()
                .setTitle(connectorTextAccessor.getTitle(ServiceNowConstants.OBJECT_TYPE_CART, locale, shortDescription))
                .setContextId(contextId)
                .setType(UI_TYPE_CONFIRMATION)
                .setWorkflowStep(WorkflowStep.INCOMPLETE)
                .setDescription(connectorTextAccessor.getDescription(ServiceNowConstants.OBJECT_TYPE_CART, locale));
    }

    private BotItem buildBotItemForAddToCartActions(String routingPrefix, Locale locale) {
        return new BotItem.Builder()
                .setTitle(connectorTextAccessor.getTitle(CART_ACTION_ITEM, locale))
                .setDescription(connectorTextAccessor.getDescription(CART_ACTION_ITEM, locale))
                .setType(UI_TYPE_TEXT)
                .setWorkflowStep(WorkflowStep.INCOMPLETE)
                .addAction(botActionBuilder
                        .getEmptyCartBotAction(routingPrefix, locale, ServiceNowConstants.EMPTY_CART, HttpMethod.DELETE,
                                ServiceNowConstants.CART_API_URL))
                .addAction(botActionBuilder
                        .getEmptyCartBotAction(routingPrefix, locale, ServiceNowConstants.CHECKOUT, HttpMethod.POST,
                                ServiceNowConstants.CONFIRM_CHECKOUT_URL))
                .addAction(botActionBuilder.getAddAnotherItemCartAction(routingPrefix, locale))
                .build();
    }

    private BotItem getCartItemChildObject(String baseUrl, CartItem cartItem, String contextId) {
        return botActionBuilder.getChildObjectBotAction(baseUrl, cartItem, contextId);
    }

    public Mono<ResponseEntity<BotObjects>> emptyCart(
            String auth,
            String baseUrl,
            Locale locale) {
        LOGGER.trace("emptyCart baseUrl={}", baseUrl);
        checkArgumentNotBlank(auth, AUTH);
        checkArgumentNotBlank(baseUrl, BASE_URL);
        return retrieveUserCart(baseUrl, auth)
                .map(cartDocument -> cartDocument.read(ServiceNowConstants.CART_ID_JSON_PATH))
                .flatMap(cartId -> deleteCart(baseUrl, auth, String.valueOf(cartId)))
                .flatMap(clientResponse -> toDeleteItemResponse(clientResponse, locale));
    }

    private Mono<ClientResponse> deleteCart(String baseUrl, String auth, String cartId) {
        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return rest.delete()
                .uri(UriBuilderUtils.buildUri(baseUri, ServiceNowConstants.SNOW_DELETE_CART_ENDPOINT, Map.of(ServiceNowConstants.CART_ID, cartId))
                )
                .header(AUTHORIZATION, auth)
                .exchange();
    }

    private Mono<ResponseEntity<BotObjects>> toDeleteItemResponse(ClientResponse sNowResponse, Locale locale) {
        if (sNowResponse.statusCode().is2xxSuccessful()) {
            return Mono.just(
                    ResponseEntity
                            .ok().body(BotObjectBuilderUtils.botObjectBuilder(
                            connectorTextAccessor.getMessage(ServiceNowConstants.EMPTY_CART_SUCCESS_MSG, locale),
                            connectorTextAccessor.getMessage(ServiceNowConstants.EMPTY_CART_SUCCESS_DESC_MSG, locale),
                            WorkflowStep.COMPLETE, UI_TYPE_CONFIRMATION)));
        }
        return sNowResponse.bodyToMono(JsonDocument.class)
                .map(body -> ResponseEntity.status(sNowResponse.statusCode())
                        .body(BotObjectBuilderUtils.botObjectBuilder(
                                connectorTextAccessor.getMessage(ServiceNowConstants.EMPTY_CART_ERROR_MSG, locale),
                                body.read(ServiceNowConstants.ERROR_MSG), WorkflowStep.COMPLETE, UI_TYPE_CONFIRMATION)));
    }
}
