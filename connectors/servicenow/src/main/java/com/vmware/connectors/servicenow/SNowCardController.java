/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.*;
import com.vmware.connectors.servicenow.domain.BotAction;
import com.vmware.connectors.servicenow.domain.BotItem;
import com.vmware.connectors.servicenow.domain.BotObjects;
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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static com.vmware.connectors.common.utils.CommonUtils.APPROVAL_ACTIONS;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RestController
@SuppressWarnings("PMD.ExcessiveClassLength")
public class SNowCardController {
    // ToDo: Avoid pmd issue. (APF-2376)

    private static final Logger logger = LoggerFactory.getLogger(SNowCardController.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * The JsonPath prefix for the ServiceNow results.
     */
    private static final String RESULT_PREFIX = "$.result.";

    private static final String AUTH_HEADER = "X-Connector-Authorization";
    private static final String BASE_URL_HEADER = "X-Connector-Base-Url";
    private static final String ROUTING_PREFIX = "x-routing-prefix";
    private static final String ROUTING_PREFIX_TEMPLATE = "X-Routing-Template";

    private static final String SNOW_ADD_TO_CART_ENDPOINT = "/api/sn_sc/servicecatalog/items/{item_id}/add_to_cart";

    private static final String SNOW_CHECKOUT_ENDPOINT = "/api/sn_sc/servicecatalog/cart/checkout";

    private static final String SNOW_DELETE_FROM_CART_ENDPOINT = "/api/sn_sc/servicecatalog/cart/{cart_item_id}";

    private static final String SNOW_DELETE_CART_ENDPOINT = "/api/sn_sc/servicecatalog/cart/{cart_id}/empty";

    private static final String SNOW_DELETE_TASK_ENDPOINT = "/api/now/table/task/{task_id}";

    private static final String REASON_PARAM_KEY = "reason";

    /**
     * The query param to specify which fields you want to come back in your
     * ServiceNow REST calls.
     */
    private static final String SNOW_SYS_PARAM_FIELDS = "sysparm_fields";

    /**
     * The query param to specify a limit of the results coming back in your
     * ServiceNow REST calls.
     * <p>
     * The default is 10,000.
     */
    private static final String SNOW_SYS_PARAM_LIMIT = "sysparm_limit";

    private static final String SNOW_SYS_PARAM_TEXT = "sysparm_text";

    private static final String SNOW_SYS_PARAM_CAT = "sysparm_category";

    private static final String SNOW_SYS_PARAM_QUAN = "sysparm_quantity";

    private static final String SNOW_SYS_PARAM_OFFSET = "sysparm_offset";

    /**
     * The maximum approval requests to fetch from ServiceNow.  Since we have
     * to filter results out based on the ticket_id param passed in by the
     * client, this has to be sufficiently large to not lose results.
     * <p>
     * I wasn't able to find a REST call that would allow me to bulk lookup the
     * approval requests (or requests) by multiple request numbers
     * (ex. REQ0010001,REQ0010002,REQ0010003), so I'm forced to do things a
     * little less ideal than I would like (calling 1x per result of the
     * sysapproval_approver call to be able to match it to the request numbers
     * passed in by the client).
     */
    private static final int MAX_APPROVAL_RESULTS = 10_000;

    private static final String NUMBER = "number";

    private static final String INSERT_OBJECT_TYPE = "INSERT_OBJECT_TYPE";

    private static final String ACTION_RESULT_KEY = "result";

    private static final String OBJECT_TYPE_TASK = "task";

    private static final String OBJECT_TYPE_CART = "cart";

    private final WebClient rest;
    private final CardTextAccessor cardTextAccessor;
    private final BotTextAccessor botTextAccessor;

    @Autowired
    public SNowCardController(
            WebClient rest,
            CardTextAccessor cardTextAccessor,
            BotTextAccessor botTextAccessor
    ) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.botTextAccessor = botTextAccessor;
    }

