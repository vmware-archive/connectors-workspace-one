/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.connectors.servicenow.domain.*;
import com.vmware.connectors.servicenow.domain.snow.*;
import com.vmware.connectors.servicenow.exception.CatalogReadException;
import com.vmware.connectors.servicenow.forms.AddToCartForm;
import com.vmware.connectors.servicenow.forms.CreateTaskForm;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.net.URI;
import java.util.*;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

//@RestController
public class SNowBotController {

    private static final Logger logger = LoggerFactory.getLogger(SNowBotController.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String AUTH_HEADER = "X-Connector-Authorization";
    private static final String BASE_URL_HEADER = "X-Connector-Base-Url";
    private static final String ROUTING_PREFIX = "x-routing-prefix";
    private static final String ROUTING_PREFIX_TEMPLATE = "X-Routing-Template";

    private static final String SNOW_ADD_TO_CART_ENDPOINT = "/api/sn_sc/servicecatalog/items/{item_id}/add_to_cart";

    private static final String SNOW_CHECKOUT_ENDPOINT = "/api/sn_sc/servicecatalog/cart/checkout";

    private static final String SNOW_DELETE_FROM_CART_ENDPOINT = "/api/sn_sc/servicecatalog/cart/{cart_item_id}";

    private static final String SNOW_DELETE_CART_ENDPOINT = "/api/sn_sc/servicecatalog/cart/{cart_id}/empty";

    private static final String SNOW_DELETE_TASK_ENDPOINT = "/api/now/table/task/{task_id}";

    private static final String SNOW_SYS_PARAM_LIMIT = "sysparm_limit";

    private static final String SNOW_SYS_PARAM_TEXT = "sysparm_text";

    private static final String SNOW_SYS_PARAM_CAT = "sysparm_category";

    private static final String SNOW_SYS_PARAM_QUAN = "sysparm_quantity";

    private static final String SNOW_SYS_PARAM_OFFSET = "sysparm_offset";

    private static final String NUMBER = "number";

    private static final String INSERT_OBJECT_TYPE = "INSERT_OBJECT_TYPE";

    private static final String ACTION_RESULT_KEY = "result";

    private final WebClient rest;

    @Autowired
    public SNowBotController(
            WebClient rest
    ) {
        this.rest = rest;
    }

    @PostMapping(
            path = "/api/v1/catalog-items",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<BotObjects> getCatalogItems(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ROUTING_PREFIX) String routingPrefix,
            @RequestParam(name = "limit", required = false, defaultValue = "10") String limit,
            @RequestParam(name = "offset", required = false, defaultValue = "0") String offset,
            @Valid @RequestBody CardRequest cardRequest
    ) {

        var catalogName = cardRequest.getTokenSingleValue("catalog");
        var categoryName = cardRequest.getTokenSingleValue("category");
        var type = cardRequest.getTokenSingleValue("type");
        String contextId = cardRequest.getTokenSingleValue("context_id");

        logger.trace("getItems for catalog: {}, category: {}, type: {}", catalogName, categoryName, type);
        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return getCatalogId(catalogName, auth, baseUri)
                .flatMap(catalogId -> getCategoryId(categoryName, catalogId, auth, baseUri))
                .flatMap(categoryId -> getItems(type, categoryId,
                        auth, baseUri,
                        limit, offset))
                .map(itemList -> toCatalogBotObj(itemList, routingPrefix, contextId));
    }

    private Mono<String> getCatalogId(String catalogTitle, String auth, URI baseUri) {
        return callForSysIdByResultTitle("/api/sn_sc/servicecatalog/catalogs", catalogTitle,
                auth, baseUri);
    }

    private Mono<String> getCategoryId(String categoryTitle, String catalogId, String auth, URI baseUri) {
        return callForSysIdByResultTitle(String.format("/api/sn_sc/servicecatalog/catalogs/%s/categories", catalogId), categoryTitle,
                auth, baseUri);
    }

    // ToDo: If it can filter, don't ask much from SNow. Go only with those needed for chatbot.
    private Mono<List<CatalogItem>> getItems(String type, String categoryId, String auth, URI baseUri,
                                             String limit, String offset) {
        logger.trace("getItems type:{}, categoryId:{}, baseUrl={}.", type, categoryId, baseUri);
        return rest.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path("/api/sn_sc/servicecatalog/items")
                        .queryParam(SNOW_SYS_PARAM_TEXT, type)
                        .queryParam(SNOW_SYS_PARAM_CAT, categoryId)
                        .queryParam(SNOW_SYS_PARAM_LIMIT, limit)
                        .queryParam(SNOW_SYS_PARAM_OFFSET, offset)
                        .build())
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(CatalogItemResults.class)
                .map(CatalogItemResults::getResult);
    }

