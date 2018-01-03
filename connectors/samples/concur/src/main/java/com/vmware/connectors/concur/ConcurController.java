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
import com.vmware.connectors.common.web.ObservableUtil;
import com.vmware.connectors.concur.response.ConcurResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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

import static com.vmware.connectors.concur.ConcurConstants.ConcurActions.*;
import static com.vmware.connectors.concur.ConcurConstants.Fields.EXPENSE_REPORT_ID;
import static com.vmware.connectors.concur.ConcurConstants.Header.*;
import static com.vmware.connectors.concur.ConcurConstants.RequestParam.REASON;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.status;

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
            logger.debug("Expense report ids are empty for the base URL: {} ", baseUrl);
            return Single.just(ResponseEntity.ok(new Cards()));
        }

        final HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, authHeader);

        return Observable.from(expenseReportIds)
                .flatMap(expenseReportId -> getCardsForExpenseReport(headers, expenseReportId, baseUrl, routingPrefix))
                .collect(Cards::new, (cards, card) -> cards.getCards().add(card))
                .map(ResponseEntity::ok)
                .toSingle();
    }

    @PostMapping(path = "/api/expense/approve/{expenseReportId}",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public Single<ResponseEntity<Void>> approveRequest(
            @RequestHeader(name = AUTHORIZATION_HEADER) final String authHeader,
            @RequestHeader(name = BACKEND_BASE_URL_HEADER) final String baseUrl,
            @RequestParam(name = REASON) final String reason,
            @PathVariable(name = ConcurConstants.PathVariable.EXPENSE_REPORT_ID) final String workflowstepId) throws IOException, ExecutionException, InterruptedException {
        logger.info("Approving the concur expense for the base concur URL: {} and expense report with ID: {}", baseUrl, workflowstepId);

        return makeConcurRequest(baseUrl, reason, workflowstepId, authHeader, APPROVE);
    }

    @PostMapping(path = "/api/expense/reject/{expenseReportId}",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public Single<ResponseEntity<Void>> rejectRequest(
            @RequestHeader(name = AUTHORIZATION_HEADER) final String authHeader,
            @RequestHeader(name = BACKEND_BASE_URL_HEADER) final String baseUrl,
            @RequestParam(name = REASON) final String reason,
            @PathVariable(name = ConcurConstants.PathVariable.EXPENSE_REPORT_ID) final String workflowstepId) throws IOException, ExecutionException, InterruptedException {
        logger.info("Rejecting the concur expense for the base concur URL: {} and expense report with ID: {}", baseUrl, workflowstepId);

        return makeConcurRequest(baseUrl, reason, workflowstepId, authHeader, REJECT);
    }

    private Single<ResponseEntity<Void>> makeConcurRequest(final String baseUrl,
                                                           final String reason,
                                                           final String workflowstepId,
                                                           final String authHeader,
                                                           final String concurAction) throws IOException, ExecutionException, InterruptedException {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, authHeader);
        headers.add(CONTENT_TYPE, APPLICATION_XML_VALUE);

        // Replace the placeholder in concur request template with appropriate action and comment.
        final String concurRequestTemplate = getConcurRequestTemplate(reason, concurAction);

        final ListenableFuture<ResponseEntity<ConcurResponse>> response = rest.exchange("{baseUrl}/api/expense/expensereport/v1.1/report/{workflowstepId}/workflowaction",
                HttpMethod.POST,
                new HttpEntity<>(concurRequestTemplate, headers),
                ConcurResponse.class,
                baseUrl,
                workflowstepId);

        logger.info("Concur response : {} and the response code is : {} ", response.get().getBody(), response.get().getStatusCode());
        return Async.toSingle(response)
                .map(entity -> status(entity.getStatusCode()).build());
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

        final ListenableFuture<ResponseEntity<JsonDocument>> result = this.rest.exchange("{baseUrl}/expense/reports/{id}",
                HttpMethod.GET,
                new HttpEntity<String>(headers),
                JsonDocument.class,
                baseUrl,
                id);

        return Async.toSingle(result)
                .toObservable()
                .onErrorResumeNext(ObservableUtil::skip404)
                .map(entity -> convertResponseIntoCard(entity, baseUrl, id, routingPrefix));
    }

    private Card convertResponseIntoCard(final ResponseEntity<JsonDocument> entity,
                                         final String baseUrl,
                                         final String expenseReportId,
                                         final String routingPrefix) {

        final JsonDocument response = entity.getBody();
        final String reportTo = response.read("$.ApproverName");
        final String reportFrom = response.read("$.OwnerName");

        // TODO - Verify the actual reponse from the concur and check whether LedgerName is referring to reportPurpose.
        final String reportPurpose = response.read("$.LedgerName");
        final Integer reportAmount = response.read("$.TotalClaimedAmount");

        CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .addField(
                        new CardBodyField.Builder()
                                .setTitle(this.cardTextAccessor.getMessage("concur.report.to"))
                                .setDescription(reportTo)
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
                                .setDescription(String.valueOf(reportAmount))
                                .setType(CardBodyFieldType.GENERAL)
                                .build()
                );

        CardAction.Builder approveActionBuilder = getApproveActionBuilder(expenseReportId, routingPrefix);
        CardAction.Builder rejectActionBuilder = getRejectActionBuilder(expenseReportId, routingPrefix);
        CardAction.Builder openActionBuilder = getOpenActionBuilder(baseUrl);

        return new Card.Builder()
                .setName("Concur")
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(cardTextAccessor.getMessage("concur.title"), null)
                .setBody(cardBodyBuilder.build())
                .addAction(approveActionBuilder.build())
                .addAction(rejectActionBuilder.build())
                .addAction(openActionBuilder.build())
                .build();
    }

    private CardAction.Builder getApproveActionBuilder(final String expenseReportId, final String routingPrefix) {
        final String approveUrl = "api/expense/approve/" + expenseReportId;

        // Approver has to enter the comment to approve the expense request.
        return new CardAction.Builder()
                .setLabel(this.cardTextAccessor.getMessage("concur.approve"))
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