    @PostMapping(
            path = "/cards/requests",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<Cards> getCards(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ROUTING_PREFIX) String routingPrefix,
            Locale locale,
            @Valid @RequestBody CardRequest cardRequest,
            ServerHttpRequest request) {
        logger.trace("getCards called, baseUrl={}, routingPrefix={}, request={}", baseUrl, routingPrefix, cardRequest);

        Set<String> requestNumbers = cardRequest.getTokens("ticket_id");

        if (CollectionUtils.isEmpty(requestNumbers)) {
            return Mono.just(new Cards());
        }

        String email = cardRequest.getTokenSingleValue("email");

        if (email == null) {
            return Mono.just(new Cards());
        }

        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return callForUserSysId(baseUri, email, auth)
                .flux()
                .flatMap(userSysId -> callForApprovalRequests(baseUri, auth, userSysId))
                .flatMap(approvalRequest -> callForAndAggregateRequestInfo(baseUri, auth, approvalRequest))
                .filter(info -> requestNumbers.contains(info.getInfo().getNumber()))
                .flatMap(approvalRequestWithInfo -> callForAndAggregateRequestedItems(baseUri, auth, approvalRequestWithInfo))
                .reduce(
                        new Cards(),
                        (cards, info) -> appendCard(cards, info, routingPrefix, locale, request)
                );
    }

