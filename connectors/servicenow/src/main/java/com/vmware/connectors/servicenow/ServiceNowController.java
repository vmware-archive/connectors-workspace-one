/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow;

import com.google.common.collect.ImmutableMap;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardAction;
import com.vmware.connectors.common.payloads.response.CardActionInputField;
import com.vmware.connectors.common.payloads.response.CardActionKey;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.CardBodyField;
import com.vmware.connectors.common.payloads.response.CardBodyFieldType;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.utils.Async;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import rx.Observable;
import rx.Single;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class ServiceNowController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceNowController.class);

    /**
     * The JsonPath prefix for the ServiceNow results.
     */
    private static final String RESULT_PREFIX = "$.result.";

    private static final String AUTH_HEADER = "x-servicenow-authorization";
    private static final String BASE_URL_HEADER = "x-servicenow-base-url";
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
     *
     * The default is 10,000.
     */
    private static final String SNOW_SYS_PARAM_LIMIT = "sysparm_limit";

    /**
     * The maximum approval requests to fetch from ServiceNow.  Since we have
     * to filter results out based on the ticket_id param passed in by the
     * client, this has to be sufficiently large to not lose results.
     *
     * I wasn't able to find a REST call that would allow me to bulk lookup the
     * approval requests (or requests) by multiple request numbers
     * (ex. REQ0010001,REQ0010002,REQ0010003), so I'm forced to do things a
     * little less ideal than I would like (calling 1x per result of the
     * sysapproval_approver call to be able to match it to the request numbers
     * passed in by the client).
     */
    private static final int MAX_APPROVAL_RESULTS = 10000;

    private final AsyncRestOperations rest;
    private final CardTextAccessor cardTextAccessor;

    @Autowired
    public ServiceNowController(
            AsyncRestOperations rest,
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
    public Single<ResponseEntity<Cards>> getCards(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ROUTING_PREFIX) String routingPrefix,
            @Valid @RequestBody CardRequest request
    ) {
        logger.trace("getCards called, baseUrl={}, routingPrefix={}, request={}", baseUrl, routingPrefix, request);

        Set<String> requestNumbers = request.getTokens("ticket_id");

        if (CollectionUtils.isEmpty(requestNumbers)) {
            return Single.just(ResponseEntity.ok(new Cards()));
        }

        String email = request.getTokenSingleValue("email");

        if (email == null) {
            return Single.just(ResponseEntity.ok(new Cards()));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", auth);

        HttpEntity<HttpHeaders> httpHeaders = new HttpEntity<>(headers);

        return callForUserSysId(baseUrl, email, httpHeaders)
                .flatMap(userSysId -> callForApprovalRequests(baseUrl, httpHeaders, userSysId))
                .flatMapObservable(approvalRequests -> callForAllRequestNumbers(baseUrl, httpHeaders, approvalRequests))
                .filter(info -> requestNumbers.contains(info.getInfo().getNumber()))
                .flatMap(approvalRequestWithInfo -> callForAndAggregateRequestedItems(baseUrl, httpHeaders, approvalRequestWithInfo))
                .reduce(
                        new Cards(),
                        (cards, info) -> appendCard(cards, info, routingPrefix)
                )
                .toSingle()
                .map(ResponseEntity::ok);
    }

    private Single<String> callForUserSysId(
            String baseUrl,
            String email,
            HttpEntity<HttpHeaders> headers
    ) {
        logger.trace("callForUserSysId called: baseUrl={}", baseUrl);

        ListenableFuture<ResponseEntity<JsonDocument>> response = rest.exchange(
                UriComponentsBuilder
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
                        .toUri(),
                HttpMethod.GET,
                headers,
                JsonDocument.class
        );

        return Async.toSingle(response)
                .map(userInfoResponse -> userInfoResponse.getBody().read("$.result[0]." + SysUser.Fields.SYS_ID));
    }

    private Single<List<ApprovalRequest>> callForApprovalRequests(
            String baseUrl,
            HttpEntity<HttpHeaders> headers,
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

        ListenableFuture<ResponseEntity<JsonDocument>> response = rest.exchange(
                UriComponentsBuilder
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
                        .toUri(),
                HttpMethod.GET,
                headers,
                JsonDocument.class
        );

        return Async.toSingle(response)
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
                .map(approvalRequestsResponse -> approvalRequestsResponse.getBody().<List<Map<String, Object>>>read("$.result[*]"))
                .map(results -> results.stream().map(this::convertJsonDocToApprovalReq).collect(Collectors.toList()));
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

    private Observable<ApprovalRequestWithInfo> callForAllRequestNumbers(
            String baseUrl,
            HttpEntity<HttpHeaders> headers,
            List<ApprovalRequest> approvalRequests
    ) {
        logger.trace("callForAllRequestNumbers called: baseUrl={}, approvalRequests={}", baseUrl, approvalRequests);

        return Observable.from(approvalRequests)
                .flatMap(approvalRequest -> callForAndAggregateRequestInfo(baseUrl, headers, approvalRequest));
    }

    private Observable<ApprovalRequestWithInfo> callForAndAggregateRequestInfo(
            String baseUrl,
            HttpEntity<HttpHeaders> headers,
            ApprovalRequest approvalRequest
    ) {
        logger.trace("callForAndAggregateRequestInfo called: baseUrl={}, approvalRequest={}", baseUrl, approvalRequest);

        return callForRequestInfo(baseUrl, headers, approvalRequest)
                .toObservable()
                .map(requestNumber -> new ApprovalRequestWithInfo(approvalRequest, requestNumber));
    }

    private Single<Request> callForRequestInfo(
            String baseUrl,
            HttpEntity<HttpHeaders> headers,
            ApprovalRequest approvalRequest
    ) {
        logger.trace("callForRequestInfo called: baseUrl={}, approvalRequest={}", baseUrl, approvalRequest);

        String fields = joinFields(
                ScRequest.Fields.SYS_ID,
                ScRequest.Fields.PRICE,
                ScRequest.Fields.NUMBER
        );

        ListenableFuture<ResponseEntity<JsonDocument>> response = rest.exchange(
                UriComponentsBuilder
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
                        .toUri(),
                HttpMethod.GET,
                headers,
                JsonDocument.class
        );

        return Async.toSingle(response)
                .map(
                        reqInfoResponse ->
                                new Request(
                                        reqInfoResponse.getBody().read(RESULT_PREFIX + ScRequest.Fields.NUMBER),
                                        reqInfoResponse.getBody().read(RESULT_PREFIX + ScRequest.Fields.PRICE)
                                )
                );
    }

    private Observable<ApprovalRequestWithItems> callForAndAggregateRequestedItems(
            String baseUrl,
            HttpEntity<HttpHeaders> headers,
            ApprovalRequestWithInfo approvalRequest
    ) {
        logger.trace("callForAndAggregateRequestedItems called: baseUrl={}, approvalRequest={}", baseUrl, approvalRequest);

        return callForRequestedItems(baseUrl, headers, approvalRequest)
                .toObservable()
                .map(items -> new ApprovalRequestWithItems(approvalRequest, items));
    }

    private Single<List<RequestedItem>> callForRequestedItems(
            String baseUrl,
            HttpEntity<HttpHeaders> headers,
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

        ListenableFuture<ResponseEntity<JsonDocument>> response = rest.exchange(
                UriComponentsBuilder
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
                        .toUri(),
                HttpMethod.GET,
                headers,
                JsonDocument.class
        );

        return Async.toSingle(response)
                .map(itemsResponse -> itemsResponse.getBody().<List<Map<String, Object>>>read("$.result[*]"))
                .map(results -> results.stream().map(this::convertJsonDocToRequestedItem).collect(Collectors.toList()));
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

    private Cards appendCard(Cards cards, ApprovalRequestWithItems info, String routingPrefix) {
        logger.trace("appendCard called: cards={}, info={}, routingPrefix={}", cards, info, routingPrefix);

        cards.getCards().add(
                makeCard(
                        routingPrefix,
                        info
                )
        );

        return cards;
    }

    private Card makeCard(
            String routingPrefix,
            ApprovalRequestWithItems info
    ) {
        logger.trace("makeCard called: routingPrefix={}, info={}", routingPrefix, info);

        return new Card.Builder()
                .setName("ServiceNow") // TODO - remove this in APF-536
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(cardTextAccessor.getHeader(info.getInfo().getNumber()), null)
                .setBody(makeBody(info))
                .addAction(
                        new CardAction.Builder()
                                .setPrimary(true)
                                .setLabel(cardTextAccessor.getActionLabel("approve"))
                                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("approve"))
                                .setActionKey(CardActionKey.DIRECT)
                                .setUrl(getServiceNowUrl(routingPrefix, info.getRequestSysId()) + "/approve")
                                .setType(HttpMethod.POST)
                                .build()
                )
                .addAction(
                        new CardAction.Builder()
                                .setLabel(cardTextAccessor.getActionLabel("reject"))
                                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("reject"))
                                .setActionKey(CardActionKey.USER_INPUT)
                                .setUrl(getServiceNowUrl(routingPrefix, info.getRequestSysId()) + "/reject")
                                .setType(HttpMethod.POST)
                                .addUserInputField(
                                        new CardActionInputField.Builder()
                                                .setId(REASON_PARAM_KEY)
                                                .setLabel(cardTextAccessor.getActionLabel("reject.reason"))
                                                .setMinLength(1)
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    private CardBody makeBody(
            ApprovalRequestWithItems info
    ) {
        CardBody.Builder body = new CardBody.Builder()
                .addField(
                        new CardBodyField.Builder()
                                .setTitle(cardTextAccessor.getMessage("totalPrice.title"))
                                .setType(CardBodyFieldType.GENERAL)
                                .setDescription(cardTextAccessor.getMessage("totalPrice.description", info.getInfo().getTotalPrice()))
                                .build()
                )
                .addField(
                        new CardBodyField.Builder()
                                .setTitle(cardTextAccessor.getMessage("createdBy.title"))
                                .setType(CardBodyFieldType.GENERAL)
                                .setDescription(cardTextAccessor.getMessage("createdBy.description", info.getCreatedBy()))
                                .build()
                )
                .addField(
                        new CardBodyField.Builder()
                                .setTitle(cardTextAccessor.getMessage("dueDate.title"))
                                .setType(CardBodyFieldType.GENERAL)
                                .setDescription(cardTextAccessor.getMessage("dueDate.description", info.getDueDate()))
                                .build()
                );


        CardBodyField.Builder itemsBuilder = new CardBodyField.Builder()
                .setTitle(cardTextAccessor.getMessage("items.title"))
                .setType(CardBodyFieldType.COMMENT);

        for (RequestedItem item : info.getItems()) {
            String lineItem = cardTextAccessor.getMessage(
                    "items.line",
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
    public Single<ResponseEntity<Map<String, Object>>> approve(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @PathVariable("requestSysId") String requestSysId
    ) {
        logger.trace("approve called: baseUrl={}, requestSysId={}", baseUrl, requestSysId);

        return updateRequest(auth, baseUrl, requestSysId, SysApprovalApprover.States.APPROVED, null);
    }

    private Single<ResponseEntity<Map<String, Object>>> updateRequest(
            String auth,
            String baseUrl,
            String requestSysId,
            SysApprovalApprover.States state,
            String comments
    ) {
        logger.trace("updateState called: baseUrl={}, requestSysId={}, state={}", baseUrl, requestSysId, state);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", auth);
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);

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

        ListenableFuture<ResponseEntity<JsonDocument>> response = rest.exchange(
                UriComponentsBuilder
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
                        .toUri(),
                HttpMethod.PATCH,
                new HttpEntity<>(body.build(), headers),
                JsonDocument.class
        );

        return Async.toSingle(response)
                .map(ResponseEntity::getBody)
                .map(data -> ImmutableMap.of(
                        "approval_sys_id", data.read(RESULT_PREFIX + SysApprovalApprover.Fields.SYS_ID),
                        "approval_state", data.read(RESULT_PREFIX + SysApprovalApprover.Fields.STATE),
                        "approval_comments", data.read(RESULT_PREFIX + SysApprovalApprover.Fields.COMMENTS)
                ))
                .map(ResponseEntity::ok);
    }

    @PostMapping(
            path = "/api/v1/tickets/{requestSysId}/reject",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<Map<String, Object>>> reject(
            @RequestHeader(AUTH_HEADER) String auth,
            @RequestHeader(BASE_URL_HEADER) String baseUrl,
            @PathVariable("requestSysId") String requestSysId,
            @RequestParam(REASON_PARAM_KEY) String reason
    ) {
        logger.trace("reject called: baseUrl={}, requestSysId={}, reason={}", baseUrl, requestSysId, reason);

        return updateRequest(auth, baseUrl, requestSysId, SysApprovalApprover.States.REJECTED, reason);
    }

}
