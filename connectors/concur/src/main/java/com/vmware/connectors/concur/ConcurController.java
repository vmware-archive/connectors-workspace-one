/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.Reactive;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.vmware.connectors.concur.ConcurConstants.ConcurRequestActions.*;
import static com.vmware.connectors.concur.ConcurConstants.ConcurResponseActions.SUBMITTED_AND_PENDING_APPROVAL;
import static com.vmware.connectors.concur.ConcurConstants.Fields.EXPENSE_REPORT_ID;
import static com.vmware.connectors.concur.ConcurConstants.Header.*;
import static com.vmware.connectors.concur.ConcurConstants.RequestParam.REASON;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.*;

@RestController
public class ConcurController {

    private static final Logger logger = LoggerFactory.getLogger(ConcurController.class);


    private final WebClient rest;
    private final CardTextAccessor cardTextAccessor;
    private final Resource concurrRequestTemplate;

    @Autowired
    public ConcurController(WebClient rest,
                            CardTextAccessor cardTextAccessor,
                            @Value("classpath:static/templates/concur-request-template.xml") Resource concurRequestTemplate) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.concurrRequestTemplate = concurRequestTemplate;
    }

    @PostMapping(path = "/cards/requests",
            produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE)
    public Mono<Cards> getCards(
            @RequestHeader(name = AUTHORIZATION_HEADER) final String authHeader,
            @RequestHeader(name = BACKEND_BASE_URL_HEADER) final String baseUrl,
            @RequestHeader(name = ROUTING_PREFIX) final String routingPrefix,
            final Locale locale,
            @Valid @RequestBody CardRequest cardRequest) {

        final Set<String> expenseReportIds = cardRequest.getTokens(EXPENSE_REPORT_ID);

        return Flux.fromIterable(expenseReportIds)
                .flatMap(expenseReportId -> getCardsForExpenseReport(
                        authHeader, expenseReportId, baseUrl, routingPrefix, locale))
                .collect(Cards::new, (cards, card) -> cards.getCards().add(card))
                .defaultIfEmpty(new Cards())
                .subscriberContext(Reactive.setupContext());
    }

    @PostMapping(path = "/api/expense/approve/{expenseReportId}",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public Mono<String> approveRequest(
            @RequestHeader(name = AUTHORIZATION_HEADER) final String authHeader,
            @RequestHeader(name = BACKEND_BASE_URL_HEADER) final String baseUrl,
            @RequestParam(name = REASON) final String reason,
            @PathVariable(name = ConcurConstants.PathVariable.EXPENSE_REPORT_ID) final String workflowstepId) throws IOException, ExecutionException, InterruptedException {
        logger.debug("Approving the concur expense for the base concur URL: {} and expense report with ID: {}", baseUrl, workflowstepId);

        return makeConcurActionRequest(baseUrl, reason, workflowstepId, authHeader, APPROVE);
    }

    @PostMapping(path = "/api/expense/reject/{expenseReportId}",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public Mono<String> rejectRequest(
            @RequestHeader(name = AUTHORIZATION_HEADER) final String authHeader,
            @RequestHeader(name = BACKEND_BASE_URL_HEADER) final String baseUrl,
            @RequestParam(name = REASON) final String reason,
            @PathVariable(name = ConcurConstants.PathVariable.EXPENSE_REPORT_ID) final String workflowstepId) throws IOException, ExecutionException, InterruptedException {
        logger.debug("Rejecting the concur expense for the base concur URL: {} and expense report with ID: {}", baseUrl, workflowstepId);

        return makeConcurActionRequest(baseUrl, reason, workflowstepId, authHeader, REJECT);
    }

    private Mono<String> makeConcurActionRequest(final String baseUrl,
                                                                 final String reason,
                                                                 final String reportID,
                                                                 final String authHeader,
                                                                 final String concurAction) throws IOException, ExecutionException, InterruptedException {
        // Replace the placeholder in concur request template with appropriate action and comment.
        final String concurRequestTemplate = getConcurRequestTemplate(reason, concurAction);

        Mono<String> workFlowActionUrl = getWorkFlowActionUrl(authHeader, reportID, baseUrl);
        return workFlowActionUrl.flatMap(url -> rest.post()
                .uri(url)
                .header(AUTHORIZATION, authHeader)
                .contentType(APPLICATION_XML)
                .accept(APPLICATION_JSON)
                .syncBody(concurRequestTemplate)
                .retrieve()
                .bodyToMono(String.class));
    }

    private String getConcurRequestTemplate(final String reason,
                                            final String concurAction) throws IOException {
        String concurRequestTemplate = IOUtils.toString(this.concurrRequestTemplate.getInputStream(), StandardCharsets.UTF_8);
        concurRequestTemplate = concurRequestTemplate.replace(ACTION_PLACEHOLDER, concurAction);
        concurRequestTemplate = concurRequestTemplate.replace(COMMENT_PLACEHOLDER, HtmlUtils.htmlEscape(reason));
        return concurRequestTemplate;
    }


    private Flux<Card> getCardsForExpenseReport(final String authHeader,
                                                final String id,
                                                final String baseUrl,
                                                final String routingPrefix,
                                                final Locale locale) {
        logger.debug("Requesting expense request info from concur base URL: {} for ticket request id: {}", baseUrl, id);

        return getReportDetails(authHeader, id, baseUrl)
                .flux()
                .onErrorResume(Reactive::skipOnNotFound)
                .map(entity -> convertResponseIntoCard(entity, baseUrl, id, routingPrefix, locale));
    }

    private Mono<ResponseEntity<JsonDocument>> getReportDetails(String authHeader, String id, String baseUrl) {
        return rest.get()
                .uri(baseUrl + "/api/expense/expensereport/v2.0/report/{id}", id)
                .header(AUTHORIZATION, authHeader)
                .accept(APPLICATION_JSON)
                .exchange()
                .flatMap(Reactive::checkStatus)
                .flatMap(response -> Reactive.toResponseEntity(response, JsonDocument.class));
    }

    private Mono<String> getWorkFlowActionUrl(final String authHeader,
                                                final String id,
                                                final String baseUrl) {
        return getReportDetails(authHeader, id, baseUrl)
                .map(ResponseEntity::getBody)
                .map(jsonDocument -> jsonDocument.read("$.WorkflowActionURL"));
    }

    private Card convertResponseIntoCard(final ResponseEntity<JsonDocument> entity,
                                         final String baseUrl,
                                         final String expenseReportId,
                                         final String routingPrefix,
                                         final Locale locale) {
        final JsonDocument response = entity.getBody();
        final String approvalStatus = response.read("$.ApprovalStatusName");

        final Card.Builder cardBuilder = new Card.Builder()
                .setName("Concur")
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(cardTextAccessor.getMessage("concur.title", locale))
                .setBody(buildCardBodyBuilder(response, locale));

        // Add approve and reject actions only if the approval status is submitted and pending approval.
        if (SUBMITTED_AND_PENDING_APPROVAL.equalsIgnoreCase(approvalStatus)) {
            CardAction.Builder approveActionBuilder = getApproveActionBuilder(expenseReportId, routingPrefix, locale);
            CardAction.Builder rejectActionBuilder = getRejectActionBuilder(expenseReportId, routingPrefix, locale);

            cardBuilder.addAction(approveActionBuilder.build());
            cardBuilder.addAction(rejectActionBuilder.build());
        }

        final CardAction.Builder openActionBuilder = getOpenActionBuilder(baseUrl, locale);
        cardBuilder.addAction(openActionBuilder.build());

        return cardBuilder.build();
    }

    private CardBody buildCardBodyBuilder(final JsonDocument response, Locale locale) {
        final String approvalStatus = response.read("$.ApprovalStatusName");
        final String reportFrom = response.read("$.EmployeeName");
        final String reportPurpose = response.read("$.ReportName");
        final String reportAmount = String.format("%.2f", Float.parseFloat(response.read("$.ReportTotal"))) + " " + response.read("$.CurrencyCode");

        CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .addField(makeCardBodyField(this.cardTextAccessor.getMessage("concur.report.status", locale), approvalStatus))
                .addField(makeCardBodyField(this.cardTextAccessor.getMessage("concur.report.from", locale), reportFrom))
                .addField(makeCardBodyField(this.cardTextAccessor.getMessage("concur.report.purpose", locale), reportPurpose))
                .addField(makeCardBodyField(this.cardTextAccessor.getMessage("concur.report.amount", locale), reportAmount));

        if (StringUtils.isNotBlank(response.read("$.ExpenseEntriesList[0].BusinessPurpose"))) {
            cardBodyBuilder.setDescription(response.read("$.ExpenseEntriesList[0].BusinessPurpose"));
        }

        return cardBodyBuilder.build();
    }
    private CardBodyField makeCardBodyField(final String title, final String description) {
        return new CardBodyField.Builder()
                .setTitle(title)
                .setDescription(description)
                .setType(CardBodyFieldType.GENERAL)
                .build();
    }

    private CardAction.Builder getApproveActionBuilder(
            final String expenseReportId, final String routingPrefix, final Locale locale) {
        final String approveUrl = "api/expense/approve/" + expenseReportId;

        // Approver has to enter the comment to approve the expense request.
        return new CardAction.Builder()
                .setLabel(this.cardTextAccessor.getActionLabel("concur.approve", locale))
                .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel("concur.approve", locale))
                .setActionKey(CardActionKey.USER_INPUT)
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + approveUrl)
                .setPrimary(true)
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setId(REASON)
                                .setLabel(cardTextAccessor.getMessage("concur.approve.reason.label", locale))
                                .setMinLength(1)
                                .build()
                );
    }

    private CardAction.Builder getRejectActionBuilder(
            final String expenseReportId, final String routingPrefix, final Locale locale) {
        final String rejectUrl = "api/expense/reject/" + expenseReportId;

        // Approver has to enter the comment to reject the expense request.
        return new CardAction.Builder()
                .setLabel(this.cardTextAccessor.getActionLabel("concur.reject", locale))
                .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel("concur.reject", locale))
                .setActionKey(CardActionKey.USER_INPUT)
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + rejectUrl)
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setId(REASON)
                                .setLabel(this.cardTextAccessor.getMessage("concur.reject.reason.label", locale))
                                .setMinLength(1)
                                .build()
                );
    }

    private CardAction.Builder getOpenActionBuilder(final String baseUrl, final Locale locale) {
        // Did not find any concur API to open the concur page with report directly. Only baseUrl is added.
        return new CardAction.Builder()
                .setLabel(this.cardTextAccessor.getActionLabel("concur.open", locale))
                .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel("concur.open", locale))
                .setActionKey(CardActionKey.OPEN_IN)
                .setAllowRepeated(true)
                .setType(HttpMethod.GET)
                .setUrl(baseUrl);
    }
}
