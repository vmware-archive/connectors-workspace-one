/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.Async;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.ObservableUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.util.HtmlUtils;
import rx.Observable;
import rx.Single;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.vmware.connectors.concur.ConcurConstants.ConcurRequestActions.*;
import static com.vmware.connectors.concur.ConcurConstants.ConcurResponseActions.SUBMITTED_AND_PENDING_APPROVAL;
import static com.vmware.connectors.concur.ConcurConstants.Fields.EXPENSE_REPORT_ID;
import static com.vmware.connectors.concur.ConcurConstants.Header.*;
import static com.vmware.connectors.concur.ConcurConstants.RequestParam.REASON;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.MediaType.*;

@RestController
public class ConcurController {

    private static final Logger logger = LoggerFactory.getLogger(ConcurController.class);

    @Resource
    private AsyncRestOperations rest;

    @Resource
    private CardTextAccessor cardTextAccessor;

    @Value("classpath:static/templates/concur-request-template.xml")
    private org.springframework.core.io.Resource concurrRequestTemplate;

    @PostMapping(path = "/cards/requests",
            produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE)
    public Single<ResponseEntity<Cards>> getCards(
            @RequestHeader(name = AUTHORIZATION_HEADER) final String authHeader,
            @RequestHeader(name = BACKEND_BASE_URL_HEADER) final String baseUrl,
            @RequestHeader(name = ROUTING_PREFIX) final String routingPrefix,
            @Valid @RequestBody CardRequest cardRequest) {

        final Set<String> expenseReportIds = cardRequest.getTokens(EXPENSE_REPORT_ID);
        if (CollectionUtils.isEmpty(expenseReportIds)) {
            logger.info("Expense report ids are empty for the base URL: {} ", baseUrl);
            return Single.just(ResponseEntity.ok(new Cards()));
        }

        final HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, authHeader);
        headers.set(ACCEPT, APPLICATION_JSON_VALUE);

