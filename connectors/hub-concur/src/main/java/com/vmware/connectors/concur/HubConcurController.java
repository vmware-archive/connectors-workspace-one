/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.common.json.JsonDocument;
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
import com.vmware.connectors.common.utils.CommonUtils;
import com.vmware.connectors.common.utils.Reactive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class HubConcurController {

    private static final Logger logger = LoggerFactory.getLogger(HubConcurController.class);

    private static final String X_BASE_URL_HEADER = "X-Connector-Base-Url";

    private static final String COMMENT_KEY = "comment";
    private static final String REASON_KEY = "reason";

    private final WebClient rest;
    private final CardTextAccessor cardTextAccessor;

    @Autowired
    public HubConcurController(
            WebClient rest,
            CardTextAccessor cardTextAccessor
    ) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
    }

    @PostMapping(
            path = "/cards/requests",
            produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE
    )
    public Mono<Cards> getCards(
            @AuthenticationPrincipal String username,
            @RequestHeader(name = AUTHORIZATION) String vidmAuthHeader,
            @RequestHeader(name = X_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(name = "X-Routing-Prefix") String routingPrefix,
            Locale locale,
            @Valid @RequestBody CardRequest cardRequest,
            HttpServletRequest request
    ) {
        logger.debug("getCards called: baseUrl={}, username={}, cardRequest={}", baseUrl, username, cardRequest);

        return fetchAllRequests(baseUrl, vidmAuthHeader, username, cardRequest)
                .map(ResponseEntity::getBody)
                .flatMapIterable(doc -> doc.<List<Map<String, String>>>read("$[*]['id', 'status']"))
                .filter(req -> "open".equals(req.get("status")))
                .flatMap(req -> fetchRequestData(baseUrl, vidmAuthHeader, req.get("id")))
                .map(ResponseEntity::getBody)
                .map(doc -> makeCard(routingPrefix, locale, doc, request))
                .reduce(new Cards(), this::addCard);
    }

    private Cards addCard(
            Cards cards,
            Card card
    ) {
        cards.getCards().add(card);
        return cards;
    }

    private Mono<ResponseEntity<JsonDocument>> fetchAllRequests(
            String baseUrl,
            String vidmAuthHeader,
            String username,
            CardRequest cardRequest
    ) {
        logger.debug("fetchAllRequests called: baseUrl={}, username={}, cardRequest={}", baseUrl, username, cardRequest);

        return rest.get()
                .uri(baseUrl + "/fake-api/requests?user_id={username}", username)
                // For now, since I'm calling into myself for fake data,
                // I'm using the same auth header as the original request
                .header(AUTHORIZATION, vidmAuthHeader)
                .accept(APPLICATION_JSON)
                .exchange()
                .flatMap(Reactive::checkStatus)
                .flatMap(response -> response.toEntity(JsonDocument.class));
    }

    private Mono<ResponseEntity<JsonDocument>> fetchRequestData(
            String baseUrl,
            String vidmAuthHeader,
            String id
    ) {
        logger.debug("fetchRequestData called: baseUrl={}, id={}", baseUrl, id);

        return rest.get()
                .uri(baseUrl + "/fake-api/requests/{id}", id)
                // For now, since I'm calling into myself for fake data,
                // I'm using the same auth header as the original request
                .header(AUTHORIZATION, vidmAuthHeader)
                .accept(APPLICATION_JSON)
                .exchange()
                .flatMap(Reactive::checkStatus)
                .flatMap(response -> response.toEntity(JsonDocument.class));
    }

    private Card makeCard(
            String routingPrefix,
            Locale locale,
            JsonDocument doc,
            HttpServletRequest request
    ) {
        String requestId = doc.read("$.id");
        String reportName = doc.read("$.report_name");
        String projectNumber = doc.read("$.project_number");

        logger.debug("makeCard called: routingPrefix={}, requestId={}, reportName={}", routingPrefix, requestId, reportName);

        Card.Builder builder = new Card.Builder()
                .setName("Concur")
                .setHeader(
                        cardTextAccessor.getMessage("hub.concur.header", locale, reportName),
                        projectNumber
                )
                .setBody(
                        new CardBody.Builder()
                                .addField(makeSubmissionDateField(locale, doc))
                                .addField(makeRequestedByField(locale, doc))
                                .addField(makeCostCenterField(locale, doc))
                                .addField(makeExpenseAmountField(locale, doc))
                                .build()
                )
                .addAction(makeApproveAction(routingPrefix, locale, requestId))
                .addAction(makeDeclineAction(routingPrefix, locale, requestId));

        CommonUtils.buildConnectorImageUrl(builder, request);

        return builder.build();
    }

    private CardBodyField makeSubmissionDateField(
            Locale locale,
            JsonDocument doc
    ) {
        return new CardBodyField.Builder()
                .setType(CardBodyFieldType.GENERAL)
                .setTitle(cardTextAccessor.getMessage("hub.concur.submissionDate", locale))
                .setDescription(doc.read("$.submission_date"))
                .build();
    }

    private CardBodyField makeRequestedByField(
            Locale locale,
            JsonDocument doc
    ) {
        return new CardBodyField.Builder()
                .setType(CardBodyFieldType.GENERAL)
                .setTitle(cardTextAccessor.getMessage("hub.concur.requester", locale))
                .setDescription(doc.read("$.requested_by"))
                .build();
    }

    private CardBodyField makeCostCenterField(
            Locale locale,
            JsonDocument doc
    ) {
        return new CardBodyField.Builder()
                .setType(CardBodyFieldType.GENERAL)
                .setTitle(cardTextAccessor.getMessage("hub.concur.costCenter", locale))
                .setDescription(doc.read("$.cost_center"))
                .build();
    }

    private CardBodyField makeExpenseAmountField(
            Locale locale,
            JsonDocument doc
    ) {
        return new CardBodyField.Builder()
                .setType(CardBodyFieldType.GENERAL)
                .setTitle(cardTextAccessor.getMessage("hub.concur.expenseAmount", locale))
                .setDescription(doc.read("$.expense_amount"))
                .build();
    }

    private CardAction makeApproveAction(
            String routingPrefix,
            Locale locale,
            String requestId
    ) {
        return new CardAction.Builder()
                .setActionKey(CardActionKey.USER_INPUT)
                .setLabel(cardTextAccessor.getMessage("hub.concur.approve.label", locale))
                .setCompletedLabel(cardTextAccessor.getMessage("hub.concur.approve.completedLabel", locale))
                .setPrimary(true)
                .setMutuallyExclusiveSetId("approval-actions")
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + "api/expense/" + requestId + "/approve")
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setFormat("textarea")
                                .setId(COMMENT_KEY)
                                .setLabel(cardTextAccessor.getMessage("hub.concur.approve.comment.label", locale))
                                .build()
                )
                .build();
    }

    private CardAction makeDeclineAction(
            String routingPrefix,
            Locale locale,
            String requestId
    ) {
        return new CardAction.Builder()
                .setActionKey(CardActionKey.USER_INPUT)
                .setLabel(cardTextAccessor.getMessage("hub.concur.decline.label", locale))
                .setCompletedLabel(cardTextAccessor.getMessage("hub.concur.decline.completedLabel", locale))
                .setPrimary(false)
                .setMutuallyExclusiveSetId("approval-actions")
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + "api/expense/" + requestId + "/decline")
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setFormat("textarea")
                                .setId(REASON_KEY)
                                .setLabel(cardTextAccessor.getMessage("hub.concur.decline.reason.label", locale))
                                .build()
                )
                .build();
    }

    @PostMapping(
            path = "/api/expense/{id}/approve",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<Void> approveRequest(
            @RequestHeader(name = X_BASE_URL_HEADER) String baseUrl,
            @PathVariable(name = "id") String id,
            @RequestParam(name = COMMENT_KEY) String comment
    ) {
        logger.debug("approveRequest called: baseUrl={}, id={}, comment={}", baseUrl, id, comment);

        return Mono.empty();
    }

    @PostMapping(
            path = "/api/expense/{id}/decline",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<Void> declineRequest(
            @RequestHeader(name = X_BASE_URL_HEADER) String baseUrl,
            @PathVariable(name = "id") String id,
            @RequestParam(name = REASON_KEY) String reason
    )  {
        logger.debug("declineRequest called: baseUrl={}, id={}, reason={}", baseUrl, id, reason);

        return Mono.empty();
    }

    /////////////////////////////////////////////////////////////////////////
    // Below are fake APIs so I can test that the async rest calls work.
    // Remove these as you interact with the real Concur APIs.
    /////////////////////////////////////////////////////////////////////////

    @GetMapping(
            path = "/fake-api/requests",
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<List<Map<String, Object>>> fakeAllRequests(
            @RequestParam(name = "user_id") String userId
    ) {
        logger.debug("fakeAllRequests called: user_id={}", userId);

        return Mono.just(FakeData.ALL_REQUESTS);
    }

    @GetMapping(
            path = "/fake-api/requests/{id}",
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Map<String, Object>>> fakeRequestData(
            @PathVariable(name = "id") String id
    ) {
        logger.debug("fakeRequestData called: id={}", id);

        return FakeData.ALL_REQUEST_DATA.stream()
                .filter(req -> id.equals(req.get("id")))
                .findFirst()
                .map(ResponseEntity::ok)
                .map(Mono::just)
                .orElse(Mono.just(ResponseEntity.notFound().build()));
    }

}