    private BotObjects toCatalogBotObj(List<CatalogItem> itemList, String routingPrefix, String contextId) {
        BotObjects.Builder objectsBuilder = new BotObjects.Builder();

        itemList.forEach(catalogItem ->
                objectsBuilder.setObject(
                        new BotItem.Builder()
                                .setTitle(catalogItem.getName())
                                .setDescription(catalogItem.getShortDescription())
                                .addAction(getAddToCartAction(catalogItem.getId(), routingPrefix))
                                .setContextId(contextId)
                                .build())
        );

        return objectsBuilder.build();
    }

    //returns id of service catalog for matching title from the results.
    private Mono<String> callForSysIdByResultTitle(String endpoint, String title, String auth, URI baseUri) {
        logger.trace("read sys_id by title for endpoint:{}, title:{}", endpoint, title);
        return rest.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path(endpoint)
                        .build())
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .flatMap(doc -> {
                    List<String> sysIds = doc.read(String.format("$.result[?(@.title=~/.*%s/i)].sys_id", title));
                    if (sysIds != null && sysIds.size() == 1) {
                        return Mono.just(sysIds.get(0));
                    }

                    logger.debug("Couldn't find the sys_id for title:{}, endpoint:{}, baseUrl:{}",
                            title, endpoint, baseUri);
                    return Mono.error(new CatalogReadException("Can't find " + title));
                });
    }

    @DeleteMapping(
            path = "/api/v1/tasks/{taskId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Map<String, Object>>> deleteTask(
            @RequestHeader(AUTHORIZATION) String mfToken,
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @PathVariable String taskId) {

        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return rest.delete()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path(SNOW_DELETE_TASK_ENDPOINT)
                        .build(Map.of("task_id", taskId))
                )
                .header(AUTHORIZATION, auth)
                .exchange()
                .flatMap(this::toDeleteItemResponse);
    }


    @PostMapping(
            path = "/api/v1/tasks",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<BotObjects> getTasks(
            @RequestHeader(AUTHORIZATION) String mfToken,
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(ROUTING_PREFIX) String routingPrefix,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestBody CardRequest cardRequest,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset) {
        // Both task type and number are optional.
        // If a specific type is not specified, default to task.
        // If ticket number is not specified, deliver user created tasks/specific-types.

        // ToDo: Either validate w.r.t hardcoded types of tasks or handle error from SNow.
        String taskType;
        if (StringUtils.isBlank(cardRequest.getTokenSingleValue("type"))) {
            taskType = "task";
        } else {
            taskType = cardRequest.getTokenSingleValue("type");
        }
        String taskNumber = cardRequest.getTokenSingleValue(NUMBER);
        String contextId = cardRequest.getTokenSingleValue("context_id");

        var userEmail = AuthUtil.extractUserEmail(mfToken);

        logger.trace("getTasks for type={}, number={}, baseUrl={}, userEmail={}", taskType, taskNumber, baseUrl, userEmail);
        URI taskUri = buildTasksUriByReqParam(taskType, taskNumber, userEmail, baseUrl, limit, offset);

        return retrieveTasks(taskUri, auth)
                .map(taskList -> toTaskBotObj(taskList, routingPrefix, contextId));
    }

    private URI buildTasksUriByReqParam(String taskType, String taskNumber, String userEmail, String baseUrl, Integer limit, Integer offset) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder
                .fromUriString(baseUrl)
                .replacePath("/api/now/table/");

        if (StringUtils.isBlank(taskNumber)) {
            uriComponentsBuilder
                    .path(taskType)
                    .queryParam(SNOW_SYS_PARAM_LIMIT, limit)
                    .queryParam(SNOW_SYS_PARAM_OFFSET, offset)
                    .queryParam("opened_by.email", userEmail);
        } else {
            // If task number is provided, may be it doesn't matter to apply the filter about who created the ticket.
            uriComponentsBuilder
                    .path(taskType)
                    .queryParam(NUMBER, taskNumber);
        }

        return uriComponentsBuilder
                .encode().build().toUri();
    }

    private Mono<List<Task>> retrieveTasks(URI taskUri, String auth) {
        return rest.get()
                .uri(taskUri)
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(TaskResults.class)
                .map(TaskResults::getResult);
    }

    private BotObjects toTaskBotObj(List<Task> tasks, String routingPrefix, String contextId) {
        BotObjects.Builder objectsBuilder = new BotObjects.Builder();

        tasks.forEach(task ->
                objectsBuilder.setObject(new BotItem.Builder()
                        .setTitle("ServiceNow ticket: " + task.getNumber())
                        .setShortDescription(task.getShortDescription())
                        .addAction(getDeleteTaskAction(task.getSysId(), routingPrefix))
                        .setContextId(contextId)
                        .build())
        );

        return objectsBuilder.build();
    }

    @PostMapping(
            path = "/api/v1/cart",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<BotObjects> lookupCart(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestHeader(name = ROUTING_PREFIX) String routingPrefix,
            @RequestBody CardRequest cardRequest) {

        String contextId = cardRequest.getTokenSingleValue("context_id");

        return retrieveUserCart(baseUrl, auth)
                .map(cartDocument -> toCartBotObj(cartDocument, routingPrefix, contextId));
    }

    private Mono<JsonDocument> retrieveUserCart(String baseUrl, String auth) {
        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();

        return rest.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path("/api/sn_sc/servicecatalog/cart")
                        .build()
                )
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(JsonDocument.class);
    }

    private BotObjects toCartBotObj(JsonDocument cartResponse, String routingPrefix, String contextId) {
        BotItem.Builder botObjectBuilder = new BotItem.Builder()
                .setTitle("ServiceNow Shopping Cart")
                .setContextId(contextId);

        List<LinkedHashMap> items = cartResponse.read("$.result.*.items.*");
        if (CollectionUtils.isEmpty(items)) {
            botObjectBuilder.setDescription("Cart is empty.");
        } else {
            botObjectBuilder.setDescription("Things in your shopping cart.")
                    .addAction(getEmptyCartAction(routingPrefix))
                    .addAction(getCheckoutCartAction(routingPrefix));

            List<CartItem> cartItems = objectMapper.convertValue(items, new TypeReference<List<CartItem>>(){});
            cartItems.forEach(
                    cartItem -> botObjectBuilder.addChild(getCartItemChildObject(cartItem, routingPrefix, contextId))
            );
        }

        return new BotObjects.Builder()
                .setObject(botObjectBuilder.build())
                .build();
    }

    private BotItem getCartItemChildObject(CartItem cartItem, String routingPrefix, String contextId) {
        return new BotItem.Builder()
                .setTitle(cartItem.getName())
                .setContextId(contextId)
                .setDescription(cartItem.getShortDescription())
                .addAction(getRemoveFromCartAction(cartItem.getEntryId(), routingPrefix))
                .build();
    }

    private BotAction getRemoveFromCartAction(String entryId, String routingPrefix) {
        return new BotAction.Builder()
                .setTitle("Remove from cart")
                .setDescription("Remove this item from the cart")
                .setType(HttpMethod.DELETE)
                .setUrl(new Link(routingPrefix + "api/v1/cart/" + entryId))
                .build();
    }

    private BotAction getCheckoutCartAction(String routingPrefix) {
        return new BotAction.Builder()
                .setTitle("Checkout")
                .setDescription("Checkout your cart.")
                .setType(HttpMethod.POST)
                .setUrl(new Link(routingPrefix + "api/v1/checkout"))
                .build();
    }

    private BotAction getEmptyCartAction(String routingPrefix) {
        return new BotAction.Builder()
                .setTitle("Empty this cart")
                .setDescription("Empty everything in the cart")
                .setType(HttpMethod.DELETE)
                .setUrl(new Link(routingPrefix + "api/v1/cart"))
                .build();
    }

    private BotAction getAddToCartAction(String itemId, String routingPrefix) {
        return new BotAction.Builder()
                .setTitle("Add to cart")
                .setDescription("Add this item to my shopping cart.")
                .setType(HttpMethod.PUT)
                .addReqParam("item_id", itemId)
                .addUserInputParam("item_count", "How many units should be added to cart ?")
                .setUrl(new Link(routingPrefix + "api/v1/cart"))
                .build();
    }

    private BotAction getDeleteTaskAction(String taskSysId, String routingPrefix) {
        return new BotAction.Builder()
                .setTitle("Delete ticket")
                .setDescription("Delete this ticket from ServiceNow.")
                .setType(HttpMethod.DELETE)
                .setUrl(new Link(routingPrefix + "api/v1/tasks/" + taskSysId))
                .build();
    }

    @PostMapping(
            path = "/api/v1/task/create",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<BotObjects> createTask(
            @RequestHeader(AUTHORIZATION) String mfToken,
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ROUTING_PREFIX_TEMPLATE) String routingPrefixTemplate,
            @Valid CreateTaskForm form) {

        // ToDo: Validate.
        // ToDo: Should we make it optional and default it to "task" ?
        String taskType = form.getType();

        String shortDescription = form.getShortDescription();

        var userEmail = AuthUtil.extractUserEmail(mfToken);

        logger.trace("createTicket for baseUrl={}, taskType={}, userEmail={}, routingTemplate={}", baseUrl, taskType, userEmail, routingPrefixTemplate);

        String routingPrefix = routingPrefixTemplate.replace(INSERT_OBJECT_TYPE, "taskItem");

        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return this.createTask(taskType, shortDescription, userEmail, baseUri, auth)
                .map(taskNumber -> new CardRequest(Map.of("number", Set.of((String) taskNumber)), null))
                .flatMap(taskObjReq -> getTasks(mfToken, auth, routingPrefix, baseUrl, taskObjReq, 1, 0));
    }

    private Mono<String> createTask(String taskType, String shortDescription, String callerEmailId,
                                    URI baseUri, String auth) {
        return rest.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path("/api/now/table/")
                        .path(taskType)
                        .build()
                )
                .header(AUTHORIZATION, auth)
                // ToDo: Improve this request body, if somehow chat-bot is able to supply more info.
                .syncBody(Map.of(
                        "short_description", shortDescription,
                        "caller_id", callerEmailId))
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .map(doc -> doc.read("$.result.number"));
    }

    @PutMapping(
            path = "/api/v1/cart",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<BotObjects> addToCart(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ROUTING_PREFIX_TEMPLATE) String routingPrefixTemplate,
            @Valid AddToCartForm form) {

        String itemId = form.getItemId();
        // ToDo: Default itemCount to 1 ?
        Integer itemCount = form.getItemCount();

        logger.trace("addToCart itemId={}, count={}, baseUrl={}", itemId, itemCount, baseUrl);

        String routingPrefix = routingPrefixTemplate.replace(INSERT_OBJECT_TYPE, "cart");

        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return rest.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path(SNOW_ADD_TO_CART_ENDPOINT)
                        .build(Map.of("item_id", itemId))
                )
                .header(AUTHORIZATION, auth)
                .syncBody(Map.of(SNOW_SYS_PARAM_QUAN, itemCount))
                .retrieve()
                .bodyToMono(Void.class)
                .then(this.lookupCart(auth, baseUrl, routingPrefix, new CardRequest(null, null)));
    }

    @DeleteMapping(
            path = "/api/v1/cart",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Map<String, Object>>> emptyCart(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl) {
        logger.trace("emptyCart baseUrl={}", baseUrl);

        return retrieveUserCart(baseUrl, auth)
                .map(cartDocument -> cartDocument.read("$.result.cart_id"))
                .flatMap(cartId -> deleteCart(baseUrl, auth, (String) cartId))
                .flatMap(this::toDeleteItemResponse);

    }

    private Mono<ClientResponse> deleteCart(String baseUrl, String auth, String cartId) {
        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();

        return rest.delete()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path(SNOW_DELETE_CART_ENDPOINT)
                        .build(Map.of("cart_id", cartId))
                )
                .header(AUTHORIZATION, auth)
                .exchange();
    }

    //ToDo: Add path variable to take cart-item-id.
    @DeleteMapping(
            path = "/api/v1/cart/{cartItemId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<BotObjects> deleteFromCart(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @PathVariable("cartItemId") String cartItemId,
            @RequestHeader(ROUTING_PREFIX_TEMPLATE) String routingPrefixTemplate) {
        logger.trace("deleteFromCart cartItem entryId={}, baseUrl={}", cartItemId, baseUrl);

        String routingPrefix = routingPrefixTemplate.replace(INSERT_OBJECT_TYPE, "cart");
        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return rest.delete()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path(SNOW_DELETE_FROM_CART_ENDPOINT)
                        .build(Map.of("cart_item_id", cartItemId))
                )
                .header(AUTHORIZATION, auth)
                .exchange()
                .then(this.lookupCart(auth, baseUrl, routingPrefix, new CardRequest(null, null)));
    }

    private Mono<ResponseEntity<Map<String, Object>>> toDeleteItemResponse(ClientResponse sNowResponse) {
        if (sNowResponse.statusCode().is2xxSuccessful()) {
            return Mono.just(
                    ResponseEntity
                            .status(sNowResponse.statusCode()).build());
        } else {
            return sNowResponse.bodyToMono(JsonDocument.class)
                    .map(body -> ResponseEntity.status(sNowResponse.statusCode())
                            .body(Map.of(ACTION_RESULT_KEY, Map.of(
                                    "message", body.read("$.error.message")))
                            )
                    );
        }
    }

    @PostMapping(
            path = "/api/v1/checkout"
    )
    public Mono<BotObjects> checkout(
            @RequestHeader(AUTHORIZATION) String mfToken,
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ROUTING_PREFIX_TEMPLATE) String routingPrefixTemplate) {

        var userEmail = AuthUtil.extractUserEmail(mfToken);

        logger.trace("checkout cart for user={}, baseUrl={}, routingTemplate={}", userEmail, baseUrl, routingPrefixTemplate);

        String routingPrefix = routingPrefixTemplate.replace(INSERT_OBJECT_TYPE, "taskItem");

        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        // If Bot needs cart subtotal, include that by making an extra call to SNow.
        return rest.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path(SNOW_CHECKOUT_ENDPOINT)
                        .build()
                )
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .map(doc -> doc.read("$.result.request_number"))
                .doOnSuccess(no -> logger.debug("Ticket created {}", no))
                .map(reqNumber -> new CardRequest(Map.of("number", Set.of((String) reqNumber)), null))
                .flatMap(taskObjReq -> getTasks(mfToken, auth, routingPrefix, baseUrl, taskObjReq, 1, 0));

    }

    @ExceptionHandler(CatalogReadException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public Map<String, String> connectorDisabledErrorHandler(CatalogReadException e) {
        return Map.of("message", e.getMessage());
    }
}