    private Mono<String> callForUserSysId(
            URI baseUri,
            String email,
            String auth
    ) {
        logger.trace("callForUserSysId called: baseUrl={}", baseUri);

        return rest.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path("/api/now/table/{userTableName}")
                        .queryParam(SNOW_SYS_PARAM_FIELDS, joinFields(SysUser.Fields.SYS_ID))
                        .queryParam(SNOW_SYS_PARAM_LIMIT, 1)
                        /*
                         * TODO - This is flawed.  It turns out that emails do
                         * not have to uniquely identify users in ServiceNow.
                         * I am able to create 2 different sys_user records
                         * that have the same email.
                         */
                        .queryParam(SysUser.Fields.EMAIL.toString(), email)
                        .build(
                                Map.of(
                                        "userTableName", SysUser.TABLE_NAME
                                )
                        ))
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .flatMap(Reactive.wrapFlatMapper(userInfoResponse -> {
                    String userSysId = userInfoResponse.read("$.result[0]." + SysUser.Fields.SYS_ID);
                    if (userSysId == null) {
                        logger.warn("sys_id for {} not found in {}, returning empty cards", email, baseUri);
                    }
                    return Mono.justOrEmpty(userSysId);
                }));
    }

    private Flux<ApprovalRequest> callForApprovalRequests(
            URI baseUri,
            String auth,
            String userSysId
    ) {
        logger.trace("callForApprovalRequests called: baseUrl={}, userSysId={}", baseUri, userSysId);

        String fields = joinFields(
                SysApprovalApprover.Fields.SYS_ID,
                SysApprovalApprover.Fields.SYSAPPROVAL,
                SysApprovalApprover.Fields.COMMENTS,
                SysApprovalApprover.Fields.DUE_DATE,
                SysApprovalApprover.Fields.SYS_CREATED_BY
        );
        return rest.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path("/api/now/table/{apTableName}")
                        .queryParam(SNOW_SYS_PARAM_FIELDS, fields)
                        .queryParam(SNOW_SYS_PARAM_LIMIT, MAX_APPROVAL_RESULTS)
                        .queryParam(SysApprovalApprover.Fields.SOURCE_TABLE.toString(), ScRequest.TABLE_NAME)
                        .queryParam(SysApprovalApprover.Fields.STATE.toString(), SysApprovalApprover.States.REQUESTED)
                        .queryParam(SysApprovalApprover.Fields.APPROVER.toString(), userSysId)
                        .build(
                                Map.of(
                                        "apTableName", SysApprovalApprover.TABLE_NAME
                                )
                        ))
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(JsonDocument.class)
                /*
                 * I had trouble getting JsonPath to return me something more meaningful than a List<Map<>>.
                 *
                 * I considered making ApprovalRequest a proper DTO and annotating it with @JsonProperty and such,
                 * however, my current thoughts are that it would be weird to tie a hyper-generic api (specifying the
                 * fields for ServiceNow to return) to something more static (JsonProperty annotations on a class).
                 *
                 * I'm not even certain I will keep the ApprovalRequest class.  I found it useful to keep track of
                 * what information I had, but I'm not sure it follows the way we've been doing our code for the other
                 * microservices.
                 */
                .flatMapMany(approvalRequests -> Flux.fromIterable(approvalRequests.<List<Map<String, Object>>>read("$.result[*]")))
                .map(this::convertJsonDocToApprovalReq);

    }

    private String joinFields(Object... args) {
        return Arrays.stream(args)
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    private ApprovalRequest convertJsonDocToApprovalReq(
            Map<String, Object> result
    ) {
        logger.trace("convertJsonDocToApprovalReq called: result={}", result);

        return new ApprovalRequest(
                (String) result.get(SysApprovalApprover.Fields.SYS_ID.toString()),
                ((Map<String, String>) result.get(SysApprovalApprover.Fields.SYSAPPROVAL.toString())).get("value"),
                (String) result.get(SysApprovalApprover.Fields.COMMENTS.toString()),
                (String) result.get(SysApprovalApprover.Fields.DUE_DATE.toString()),
                (String) result.get(SysApprovalApprover.Fields.SYS_CREATED_BY.toString())
        );
    }

    private Mono<ApprovalRequestWithInfo> callForAndAggregateRequestInfo(
            URI baseUri,
            String auth,
            ApprovalRequest approvalRequest
    ) {
        logger.trace("callForAndAggregateRequestInfo called: baseUrl={}, approvalRequest={}", baseUri, approvalRequest);

        return callForRequestInfo(baseUri, auth, approvalRequest)
                .map(requestNumber -> new ApprovalRequestWithInfo(approvalRequest, requestNumber));
    }

    private Mono<Request> callForRequestInfo(
            URI baseUri,
            String auth,
            ApprovalRequest approvalRequest
    ) {
        logger.trace("callForRequestInfo called: baseUrl={}, approvalRequest={}", baseUri, approvalRequest);

        String fields = joinFields(
                ScRequest.Fields.SYS_ID,
                ScRequest.Fields.PRICE,
                ScRequest.Fields.NUMBER
        );

        return rest.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path("/api/now/table/{scTableName}/{approvalSysId}")
                        .queryParam(SNOW_SYS_PARAM_FIELDS, joinFields(fields))
                        .build(
                                Map.of(
                                        "scTableName", ScRequest.TABLE_NAME,
                                        "approvalSysId", approvalRequest.getApprovalSysId()
                                )
                        ))
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .map(
                        reqInfo ->
                                new Request(
                                        reqInfo.read(RESULT_PREFIX + ScRequest.Fields.NUMBER),
                                        reqInfo.read(RESULT_PREFIX + ScRequest.Fields.PRICE)
                                )
                );
    }

    private Mono<ApprovalRequestWithItems> callForAndAggregateRequestedItems(
            URI baseUri,
            String auth,
            ApprovalRequestWithInfo approvalRequest
    ) {
        logger.trace("callForAndAggregateRequestedItems called: baseUrl={}, approvalRequest={}", baseUri, approvalRequest);

        return callForRequestedItems(baseUri, auth, approvalRequest)
                .collectList()
                .map(items -> new ApprovalRequestWithItems(approvalRequest, items));
    }

    private Flux<RequestedItem> callForRequestedItems(
            URI baseUri,
            String auth,
            ApprovalRequestWithInfo approvalRequest
    ) {
        logger.trace("callForRequestedItems called: baseUrl={}, approvalRequest={}", baseUri, approvalRequest);

        String fields = joinFields(
                ScRequestedItem.Fields.SYS_ID,
                ScRequestedItem.Fields.PRICE,
                ScRequestedItem.Fields.REQUEST,
                ScRequestedItem.Fields.SHORT_DESCRIPTION,
                ScRequestedItem.Fields.QUANTITY
        );

        return rest.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path("/api/now/table/{scTableName}")
                        .queryParam(SNOW_SYS_PARAM_FIELDS, joinFields(fields))
                        .queryParam(SNOW_SYS_PARAM_LIMIT, MAX_APPROVAL_RESULTS)
                        .queryParam(ScRequestedItem.Fields.REQUEST.toString(), approvalRequest.getApprovalSysId())
                        .build(
                                Map.of(
                                        "scTableName", ScRequestedItem.TABLE_NAME
                                )
                        ))
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .flatMapMany(items -> Flux.fromIterable(items.<List<Map<String, Object>>>read("$.result[*]")))
                .map(this::convertJsonDocToRequestedItem);
    }

    private RequestedItem convertJsonDocToRequestedItem(
            Map<String, Object> result
    ) {
        logger.trace("convertJsonDocToApprovalReq called: result={}", result);

        return new RequestedItem(
                (String) result.get(ScRequestedItem.Fields.SYS_ID.toString()),
                ((Map<String, String>) result.get(ScRequestedItem.Fields.REQUEST.toString())).get("value"),
                (String) result.get(ScRequestedItem.Fields.SHORT_DESCRIPTION.toString()),
                (String) result.get(ScRequestedItem.Fields.PRICE.toString()),
                (String) result.get(ScRequestedItem.Fields.QUANTITY.toString())
        );
    }

    private Cards appendCard(Cards cards,
                             ApprovalRequestWithItems info,
                             String routingPrefix,
                             Locale locale,
                             ServerHttpRequest request) {
        logger.trace("appendCard called: cards={}, info={}, routingPrefix={}", cards, info, routingPrefix);

        cards.getCards().add(
                makeCard(routingPrefix, info, locale, request)
        );

        return cards;
    }

    private Card makeCard(
            String routingPrefix,
            ApprovalRequestWithItems info,
            Locale locale,
            ServerHttpRequest request
    ) {
        logger.trace("makeCard called: routingPrefix={}, info={}", routingPrefix, info);

        final Card.Builder card = new Card.Builder()
                .setName("ServiceNow") // TODO - remove this in APF-536
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(
                        cardTextAccessor.getHeader(locale),
                        cardTextAccessor.getMessage("subtitle", locale, info.getInfo().getNumber())
                )
                .setHash(toCardHash(info))
                .setBody(makeBody(info, locale))
                .addAction(
                        new CardAction.Builder()
                                .setPrimary(true)
                                .setLabel(cardTextAccessor.getActionLabel("approve", locale))
                                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("approve", locale))
                                .setActionKey(CardActionKey.DIRECT)
                                .setUrl(getServiceNowUrl(routingPrefix, info.getRequestSysId()) + "/approve")
                                .setMutuallyExclusiveSetId(APPROVAL_ACTIONS)
                                .setType(HttpMethod.POST)
                                .build()
                )
                .addAction(
                        new CardAction.Builder()
                                .setLabel(cardTextAccessor.getActionLabel("reject", locale))
                                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("reject", locale))
                                .setActionKey(CardActionKey.USER_INPUT)
                                .setUrl(getServiceNowUrl(routingPrefix, info.getRequestSysId()) + "/reject")
                                .setMutuallyExclusiveSetId(APPROVAL_ACTIONS)
                                .setType(HttpMethod.POST)
                                .addUserInputField(
                                        new CardActionInputField.Builder()
                                                .setId(REASON_PARAM_KEY)
                                                .setLabel(cardTextAccessor.getActionLabel("reject.reason", locale))
                                                .setMinLength(1)
                                                .build()
                                )
                                .build()
                );
        // Set image url.
        CommonUtils.buildConnectorImageUrl(card, request);

        return card.build();
    }

    private static String toCardHash(ApprovalRequestWithItems info) {
        /*
         * Note: The hash isn't really necessary for Boxer cards, however,
         * we'll keep this code duplicated from HubServiceNowController to
         * reduce the chance of it being lost when unforking the connectors
         * in APF-1854.
         */
        List<String> itemsHashes = info.getItems()
                .stream()
                .map(item -> HashUtil.hash("id", item.getSysId(), "qty", item.getQuantity()))
                .collect(Collectors.toList());
        String itemsHash = HashUtil.hashList(itemsHashes);
        return HashUtil.hash("id", info.getInfo().getNumber(), "items", itemsHash);
    }

    private CardBody makeBody(
            ApprovalRequestWithItems info,
            Locale locale
    ) {
        CardBody.Builder body = new CardBody.Builder()
                .addField(
                        new CardBodyField.Builder()
                                .setTitle(cardTextAccessor.getMessage("totalPrice.title", locale))
                                .setType(CardBodyFieldType.GENERAL)
                                .setDescription(cardTextAccessor.getMessage("totalPrice.description", locale, info.getInfo().getTotalPrice()))
                                .build()
                )
                .addField(
                        new CardBodyField.Builder()
                                .setTitle(cardTextAccessor.getMessage("createdBy.title", locale))
                                .setType(CardBodyFieldType.GENERAL)
                                .setDescription(cardTextAccessor.getMessage("createdBy.description", locale, info.getCreatedBy()))
                                .build()
                )
                .addField(
                        new CardBodyField.Builder()
                                .setTitle(cardTextAccessor.getMessage("dueDate.title", locale))
                                .setType(CardBodyFieldType.GENERAL)
                                .setDescription(cardTextAccessor.getMessage("dueDate.description", locale, info.getDueDate()))
                                .build()
                );


        CardBodyField.Builder itemsBuilder = new CardBodyField.Builder()
                .setTitle(cardTextAccessor.getMessage("items.title", locale))
                .setType(CardBodyFieldType.COMMENT);

        for (RequestedItem item : info.getItems()) {
            String lineItem = cardTextAccessor.getMessage(
                    "items.line", locale,
                    item.getShortDescription(),
                    item.getQuantity(),
                    item.getPrice()
            );
            itemsBuilder.addContent(Map.of("text", lineItem));
        }

        return body
                .addField(itemsBuilder.build())
                .build();
    }

    private String getServiceNowUrl(String routingPrefix, String ticketId) {
        return routingPrefix + "api/v1/tickets/" + ticketId;
    }

    @PostMapping(
            path = "/api/v1/tickets/{requestSysId}/approve",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<Map<String, Object>> approve(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @PathVariable("requestSysId") String requestSysId
    ) {
        logger.trace("approve called: baseUrl={}, requestSysId={}", baseUrl, requestSysId);

        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return updateRequest(auth, baseUri, requestSysId, SysApprovalApprover.States.APPROVED, null);
    }

    private Mono<Map<String, Object>> updateRequest(
            String auth,
            URI baseUri,
            String requestSysId,
            SysApprovalApprover.States state,
            String comments
    ) {
        logger.trace("updateState called: baseUrl={}, requestSysId={}, state={}", baseUri, requestSysId, state);

        Map<String, String> body;
        if (StringUtils.isNotBlank(comments)) {
            body = Map.of(
                    SysApprovalApprover.Fields.STATE.toString(), state.toString(),
                    SysApprovalApprover.Fields.COMMENTS.toString(), comments);
        } else {
            body = Map.of(
                    SysApprovalApprover.Fields.STATE.toString(), state.toString());
        }

        String fields = joinFields(
                SysApprovalApprover.Fields.SYS_ID,
                SysApprovalApprover.Fields.STATE,
                SysApprovalApprover.Fields.COMMENTS
        );
        return rest.patch()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUri.getScheme())
                        .host(baseUri.getHost())
                        .port(baseUri.getPort())
                        .path("/api/now/table/{apTableName}/{requestSysId}")
                        .queryParam(SNOW_SYS_PARAM_FIELDS, fields)
                        .build(
                                Map.of(
                                        "apTableName", SysApprovalApprover.TABLE_NAME,
                                        "requestSysId", requestSysId
                                )
                        ))
                .header(AUTHORIZATION, auth)
                .contentType(APPLICATION_JSON)
                .syncBody(body)
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .map(data -> Map.of(
                        "approval_sys_id", data.read(RESULT_PREFIX + SysApprovalApprover.Fields.SYS_ID),
                        "approval_state", data.read(RESULT_PREFIX + SysApprovalApprover.Fields.STATE),
                        "approval_comments", data.read(RESULT_PREFIX + SysApprovalApprover.Fields.COMMENTS)
                ));
    }

    @PostMapping(
            path = "/api/v1/tickets/{requestSysId}/reject",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<Map<String, Object>> reject(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @PathVariable("requestSysId") String requestSysId,
            @Valid RejectForm form
    ) {
        logger.trace("reject called: baseUrl={}, requestSysId={}, reason={}", baseUrl, requestSysId, form.getReason());

        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return updateRequest(auth, baseUri, requestSysId, SysApprovalApprover.States.REJECTED, form.getReason());
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
            Locale locale,
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
                .map(itemList -> toCatalogBotObj(itemList, routingPrefix, contextId, locale));
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

    private BotObjects toCatalogBotObj(List<CatalogItem> itemList, String routingPrefix, String contextId, Locale locale) {
        BotObjects.Builder objectsBuilder = new BotObjects.Builder();

        itemList.forEach(catalogItem ->
                objectsBuilder.addObject(
                        new BotItem.Builder()
                                .setTitle(catalogItem.getName())
                                .setDescription(catalogItem.getShortDescription())
                                .addAction(getAddToCartAction(catalogItem.getId(), routingPrefix, locale))
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
            Locale locale,
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
                .map(taskList -> toTaskBotObj(taskList, routingPrefix, contextId, locale));
    }

    private URI buildTasksUriByReqParam(String taskType, String taskNumber, String userEmail, String baseUrl, Integer limit, Integer offset) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder
                .fromUriString(baseUrl)
                .replacePath("/api/now/table/")
                .path(taskType);

        if (StringUtils.isBlank(taskNumber)) {
            uriComponentsBuilder
                    .queryParam(SNOW_SYS_PARAM_LIMIT, limit)
                    .queryParam(SNOW_SYS_PARAM_OFFSET, offset)
                    .queryParam("opened_by.email", userEmail);
        } else {
            // If task number is provided, may be it doesn't matter to apply the filter about who created the ticket.
            uriComponentsBuilder
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

    private BotObjects toTaskBotObj(List<Task> tasks, String routingPrefix, String contextId, Locale locale) {
        BotObjects.Builder objectsBuilder = new BotObjects.Builder();

        tasks.forEach(task ->
                objectsBuilder.addObject(new BotItem.Builder()
                        .setTitle(botTextAccessor.getObjectTitle(OBJECT_TYPE_TASK, locale, task.getNumber()))
                        .setShortDescription(task.getShortDescription())
                        .addAction(getDeleteTaskAction(task.getSysId(), routingPrefix, locale))
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
            @RequestHeader(ROUTING_PREFIX) String routingPrefix,
            Locale locale,
            @RequestBody CardRequest cardRequest) {

        String contextId = cardRequest.getTokenSingleValue("context_id");

        return retrieveUserCart(baseUrl, auth)
                .map(cartDocument -> toCartBotObj(cartDocument, routingPrefix, contextId, locale));
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

    private BotObjects toCartBotObj(JsonDocument cartResponse, String routingPrefix, String contextId, Locale locale) {
        BotItem.Builder botObjectBuilder = new BotItem.Builder()
                .setTitle(botTextAccessor.getObjectTitle(OBJECT_TYPE_CART, locale))
                .setContextId(contextId);

        List<LinkedHashMap> items = cartResponse.read("$.result.*.items.*");
        if (CollectionUtils.isEmpty(items)) {
            botObjectBuilder.setDescription(botTextAccessor.getMessage(OBJECT_TYPE_CART + ".description.empty", locale));
        } else {
            botObjectBuilder.setDescription(botTextAccessor.getObjectDescription(OBJECT_TYPE_CART, locale))
                    .addAction(getEmptyCartAction(routingPrefix, locale))
                    .addAction(getCheckoutCartAction(routingPrefix, locale));

            List<CartItem> cartItems = objectMapper.convertValue(items, new TypeReference<List<CartItem>>(){});
            cartItems.forEach(
                    cartItem -> botObjectBuilder.addChild(getCartItemChildObject(cartItem, routingPrefix, contextId, locale))
            );
        }

        return new BotObjects.Builder()
                .addObject(botObjectBuilder.build())
                .build();
    }

    private BotItem getCartItemChildObject(CartItem cartItem, String routingPrefix, String contextId, Locale locale) {
        return new BotItem.Builder()
                .setTitle(cartItem.getName())
                .setContextId(contextId)
                .setDescription(cartItem.getShortDescription())
                .addAction(getRemoveFromCartAction(cartItem.getEntryId(), routingPrefix, locale))
                .build();
    }

    private BotAction getRemoveFromCartAction(String entryId, String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(botTextAccessor.getActionTitle("removeFromCart", locale))
                .setDescription(botTextAccessor.getActionDescription("removeFromCart", locale))
                .setType(HttpMethod.DELETE)
                .setUrl(new Link(routingPrefix + "api/v1/cart/" + entryId))
                .build();
    }

    private BotAction getCheckoutCartAction(String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(botTextAccessor.getActionTitle("checkout", locale))
                .setDescription(botTextAccessor.getActionDescription("checkout", locale))
                .setType(HttpMethod.POST)
                .setUrl(new Link(routingPrefix + "api/v1/checkout"))
                .build();
    }

    private BotAction getEmptyCartAction(String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(botTextAccessor.getActionTitle("emptyCart", locale))
                .setDescription(botTextAccessor.getActionDescription("emptyCart", locale))
                .setType(HttpMethod.DELETE)
                .setUrl(new Link(routingPrefix + "api/v1/cart"))
                .build();
    }

    private BotAction getAddToCartAction(String itemId, String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(botTextAccessor.getActionTitle("addToCart", locale))
                .setDescription(botTextAccessor.getActionDescription("addToCart", locale))
                .setType(HttpMethod.PUT)
                .addReqParam("item_id", itemId)
                .addUserInputParam("item_count", botTextAccessor.getActionUserInputLabel("addToCart", "itemCount", locale))
                .setUrl(new Link(routingPrefix + "api/v1/cart"))
                .build();
    }

    private BotAction getDeleteTaskAction(String taskSysId, String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(botTextAccessor.getActionTitle("deleteTicket", locale))
                .setDescription(botTextAccessor.getActionDescription("deleteTicket", locale))
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
            Locale locale,
            @Valid CreateTaskForm form) {

        // ToDo: Validate.
        // ToDo: Should we make it optional and default it to "task" ?
        String taskType = form.getType();

        String shortDescription = form.getShortDescription();

        var userEmail = AuthUtil.extractUserEmail(mfToken);

        logger.trace("createTicket for baseUrl={}, taskType={}, userEmail={}, routingTemplate={}", baseUrl, taskType, userEmail, routingPrefixTemplate);

        String routingPrefix = routingPrefixTemplate.replace(INSERT_OBJECT_TYPE, OBJECT_TYPE_TASK);

        URI baseUri = UriComponentsBuilder.fromUriString(baseUrl).build().toUri();
        return this.createTask(taskType, shortDescription, userEmail, baseUri, auth)
                .map(taskNumber -> new CardRequest(Map.of("number", Set.of((String) taskNumber)), null))
                .flatMap(taskObjReq -> getTasks(mfToken, auth, routingPrefix, baseUrl, locale, taskObjReq, 1, 0));
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
            Locale locale,
            @Valid AddToCartForm form) {

        String itemId = form.getItemId();
        // ToDo: Default itemCount to 1 ?
        Integer itemCount = form.getItemCount();

        logger.trace("addToCart itemId={}, count={}, baseUrl={}", itemId, itemCount, baseUrl);

        String routingPrefix = routingPrefixTemplate.replace(INSERT_OBJECT_TYPE, OBJECT_TYPE_CART);

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
                .then(this.lookupCart(auth, baseUrl, routingPrefix, locale, new CardRequest(null, null)));
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
            Locale locale,
            @PathVariable("cartItemId") String cartItemId,
            @RequestHeader(ROUTING_PREFIX_TEMPLATE) String routingPrefixTemplate) {
        logger.trace("deleteFromCart cartItem entryId={}, baseUrl={}", cartItemId, baseUrl);

        String routingPrefix = routingPrefixTemplate.replace(INSERT_OBJECT_TYPE, OBJECT_TYPE_CART);
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
                .then(this.lookupCart(auth, baseUrl, routingPrefix, locale, new CardRequest(null, null)));
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
            @RequestHeader(ROUTING_PREFIX_TEMPLATE) String routingPrefixTemplate,
            Locale locale) {

        var userEmail = AuthUtil.extractUserEmail(mfToken);

        logger.trace("checkout cart for user={}, baseUrl={}, routingTemplate={}", userEmail, baseUrl, routingPrefixTemplate);

        String routingPrefix = routingPrefixTemplate.replace(INSERT_OBJECT_TYPE, OBJECT_TYPE_TASK);

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
                .flatMap(taskObjReq -> getTasks(mfToken, auth, routingPrefix, baseUrl, locale, taskObjReq, 1, 0));

    }

    @ExceptionHandler(CatalogReadException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public Map<String, String> connectorDisabledErrorHandler(CatalogReadException e) {
        return Map.of("message", e.getMessage());
    }
}