        return Observable.from(expenseReportIds)
                .flatMap(expenseReportId -> getCardsForExpenseReport(headers, expenseReportId, baseUrl, routingPrefix))
                .collect(Cards::new, (cards, card) -> cards.getCards().add(card))
                .map(ResponseEntity::ok)
                .toSingle();
    }

    @PostMapping(path = "/api/expense/approve/{expenseReportId}",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public Single<ResponseEntity<String>> approveRequest(
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
    public Single<ResponseEntity<String>> rejectRequest(
            @RequestHeader(name = AUTHORIZATION_HEADER) final String authHeader,
            @RequestHeader(name = BACKEND_BASE_URL_HEADER) final String baseUrl,
            @RequestParam(name = REASON) final String reason,
            @PathVariable(name = ConcurConstants.PathVariable.EXPENSE_REPORT_ID) final String workflowstepId) throws IOException, ExecutionException, InterruptedException {
        logger.debug("Rejecting the concur expense for the base concur URL: {} and expense report with ID: {}", baseUrl, workflowstepId);

        return makeConcurActionRequest(baseUrl, reason, workflowstepId, authHeader, REJECT);
    }

    @GetMapping("/test-auth")
    public Single<ResponseEntity<Void>> verifyAuth(@RequestHeader(AUTHORIZATION_HEADER) final String authHeader,
                                                   @RequestHeader(BACKEND_BASE_URL_HEADER) final String baseUrl) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, authHeader);

        return Async.toSingle(this.rest.exchange("{baseUrl}/api/v3.0/expense/reports",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Void.class,
                baseUrl))
                .map(response -> ResponseEntity.status(response.getStatusCode()).build());
    }

    private Single<ResponseEntity<String>> makeConcurActionRequest(final String baseUrl,
                                                                 final String reason,
                                                                 final String reportID,
                                                                 final String authHeader,
                                                                 final String concurAction) throws IOException, ExecutionException, InterruptedException {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, authHeader);
        headers.add(CONTENT_TYPE, APPLICATION_XML_VALUE);
        headers.add(ACCEPT, APPLICATION_JSON_VALUE);

        // Replace the placeholder in concur request template with appropriate action and comment.
        final String concurRequestTemplate = getConcurRequestTemplate(reason, concurAction);

        return getWorkFlowActionUrl(headers, reportID, baseUrl)
                .flatMap(workflowActionUrl -> {
                    final ListenableFuture<ResponseEntity<String>> response = rest.exchange(workflowActionUrl,
                            HttpMethod.POST,
                            new HttpEntity<>(concurRequestTemplate, headers),
                            String.class,
                            baseUrl,
                            reportID);

                    return Async.toSingle(response)
                            .map(entity -> ResponseEntity.status(entity.getStatusCode()).build());
                });
    }

    private String getConcurRequestTemplate(final String reason,
                                            final String concurAction) throws IOException {
        String concurRequestTemplate = IOUtils.toString(this.concurrRequestTemplate.getInputStream(), StandardCharsets.UTF_8);
        concurRequestTemplate = concurRequestTemplate.replace(ACTION_PLACEHOLDER, concurAction);
        concurRequestTemplate = concurRequestTemplate.replace(COMMENT_PLACEHOLDER, HtmlUtils.htmlEscape(reason));
        return concurRequestTemplate;
    }


    private Observable<Card> getCardsForExpenseReport(final HttpHeaders headers,
                                                      final String id,
                                                      final String baseUrl,
                                                      final String routingPrefix) {
        logger.debug("Requesting expense request info from concur base URL: {} for ticket request id: {}", baseUrl, id);

        final Single<ResponseEntity<JsonDocument>> result = getReportDetails(headers, id, baseUrl);

        return result
                .toObservable()
                .onErrorResumeNext(ObservableUtil::skip404)
                .map(entity -> convertResponseIntoCard(entity, baseUrl, id, routingPrefix));
    }

    private Single<ResponseEntity<JsonDocument>> getReportDetails(HttpHeaders headers, String id, String baseUrl) {
        final ListenableFuture<ResponseEntity<JsonDocument>> result = this.rest.exchange("{baseUrl}/api/expense/expensereport/v2.0/report/{id}",
                HttpMethod.GET,
                new HttpEntity<String>(headers),
                JsonDocument.class,
                baseUrl,
                id);
        return Async.toSingle(result);
    }

    private Single<String> getWorkFlowActionUrl(final HttpHeaders headers,
                                                final String id,
                                                final String baseUrl) {
        final Single<ResponseEntity<JsonDocument>> result = getReportDetails(headers, id, baseUrl);

        return result.map(ResponseEntity::getBody)
                .map(jsonDocument -> jsonDocument.read("$.WorkflowActionURL"));
    }

    private Card convertResponseIntoCard(final ResponseEntity<JsonDocument> entity,
                                         final String baseUrl,
                                         final String expenseReportId,
                                         final String routingPrefix) {

        final JsonDocument response = entity.getBody();
        final String approvalStatus = response.read("$.ApprovalStatusName");
        final String reportFrom = response.read("$.EmployeeName");
        final String reportPurpose = response.read("$.ReportName");
        final String reportAmount = String.format("%.2f", Float.parseFloat(response.read("$.ReportTotal"))) + " " + response.read("$.CurrencyCode");

        CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .addField(
                        new CardBodyField.Builder()
                                .setTitle(this.cardTextAccessor.getMessage("concur.report.status"))
                                .setDescription(approvalStatus)
                                .setType(CardBodyFieldType.GENERAL)
                                .build()
                )
                .addField(
                        new CardBodyField.Builder()
                                .setTitle(this.cardTextAccessor.getMessage("concur.report.from"))
                                .setDescription(reportFrom)
                                .setType(CardBodyFieldType.GENERAL)
                                .build()
                )
                .addField(
                        new CardBodyField.Builder()
                                .setTitle(this.cardTextAccessor.getMessage("concur.report.purpose"))
                                .setDescription(reportPurpose)
                                .setType(CardBodyFieldType.GENERAL)
                                .build()
                )
                .addField(
                        new CardBodyField.Builder()
                                .setTitle(this.cardTextAccessor.getMessage("concur.report.amount"))
                                .setDescription(reportAmount)
                                .setType(CardBodyFieldType.GENERAL)
                                .build()
                );

        final CardAction.Builder openActionBuilder = getOpenActionBuilder(baseUrl);
        final Card.Builder cardBuilder = new Card.Builder()
                .setName("Concur")
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(cardTextAccessor.getMessage("concur.title"), null)
                .setBody(cardBodyBuilder.build())
                .addAction(openActionBuilder.build());

        // Add approve and reject actions only if the approval status is submitted and pending approval.
        if (SUBMITTED_AND_PENDING_APPROVAL.equalsIgnoreCase(approvalStatus)) {
            CardAction.Builder approveActionBuilder = getApproveActionBuilder(expenseReportId, routingPrefix);
            CardAction.Builder rejectActionBuilder = getRejectActionBuilder(expenseReportId, routingPrefix);

            cardBuilder.addAction(approveActionBuilder.build());
            cardBuilder.addAction(rejectActionBuilder.build());
        }
        return cardBuilder.build();
    }

    private CardAction.Builder getApproveActionBuilder(final String expenseReportId, final String routingPrefix) {
        final String approveUrl = "api/expense/approve/" + expenseReportId;

        // Approver has to enter the comment to approve the expense request.
        return new CardAction.Builder()
                .setLabel(this.cardTextAccessor.getMessage("concur.approve"))
                .setCompletedLabel(this.cardTextAccessor.getMessage("concur.approved"))
                .setActionKey(CardActionKey.USER_INPUT)
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + approveUrl)
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setId(REASON)
                                .setLabel(cardTextAccessor.getMessage("concur.approve.reason.label"))
                                .setMinLength(1)
                                .build()
                );
    }

    private CardAction.Builder getRejectActionBuilder(final String expenseReportId, final String routingPrefix) {
        final String rejectUrl = "api/expense/reject/" + expenseReportId;

        // Approver has to enter the comment to reject the expense request.
        return new CardAction.Builder()
                .setLabel(this.cardTextAccessor.getMessage("concur.reject"))
                .setCompletedLabel(this.cardTextAccessor.getMessage("concur.rejected"))
                .setActionKey(CardActionKey.USER_INPUT)
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + rejectUrl)
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setId(REASON)
                                .setLabel(cardTextAccessor.getMessage("concur.reject.reason.label"))
                                .setMinLength(1)
                                .build()
                );
    }

    private CardAction.Builder getOpenActionBuilder(final String baseUrl) {
        // Did not find any concur API to open the concur page with report directly. Only baseUrl is added.
        return new CardAction.Builder()
                .setLabel(this.cardTextAccessor.getMessage("concur.open"))
                .setActionKey(CardActionKey.OPEN_IN)
                .setType(HttpMethod.GET)
                .setUrl(baseUrl);
    }
}
