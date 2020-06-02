/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.controllers;

import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.domain.BotObjects;
import com.vmware.ws1connectors.servicenow.exception.CatalogReadException;
import com.vmware.ws1connectors.servicenow.forms.AddToCartForm;
import com.vmware.ws1connectors.servicenow.forms.ViewTaskForm;
import com.vmware.ws1connectors.servicenow.service.impl.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.AUTH_HEADER;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.BASE_URL_HEADER;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
@Slf4j
public class CartController extends BaseController {

    private static final String JSON_PATH_REQUEST_NUMBER = "$.result.request_number";
    @Autowired private CartService cartService;
    @Autowired private WebClient rest;
    @Autowired private SNowBotController sNowBotController;

    @PutMapping(
            path = ServiceNowConstants.CART_API_URL,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<BotObjects> addToCart(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ServiceNowConstants.ROUTING_PREFIX_TEMPLATE) String routingPrefixTemplate,
            Locale locale,
            @Valid AddToCartForm form) {

        String itemId = form.getItemId();
        // ToDo: Default itemCount to 1 ?
        //Please refer this to track issue https://jira-euc.eng.vmware.com/jira/browse/HW-109418
        Integer itemQuantity = form.getItemCount();

        LOGGER.trace("addToCart itemId={}, count={}, baseUrl={}", itemId, itemQuantity, baseUrl);

        String routingPrefix = routingPrefixTemplate.replace(ServiceNowConstants.INSERT_OBJECT_TYPE, ServiceNowConstants.OBJECT_TYPE_BOT_DISCOVERY);

        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return cartService.addToCart(itemId, itemQuantity, auth, baseUri, routingPrefix, locale);
    }

    @PostMapping(
            path = ServiceNowConstants.CHECKOUT_URL
    )
    public Mono<Map<String, List<Map<String, BotItem>>>> checkout(
            @RequestHeader(AUTHORIZATION) String mfToken,
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            Locale locale) {
        String userEmail = AuthUtil.extractUserEmail(mfToken);
        LOGGER.trace("checkout cart for user={}, baseUrl={}", userEmail, baseUrl);
        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        // If Bot needs cart subtotal, include that by making an extra call to SNow.
        return rest.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path(ServiceNowConstants.SNOW_CHECKOUT_ENDPOINT)
                        .build()
                )
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .map(doc -> doc.<String>read(JSON_PATH_REQUEST_NUMBER))
                .doOnSuccess(no -> LOGGER.debug("Ticket created {}", no))
                .doOnError(throwable -> {
                    throw new CatalogReadException(throwable.getMessage());
                })
                .map(ViewTaskForm::new)
                .flatMap(viewTaskForm -> sNowBotController.getTasks(mfToken, auth, baseUrl, viewTaskForm, locale));
    }

    @DeleteMapping(
            path = ServiceNowConstants.CART_API_URL,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<BotObjects>> emptyCart(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            Locale locale) {
        LOGGER.trace("emptyCart baseUrl={}", baseUrl);
        return cartService.emptyCart(auth, baseUrl, locale);
    }
}