/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow;

import com.google.common.collect.ImmutableMap;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.CommonUtils;
import com.vmware.connectors.common.utils.HashUtil;
import com.vmware.connectors.common.utils.Reactive;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

import static com.vmware.connectors.common.utils.CommonUtils.APPROVAL_ACTIONS;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RestController
public class ServiceNowController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceNowController.class);

    /**
     * The JsonPath prefix for the ServiceNow results.
     */
    private static final String RESULT_PREFIX = "$.result.";

    private static final String AUTH_HEADER = "X-Connector-Authorization";
    private static final String BASE_URL_HEADER = "X-Connector-Base-Url";
    private static final String ROUTING_PREFIX = "x-routing-prefix";

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

    private final WebClient rest;
    private final CardTextAccessor cardTextAccessor;

    @Autowired
    public ServiceNowController(
            WebClient rest,
            CardTextAccessor cardTextAccessor
    ) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
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
            final HttpServletRequest request
    ) {
        logger.trace("getCards called, baseUrl={}, routingPrefix={}, request={}", baseUrl, routingPrefix, cardRequest);

        Set<String> requestNumbers = cardRequest.getTokens("ticket_id");

        if (CollectionUtils.isEmpty(requestNumbers)) {
            return Mono.just(new Cards());
        }

        String email = cardRequest.getTokenSingleValue("email");

        if (email == null) {
            return Mono.just(new Cards());
        }

        return callForUserSysId(baseUrl, email, auth)
                .flux()
                .flatMap(userSysId -> callForApprovalRequests(baseUrl, auth, userSysId))
                .flatMap(approvalRequest -> callForAndAggregateRequestInfo(baseUrl, auth, approvalRequest))
                .filter(info -> requestNumbers.contains(info.getInfo().getNumber()))
                .flatMap(approvalRequestWithInfo -> callForAndAggregateRequestedItems(baseUrl, auth, approvalRequestWithInfo))
                .reduce(
                        new Cards(),
                        (cards, info) -> appendCard(cards, info, routingPrefix, locale, request)
                )
                .subscriberContext(Reactive.setupContext());
    }

    private Mono<String> callForUserSysId(
            String baseUrl,
            String email,
            String auth
    ) {
        logger.trace("callForUserSysId called: baseUrl={}", baseUrl);

        return rest.get()
                .uri(UriComponentsBuilder
                        .fromHttpUrl(baseUrl)
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
                        .buildAndExpand(
                                ImmutableMap.of(
                                        "userTableName", SysUser.TABLE_NAME
                                )
                        )
                        .encode()
                        .toUri())
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .flatMap(Reactive.wrapFlatMapper(userInfoResponse -> {
                    String userSysId = userInfoResponse.read("$.result[0]." + SysUser.Fields.SYS_ID);
                    if (userSysId == null) {
                        logger.warn("sys_id for {} not found in {}, returning empty cards", email, baseUrl);
                    }
                    return Mono.justOrEmpty(userSysId);
                }));
    }

    private Flux<ApprovalRequest> callForApprovalRequests(
            String baseUrl,
            String auth,
            String userSysId
    ) {
        logger.trace("callForApprovalRequests called: baseUrl={}, userSysId={}", baseUrl, userSysId);

        String fields = joinFields(
                SysApprovalApprover.Fields.SYS_ID,
                SysApprovalApprover.Fields.SYSAPPROVAL,
                SysApprovalApprover.Fields.COMMENTS,
                SysApprovalApprover.Fields.DUE_DATE,
                SysApprovalApprover.Fields.SYS_CREATED_BY
        );
        return rest.get()
                .uri(UriComponentsBuilder
                        .fromHttpUrl(baseUrl)
                        .path("/api/now/table/{apTableName}")
                        .queryParam(SNOW_SYS_PARAM_FIELDS, fields)
                        .queryParam(SNOW_SYS_PARAM_LIMIT, MAX_APPROVAL_RESULTS)
                        .queryParam(SysApprovalApprover.Fields.SOURCE_TABLE.toString(), ScRequest.TABLE_NAME)
                        .queryParam(SysApprovalApprover.Fields.STATE.toString(), SysApprovalApprover.States.REQUESTED)
                        .queryParam(SysApprovalApprover.Fields.APPROVER.toString(), userSysId)
                        .buildAndExpand(
                                ImmutableMap.of(
                                        "apTableName", SysApprovalApprover.TABLE_NAME
                                )
                        )
                        .encode()
                        .toUri())
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
            String baseUrl,
            String auth,
            ApprovalRequest approvalRequest
    ) {
        logger.trace("callForAndAggregateRequestInfo called: baseUrl={}, approvalRequest={}", baseUrl, approvalRequest);

        return callForRequestInfo(baseUrl, auth, approvalRequest)
                .map(requestNumber -> new ApprovalRequestWithInfo(approvalRequest, requestNumber));
    }

    private Mono<Request> callForRequestInfo(
            String baseUrl,
            String auth,
            ApprovalRequest approvalRequest
    ) {
        logger.trace("callForRequestInfo called: baseUrl={}, approvalRequest={}", baseUrl, approvalRequest);

        String fields = joinFields(
                ScRequest.Fields.SYS_ID,
                ScRequest.Fields.PRICE,
                ScRequest.Fields.NUMBER
        );

        return rest.get()
                .uri(UriComponentsBuilder
                        .fromHttpUrl(baseUrl)
                        .path("/api/now/table/{scTableName}/{approvalSysId}")
                        .queryParam(SNOW_SYS_PARAM_FIELDS, joinFields(fields))
                        .buildAndExpand(
                                ImmutableMap.of(
                                        "scTableName", ScRequest.TABLE_NAME,
                                        "approvalSysId", approvalRequest.getApprovalSysId()
                                )
                        )
                        .encode()
                        .toUri())
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
            String baseUrl,
            String auth,
            ApprovalRequestWithInfo approvalRequest
    ) {
        logger.trace("callForAndAggregateRequestedItems called: baseUrl={}, approvalRequest={}", baseUrl, approvalRequest);

        return callForRequestedItems(baseUrl, auth, approvalRequest)
                .collectList()
                .map(items -> new ApprovalRequestWithItems(approvalRequest, items));
    }

    private Flux<RequestedItem> callForRequestedItems(
            String baseUrl,
            String auth,
            ApprovalRequestWithInfo approvalRequest
    ) {
        logger.trace("callForRequestedItems called: baseUrl={}, approvalRequest={}", baseUrl, approvalRequest);

        String fields = joinFields(
                ScRequestedItem.Fields.SYS_ID,
                ScRequestedItem.Fields.PRICE,
                ScRequestedItem.Fields.REQUEST,
                ScRequestedItem.Fields.SHORT_DESCRIPTION,
                ScRequestedItem.Fields.QUANTITY
        );

        return rest.get()
                .uri(UriComponentsBuilder
                        .fromHttpUrl(baseUrl)
                        .path("/api/now/table/{scTableName}")
                        .queryParam(SNOW_SYS_PARAM_FIELDS, joinFields(fields))
                        .queryParam(SNOW_SYS_PARAM_LIMIT, MAX_APPROVAL_RESULTS)
                        .queryParam(ScRequestedItem.Fields.REQUEST.toString(), approvalRequest.getApprovalSysId())
                        .buildAndExpand(
                                ImmutableMap.of(
                                        "scTableName", ScRequestedItem.TABLE_NAME
                                )
                        )
                        .encode()
                        .toUri())
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
                             HttpServletRequest request) {
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
            HttpServletRequest request
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
            itemsBuilder.addContent(ImmutableMap.of("text", lineItem));
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

        return updateRequest(auth, baseUrl, requestSysId, SysApprovalApprover.States.APPROVED, null);
    }

    private Mono<Map<String, Object>> updateRequest(
            String auth,
            String baseUrl,
            String requestSysId,
            SysApprovalApprover.States state,
            String comments
    ) {
        logger.trace("updateState called: baseUrl={}, requestSysId={}, state={}", baseUrl, requestSysId, state);

        ImmutableMap.Builder<String, String> body = new ImmutableMap.Builder<String, String>()
                .put(SysApprovalApprover.Fields.STATE.toString(), state.toString());

        if (StringUtils.isNotBlank(comments)) {
            body.put(SysApprovalApprover.Fields.COMMENTS.toString(), comments);
        }

        String fields = joinFields(
                SysApprovalApprover.Fields.SYS_ID,
                SysApprovalApprover.Fields.STATE,
                SysApprovalApprover.Fields.COMMENTS
        );
        return rest.patch()
                .uri(UriComponentsBuilder
                        .fromHttpUrl(baseUrl)
                        .path("/api/now/table/{apTableName}/{requestSysId}")
                        .queryParam(SNOW_SYS_PARAM_FIELDS, fields)
                        .buildAndExpand(
                                ImmutableMap.of(
                                        "apTableName", SysApprovalApprover.TABLE_NAME,
                                        "requestSysId", requestSysId
                                )
                        )
                        .encode()
                        .toUri())
                .header(AUTHORIZATION, auth)
                .contentType(APPLICATION_JSON)
                .syncBody(body.build())
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .map(data -> ImmutableMap.of(
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
            @RequestParam(REASON_PARAM_KEY) String reason
    ) {
        logger.trace("reject called: baseUrl={}, requestSysId={}, reason={}", baseUrl, requestSysId, reason);

        return updateRequest(auth, baseUrl, requestSysId, SysApprovalApprover.States.REJECTED, reason);
    }

}
