/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.CommonUtils;
import com.vmware.connectors.common.utils.Reactive;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.vmware.connectors.common.utils.CommonUtils.APPROVAL_ACTIONS;
import static com.vmware.connectors.concur.ConcurConstants.ConcurRequestActions.*;
import static com.vmware.connectors.concur.ConcurConstants.ConcurResponseActions.SUBMITTED_AND_PENDING_APPROVAL;
import static com.vmware.connectors.concur.ConcurConstants.Fields.CONCUR_AUTOMATED_EMAIL_SUBJECT;
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

    private final String clientId;
    private final String clientSecret;
    private final String oauthTokenUrl;

    // Client credentials token fields.
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String CRED_TYPE = "credtype";
    private static final String GRANT_TYPE = "grant_type";
    private static final String BEARER = "Bearer ";

    @Autowired
    public ConcurController(WebClient rest,
                            CardTextAccessor cardTextAccessor,
                            @Value("${concur.client-id}") final String clientId,
                            @Value("${concur.client-secret}") final String clientSecret,
                            @Value("${concur.oauth-instance-url}") final String oauthTokenUrl,
                            @Value("classpath:static/templates/concur-request-template.xml") Resource concurRequestTemplate) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.oauthTokenUrl = oauthTokenUrl;
        this.concurrRequestTemplate = concurRequestTemplate;
    }

    @GetMapping("/test-auth")
    public Mono<ResponseEntity<Void>> verifyAuth(@RequestHeader(name = AUTHORIZATION_HEADER) final String authHeader) {
        return getAuthHeader(authHeader, clientId, clientSecret)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PostMapping(path = "/cards/requests",
            produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE)
    public Mono<Cards> getCards(
            @RequestHeader(name = AUTHORIZATION_HEADER) final String authHeader,
            @RequestHeader(name = BACKEND_BASE_URL_HEADER) final String baseUrl,
            @RequestHeader(name = ROUTING_PREFIX) final String routingPrefix,
            final Locale locale,
            @Valid @RequestBody CardRequest cardRequest,
            ServerHttpRequest request) {

        logger.debug("getCards called: baseUrl={}, cardRequest={}", baseUrl, cardRequest);

        final String automatedEmailSubject = cardRequest.getTokenSingleValue(CONCUR_AUTOMATED_EMAIL_SUBJECT);
        if (StringUtils.isBlank(automatedEmailSubject)) {
            return Mono.just(new Cards());
        }

        final Set<String> expenseReportIds = cardRequest.getTokens(EXPENSE_REPORT_ID);
        if (CollectionUtils.isEmpty(expenseReportIds)) {
            return Mono.just(new Cards());
        }

        return getAuthHeader(authHeader, clientId, clientSecret)
                .flatMap(oauthHeader -> fetchCards(baseUrl, routingPrefix, locale,
                        request, expenseReportIds, oauthHeader));
    }

    private Mono<Cards> fetchCards(final String baseUrl,
                                   final String routingPrefix,
                                   final Locale locale,
                                   final ServerHttpRequest request,
                                   final Set<String> expenseReportIds,
                                   final String oauthHeader) {
        logger.debug("fetchCards called: baseUrl={}, expenseReportIds={}", baseUrl, expenseReportIds);

        return Flux.fromIterable(expenseReportIds)
                .flatMap(expenseReportId -> getCardForExpenseReport(expenseReportId, baseUrl, routingPrefix,
                        locale, request, oauthHeader))
                .collect(Cards::new, (cards, card) -> cards.getCards().add(card))
                .defaultIfEmpty(new Cards());
    }

    private Mono<String> getAuthHeader(final String authHeader,
                                       final String clientId,
                                       final String clientSecret) {
        logger.debug("getAuthHeader called: clientId={}", clientId);

        final String authValue = authHeader.substring("Basic".length()).trim();
        final byte[] decodedAuthValue = Base64.getDecoder().decode(authValue);
        final String decodedValue = new String(decodedAuthValue, StandardCharsets.UTF_8);
        final String userName = decodedValue.split(":")[0];
        final String password = decodedValue.split(":")[1];

        final MultiValueMap<String, String> body = getBody(clientId, clientSecret, userName, password);

        return rest.post()
                .uri(UriComponentsBuilder.fromUriString(oauthTokenUrl).path("/oauth2/v0/token").toUriString())
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(body))
                .retrieve()
                .bodyToMono(JsonDocument.class)
                .map(jsonDocument -> BEARER + jsonDocument.read("$.access_token"))
                .onErrorMap(WebClientResponseException.class, e -> handleForbiddenError(e));
    }

    private Throwable handleForbiddenError(WebClientResponseException e) {
        // Concur gives a 403 instead of a 401 when the password is bad, so we must convert it
        if (HttpStatus.FORBIDDEN.equals(e.getStatusCode())) {
            return new WebClientResponseException(
                    e.getMessage(),
                    HttpStatus.UNAUTHORIZED.value(),
                    e.getStatusText(),
                    e.getHeaders(),
                    e.getResponseBodyAsByteArray(),
                    StandardCharsets.UTF_8
            );
        }
        return e;
    }

    private MultiValueMap<String, String> getBody(String clientId, String clientSecret, String userName, String password) {
        final MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.put(CLIENT_ID, List.of(clientId));
        body.put(CLIENT_SECRET, List.of(clientSecret));
        body.put(USERNAME, List.of(userName));
        body.put(PASSWORD, List.of(password));
        body.put(GRANT_TYPE, List.of(PASSWORD));
        body.put(CRED_TYPE, List.of(PASSWORD));
        return body;
    }

    @PostMapping(path = "/api/expense/approve/{expenseReportId}",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public Mono<String> approveRequest(
            @RequestHeader(name = AUTHORIZATION_HEADER) final String authHeader,
            @RequestHeader(name = BACKEND_BASE_URL_HEADER) final String baseUrl,
            @Valid ActionForm form,
            @PathVariable(name = ConcurConstants.PathVariable.EXPENSE_REPORT_ID) final String workflowstepId) throws IOException, ExecutionException, InterruptedException {
        logger.debug("Approving the concur expense for the base concur URL: {} and expense report with ID: {}", baseUrl, workflowstepId);

        return makeConcurActionRequest(baseUrl, form.getReason(), workflowstepId, authHeader, APPROVE);
    }

    @PostMapping(path = "/api/expense/reject/{expenseReportId}",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE)
    public Mono<String> rejectRequest(
            @RequestHeader(name = AUTHORIZATION_HEADER) final String authHeader,
            @RequestHeader(name = BACKEND_BASE_URL_HEADER) final String baseUrl,
            @Valid ActionForm form,
            @PathVariable(name = ConcurConstants.PathVariable.EXPENSE_REPORT_ID) final String workflowstepId) throws IOException, ExecutionException, InterruptedException {
        logger.debug("Rejecting the concur expense for the base concur URL: {} and expense report with ID: {}", baseUrl, workflowstepId);

        return makeConcurActionRequest(baseUrl, form.getReason(), workflowstepId, authHeader, REJECT);
    }

    private Mono<String> makeConcurActionRequest(final String baseUrl,
                                                 final String reason,
                                                 final String reportID,
                                                 final String authHeader,
                                                 final String concurAction) throws IOException {
        // Replace the placeholder in concur request template with appropriate action and comment.
        final String concurRequestTemplate = getConcurRequestTemplate(reason, concurAction);

        return getAuthHeader(authHeader, clientId, clientSecret)
                .flatMap(oauthHeader -> getWorkFlowActionUrl(oauthHeader, reportID, baseUrl)
                        .flatMap(url -> rest.post()
                                .uri(url)
                                .header(AUTHORIZATION, oauthHeader)
                                .contentType(APPLICATION_XML)
                                .accept(APPLICATION_JSON)
                                .syncBody(concurRequestTemplate)
                                .retrieve()
                                .bodyToMono(String.class)));
    }

    private String getConcurRequestTemplate(final String reason,
                                            final String concurAction) throws IOException {
        String concurRequestTemplate = IOUtils.toString(this.concurrRequestTemplate.getInputStream(), StandardCharsets.UTF_8);
        concurRequestTemplate = concurRequestTemplate.replace(ACTION_PLACEHOLDER, concurAction);
        concurRequestTemplate = concurRequestTemplate.replace(COMMENT_PLACEHOLDER, HtmlUtils.htmlEscape(reason));
        return concurRequestTemplate;
    }

    private Mono<Card> getCardForExpenseReport(final String id,
                                               final String baseUrl,
                                               final String routingPrefix,
                                               final Locale locale,
                                               final ServerHttpRequest request,
                                               final String oauthHeader) {
        logger.debug("Requesting expense request info from concur base URL: {} for ticket request id: {}", baseUrl, id);

        return getReportDetails(oauthHeader, id, baseUrl)
                .onErrorResume(throwable -> Reactive.skipOnStatus(throwable, HttpStatus.BAD_REQUEST))
                .map(entity -> convertResponseIntoCard(entity, id, routingPrefix, locale, request));
    }

    private Mono<ResponseEntity<JsonDocument>> getReportDetails(String oauthHeader, String id, String baseUrl) {
        logger.debug("getReportDetails called: baseUrl={}, id={}", baseUrl, id);

        return rest.get()
                .uri(baseUrl + "/api/expense/expensereport/v2.0/report/{id}", id)
                .header(AUTHORIZATION, oauthHeader)
                .accept(APPLICATION_JSON)
                .exchange()
                .flatMap(Reactive::checkStatus)
                .flatMap(response -> response.toEntity(JsonDocument.class));
    }

    private Mono<String> getWorkFlowActionUrl(final String authHeader,
                                              final String id,
                                              final String baseUrl) {
        return getReportDetails(authHeader, id, baseUrl)
                .map(ResponseEntity::getBody)
                .map(jsonDocument -> jsonDocument.read("$.WorkflowActionURL"));
    }

    private Card convertResponseIntoCard(final ResponseEntity<JsonDocument> entity,
                                         final String expenseReportId,
                                         final String routingPrefix,
                                         final Locale locale,
                                         final ServerHttpRequest request) {
        logger.debug("convertResponseIntoCard called: expenseReportId={}", expenseReportId);

        final JsonDocument response = entity.getBody();
        final String approvalStatus = response.read("$.ApprovalStatusName");

        final Card.Builder cardBuilder = new Card.Builder()
                .setName("Concur")
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(cardTextAccessor.getMessage("concur.title", locale))
                .setBody(buildCardBodyBuilder(response, locale));

        // Set image url.
        CommonUtils.buildConnectorImageUrl(cardBuilder, request);

        // Add approve and reject actions only if the approval status is submitted and pending approval.
        if (SUBMITTED_AND_PENDING_APPROVAL.equalsIgnoreCase(approvalStatus)) {
            CardAction.Builder approveActionBuilder = getApproveActionBuilder(expenseReportId, routingPrefix, locale);
            CardAction.Builder rejectActionBuilder = getRejectActionBuilder(expenseReportId, routingPrefix, locale);

            cardBuilder.addAction(approveActionBuilder.build());
            cardBuilder.addAction(rejectActionBuilder.build());
        }

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

        // User has to enter the comment to approve the expense request.
        return new CardAction.Builder()
                .setLabel(this.cardTextAccessor.getActionLabel("concur.approve", locale))
                .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel("concur.approve", locale))
                .setActionKey(CardActionKey.USER_INPUT)
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + approveUrl)
                .setPrimary(true)
                .setMutuallyExclusiveSetId(APPROVAL_ACTIONS)
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setId(REASON)
                                .setFormat("textarea")
                                .setLabel(cardTextAccessor.getMessage("concur.approve.reason.label", locale))
                                .setMinLength(1)
                                .build()
                );
    }

    private CardAction.Builder getRejectActionBuilder(
            final String expenseReportId, final String routingPrefix, final Locale locale) {
        final String rejectUrl = "api/expense/reject/" + expenseReportId;

        // User has to enter the comment to reject the expense request.
        return new CardAction.Builder()
                .setLabel(this.cardTextAccessor.getActionLabel("concur.reject", locale))
                .setCompletedLabel(this.cardTextAccessor.getActionCompletedLabel("concur.reject", locale))
                .setActionKey(CardActionKey.USER_INPUT)
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + rejectUrl)
                .setMutuallyExclusiveSetId(APPROVAL_ACTIONS)
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setId(REASON)
                                .setFormat("textarea")
                                .setLabel(this.cardTextAccessor.getMessage("concur.reject.reason.label", locale))
                                .setMinLength(1)
                                .build()
                );
    }
}
