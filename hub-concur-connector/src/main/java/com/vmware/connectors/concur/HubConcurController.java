/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.nimbusds.jose.util.StandardCharset;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.concur.domain.*;
import com.vmware.connectors.concur.exception.AttachmentURLNotFoundException;
import com.vmware.connectors.concur.exception.ExpenseReportNotFoundException;
import com.vmware.connectors.concur.exception.UserNotFoundException;
import com.vmware.connectors.concur.exception.WorkFlowActionFailureException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.vmware.connectors.common.utils.CommonUtils.BACKEND_STATUS;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

@RestController
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class HubConcurController {

    private static final Logger logger = LoggerFactory.getLogger(HubConcurController.class);
    private static final String X_BASE_URL_HEADER = "X-Connector-Base-Url";
    private static final String COMMENT_KEY = "comment";
    private static final String REASON_KEY = "reason";
    private static final String APPROVE = "APPROVE";
    private static final String REJECT = "Send Back to Employee";
    private static final String WORKFLOW_ACTION_FAILURE_STATUS = "FAILURE";
    private static final String CONNECTOR_AUTH = "X-Connector-Authorization";
    private static final String CONTENT_DISPOSITION_FORMAT = "attachment; filename=\"%s.pdf\"";

    private final WebClient rest;
    private final CardTextAccessor cardTextAccessor;
    private final Resource concurRequestTemplate;

    @Autowired
    public HubConcurController(
            WebClient rest,
            CardTextAccessor cardTextAccessor,
            @Value("classpath:static/templates/concur-request-template.xml") Resource concurRequestTemplate
    ) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.concurRequestTemplate = concurRequestTemplate;
    }

    @PostMapping(
            path = "/cards/requests",
            produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Cards>> getCards(
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @RequestHeader("X-Routing-Prefix") String routingPrefix,
            @RequestHeader(name = CONNECTOR_AUTH) String connectorAuth,
            Locale locale
    ) {
        String userEmail = AuthUtil.extractUserEmail(authorization);
        logger.debug("getCards called: baseUrl={}, routingPrefix={}, userEmail={}", baseUrl, routingPrefix, userEmail);

        return fetchCards(baseUrl, locale, routingPrefix, userEmail, connectorAuth)
                .map(ResponseEntity::ok);
    }

    private Mono<Cards> fetchCards(
            String baseUrl,
            Locale locale,
            String routingPrefix,
            String userEmail,
            String connectorAuth
    ) {
        logger.debug("fetchCards called: baseUrl={}, routingPrefix={}, userEmail={}", baseUrl, routingPrefix, userEmail);

        return fetchLoginIdFromUserEmail(userEmail, baseUrl, connectorAuth)
                .flatMapMany(loginId -> fetchAllApprovals(baseUrl, loginId, connectorAuth))
                .flatMap(expense -> fetchRequestData(baseUrl, expense.getId(), connectorAuth))
                .map(report -> makeCards(baseUrl, routingPrefix, locale, report))
                .reduce(new Cards(), this::addCard)
                .defaultIfEmpty(new Cards());
    }

    private Mono<String> fetchLoginIdFromUserEmail(
            String userEmail,
            String baseUrl,
            String connectorAuth
    ) {
        return rest.get()
                .uri(baseUrl + "/api/v3.0/common/users?primaryEmail={userEmail}", userEmail)
                .header(AUTHORIZATION, connectorAuth)
                .accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono(UserDetailsResponse.class)
                .flatMapMany(userDetails -> Flux.fromIterable(userDetails.getItems()))
                .next()
                .map(UserDetailsVO::getLoginId);
    }

    private Flux<PendingApprovalsVO> fetchAllApprovals(
            String baseUrl,
            String userEmail,
            String connectorAuth
    ) {
        int limit = 50;
        String userFilter = "all";
        return rest.get()
                .uri(baseUrl + "/api/v3.0/expense/reportdigests?approverLoginID={userEmail}&limit={limit}&user={userFilter}",
                        userEmail, limit, userFilter)
                .header(AUTHORIZATION, connectorAuth)
                .accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono(PendingApprovalResponse.class)
                .flatMapMany(expenses -> Flux.fromIterable(expenses.getPendingApprovals()));
    }

    private Mono<ExpenseReportResponse> fetchRequestData(
            String baseUrl,
            String reportId,
            String connectorAuth
    ) {
        logger.trace("fetchRequestData called: baseUrl={}, reportId={}", baseUrl, reportId);

        return rest.get()
                .uri(baseUrl + "/api/expense/expensereport/v2.0/report/{reportId}", reportId)
                .header(AUTHORIZATION, connectorAuth)
                .accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono(ExpenseReportResponse.class);
    }

    private Card makeCards(
            String baseUrl,
            String routingPrefix,
            Locale locale,
            ExpenseReportResponse report
    ) {
        String reportId = report.getReportID();
        String reportName = report.getReportName();

        logger.trace("makeCard called: routingPrefix={}, reportId={}, reportName={}", routingPrefix, reportId, reportName);

        Card.Builder builder = new Card.Builder()
                .setName("Concur")
                .setHeader(
                        new CardHeader(
                                cardTextAccessor.getMessage("hub.concur.header", locale, reportName),
                                null,
                                new CardHeaderLinks(
                                        UriComponentsBuilder
                                                .fromUriString(baseUrl)
                                                .path("/approvalsportal.asp")
                                                .toUriString(),
                                        null
                                )
                        )
                )
                .setBody(buildCard(locale, report, routingPrefix))
                .setBackendId(report.getReportID())
                .addAction(makeAction(routingPrefix, locale, reportId,
                        true, "hub.concur.approve", COMMENT_KEY, "hub.concur.approve.comment.label", "/approve"))
                .addAction(makeAction(routingPrefix, locale, reportId,
                        false, "hub.concur.decline", REASON_KEY, "hub.concur.decline.reason.label", "/decline"));

        builder.setImageUrl("https://s3.amazonaws.com/vmw-mf-assets/connector-images/hub-concur.png");

        return builder.build();
    }

    private CardBody buildCard(final Locale locale,
                               final ExpenseReportResponse report,
                               final String routingPrefix) {
        final CardBody.Builder cardBodyBuilder =  new CardBody.Builder()
                .addField(makeGeneralField(locale, "hub.concur.report.name", report.getReportName()))
                .addField(makeGeneralField(locale, "hub.concur.requester", report.getEmployeeName()))
                .addField(makeGeneralField(locale, "hub.concur.expenseAmount",
                        formatCurrency(report.getReportTotal(), locale, report.getCurrencyCode())));

        if (!CollectionUtils.isEmpty(report.getExpenseEntriesList())) {
            buildExpenseItems(report, locale).forEach(cardBodyBuilder::addField);

            // Add expense report attachment URL.
            if (StringUtils.isNotBlank(report.getReportImageURL())) {
                cardBodyBuilder.addField(buildAttachmentURL(routingPrefix, report.getReportID(), locale));
            }
        }
        return cardBodyBuilder.build();
    }

    private List<CardBodyField> buildExpenseItems(final ExpenseReportResponse report, final Locale locale) {
        return report.getExpenseEntriesList()
                .stream()
                .map(entry -> new CardBodyField.Builder()
                        .setType(CardBodyFieldType.SECTION)
                        .setTitle(cardTextAccessor.getMessage("hub.concur.business.purpose", locale, entry.getBusinessPurpose()))
                        .addItems(buildItems(locale, entry, report.getCurrencyCode()))
                        .build()
                )
                .collect(Collectors.toList());
    }

    private List<CardBodyFieldItem> buildItems(Locale locale,
                                               ExpenseEntriesVO expenseEntry,
                                               String creatorReimbursementCurrency) {
        final List<CardBodyFieldItem> items = new ArrayList<>();

        addItem("hub.concur.expense.type.name", expenseEntry.getExpenseTypeName(), locale, items);
        addItem("hub.concur.transaction.date", expenseEntry.getTransactionDate(), locale, items);
        addItem("hub.concur.vendor.name", expenseEntry.getVendorDescription(), locale, items);
        addItem("hub.concur.city.of.purchase", expenseEntry.getLocationName(), locale, items);
        addItem("hub.concur.payment.type", expenseEntry.getPaymentTypeCode(), locale, items);
        addItem("hub.concur.amount",
                formatCurrency(expenseEntry.getPostedAmount(), locale, creatorReimbursementCurrency),
                locale,
                items);

        final List<String> attendeesList = expenseEntry.getAttendeesList();
        if (!CollectionUtils.isEmpty(attendeesList)) {
            items.add(makeCardBodyFieldItem(cardTextAccessor.getMessage("hub.concur.attendees", locale), attendeesList.toString()));
        }

        return items;
    }

    private void addItem(final String title,
                         final String description,
                         final Locale locale,
                         final List<CardBodyFieldItem> items) {
        if (StringUtils.isBlank(description)) {
            return;
        }

        items.add(makeCardBodyFieldItem(cardTextAccessor.getMessage(title, locale), description));
    }

    private CardBodyFieldItem makeCardBodyFieldItem(final String title, final String description) {
        return new CardBodyFieldItem.Builder()
                .setType(CardBodyFieldType.GENERAL)
                .setTitle(title)
                .setDescription(description)
                .build();
    }

    private CardBodyField makeGeneralField(
            Locale locale,
            String labelKey,
            String value
    ) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        return new CardBodyField.Builder()
                .setType(CardBodyFieldType.GENERAL)
                .setTitle(cardTextAccessor.getMessage(labelKey, locale))
                .setDescription(value)
                .build();
    }

    private String formatCurrency(
            String amount,
            Locale locale,
            String currencyCode
    ) {
        return String.format(
                "%s %s",
                currencyCode,
                NumberFormat.getNumberInstance(locale ==  null? Locale.getDefault() : locale).format(Double.parseDouble(amount))
        );
    }

    private CardAction makeAction(
            String routingPrefix,
            Locale locale,
            String reportId,
            boolean primary,
            String buttonLabelKey,
            String textFieldId,
            String textFieldLabelKey,
            String apiPath
    ) {
        return new CardAction.Builder()
                .setActionKey(CardActionKey.USER_INPUT)
                .setLabel(cardTextAccessor.getActionLabel(buttonLabelKey, locale))
                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel(buttonLabelKey, locale))
                .setPrimary(primary)
                .setMutuallyExclusiveSetId("approval-actions")
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + "api/expense/" + reportId + apiPath)
                .addUserInputField(
                        new CardActionInputField.Builder().setFormat("textarea")
                                .setId(textFieldId)
                                .setLabel(cardTextAccessor.getMessage(textFieldLabelKey, locale))
                                .build()
                )
                .build();
    }

    private CardBodyField buildAttachmentURL(final String routingPrefix,
                                             final String reportID,
                                             final Locale locale) {
        CardBodyField.Builder builder = new CardBodyField.Builder()
                .setTitle(cardTextAccessor.getMessage("hub.concur.attachment", locale))
                .setType(CardBodyFieldType.SECTION)
                .addItem(new CardBodyFieldItem.Builder()
                        .setAttachmentName(cardTextAccessor.getMessage("hub.concur.attachmentName", locale))
                        .setTitle(cardTextAccessor.getMessage("hub.concur.report.image.url", locale))
                        .setAttachmentMethod(HttpMethod.GET)

                        .setAttachmentUrl(getAttachmentUrl(routingPrefix, reportID))
                        .setType(CardBodyFieldType.ATTACHMENT_URL)
                        .setAttachmentContentType(APPLICATION_PDF_VALUE) // Concur always returns a PDF file. It consolidates all the attachments into a single PDF file.
                        .build());

        return builder.build();
    }

    private String getAttachmentUrl(String routingPrefix, String reportID) {
        return UriComponentsBuilder.fromUriString(routingPrefix).path("/api/expense/report/{report_id}/attachment")
                .buildAndExpand(
                        Map.of(
                                "report_id", reportID
                        )
                ).toUriString();
    }

    private Cards addCard(
            Cards cards,
            Card card
    ) {
        cards.getCards().add(card);
        return cards;
    }

    @PostMapping(
            path = "/api/expense/{id}/approve",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Void>> approveRequest(
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(name = CONNECTOR_AUTH) String connectorAuth,
            @PathVariable("id") String id,
            @Valid CommentForm form,
            Locale locale
    ) {
        String userEmail = AuthUtil.extractUserEmail(authorization);
        logger.debug("approveRequest called: baseUrl={}, id={}, email={} comment={}", baseUrl, id, userEmail, form.getComment());

        return doWorkFlowAction(form.getComment(), baseUrl, APPROVE, id, userEmail, connectorAuth, locale)
                        .map(ResponseEntity::ok);
    }

    private Mono<Void> doWorkFlowAction(
            String reason,
            String baseUrl,
            String action,
            String reportId,
            String userEmail,
            String connectorAuth,
            Locale locale
    ) {
        String concurRequestTemplate = getConcurRequestTemplate(reason, action);

        return fetchLoginIdFromUserEmail(userEmail, baseUrl, connectorAuth)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User with email " + userEmail + " is not found.")))
                .flatMapMany(loginId -> validateReportAgainstCallingUser(baseUrl, reportId, loginId, connectorAuth, locale))
                .flatMap(ignored -> fetchRequestData(baseUrl, reportId, connectorAuth))
                .map(ExpenseReportResponse::getWorkflowActionURL)
                .flatMap(
                        url ->
                                rest.post()
                                        .uri(url)
                                        .header(AUTHORIZATION, connectorAuth)
                                        .contentType(APPLICATION_XML)
                                        .accept(APPLICATION_JSON)
                                        .bodyValue(concurRequestTemplate)
                                        .retrieve()
                                        .bodyToMono(JsonDocument.class))
                .doOnNext(response -> this.validateWorkflowResponseStatus(response, userEmail, baseUrl, locale))
                .then();
    }

    private void validateWorkflowResponseStatus(JsonDocument response, String userEmail, String baseUrl, Locale locale) {
        if (WORKFLOW_ACTION_FAILURE_STATUS.equals(response.read("$.Status"))) {
            logger.debug("Action failure response from Concur: {}, for user: {}", response.toString(), userEmail);

            String expensePrefUrl = UriComponentsBuilder
                    .fromUriString(baseUrl)
                    .replacePath("/expense/profile/ExpensePreference.asp").build()
                    .toUriString();
            ActionFailureResponse.OpenInAction action = new ActionFailureResponse.OpenInAction.Builder()
                    .setLabel(cardTextAccessor.getMessage("hub.concur.failedAction.action.label", locale))
                    .setPrimary(true)
                    .setRemoveCardOnCompletion(false)
                    .setUrl(expensePrefUrl)
                    .build();

            ActionFailureResponse actionFailureResponse = new ActionFailureResponse.Builder()
                    .setTitle(cardTextAccessor.getMessage("hub.concur.failedAction.title", locale))
                    .setErrorMessage(cardTextAccessor.getMessage("hub.concur.failedAction.errorMessage", locale))
                    .addAction(action)
                    .build();

            throw new WorkFlowActionFailureException(
                    "Failed to execute workflow action for: " + userEmail + ", " + response.read("$.Message"), actionFailureResponse);
        }
    }

    private String getConcurRequestTemplate(
            String reason,
            String concurAction
    ) {
        try {
            return IOUtils.toString(concurRequestTemplate.getInputStream(), StandardCharsets.UTF_8)
                    .replace("${action}", concurAction)
                    .replace("${comment}", HtmlUtils.htmlEscape(reason));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read in concur request template!", e); // NOPMD
        }
    }

    /*
     * On actions or attachment retrieval we need to make sure that
     * report id is actually associated with the calling user.
     */
    private Mono<?> validateReportAgainstCallingUser(
            String baseUrl,
            String reportId,
            String loginID,
            String connectorAuth,
            Locale locale
    ) {
        return fetchAllApprovals(baseUrl, loginID, connectorAuth)
                .filter(expense -> expense.getId().equals(reportId))
                .filter(expense -> expense.getApproverLoginID().equals(loginID))
                .next()
                .switchIfEmpty(Mono.defer(() -> toReportNotFoundError(reportId, loginID, baseUrl, locale)));

    }

    private Mono<? extends PendingApprovalsVO> toReportNotFoundError(String reportId, String loginID, String baseUrl, Locale locale) {
        logger.debug("The report: {} is not found or is not associated with user: {}", reportId, loginID);

        String pendingApprovalsUrl = UriComponentsBuilder
                .fromUriString(baseUrl)
                .replacePath("/approvalsportal.asp").build()
                .toUriString();

        ActionFailureResponse.OpenInAction action = new ActionFailureResponse.OpenInAction.Builder()
                .setLabel(cardTextAccessor.getMessage("hub.concur.failedAction.reportNotFound.action.label", locale))
                .setPrimary(true)
                .setRemoveCardOnCompletion(true)
                .setUrl(pendingApprovalsUrl)
                .build();

        ActionFailureResponse actionFailureResponse = new ActionFailureResponse.Builder()
                .setTitle(cardTextAccessor.getMessage("hub.concur.failedAction.reportNotFound.title", locale))
                .setErrorMessage(cardTextAccessor.getMessage("hub.concur.failedAction.reportNotFound.errorMessage", locale))
                .addAction(action)
                .build();
        return Mono.error(new ExpenseReportNotFoundException("Report ID " + reportId + " is not found.", actionFailureResponse));
    }

    @PostMapping(
            path = "/api/expense/{id}/decline",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Void>> declineRequest(
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(name = CONNECTOR_AUTH) String connectorAuth,
            @PathVariable("id") String id,
            @Valid DeclineForm form,
            Locale locale
    ) {
        String userEmail = AuthUtil.extractUserEmail(authorization);
        logger.debug("declineRequest called: baseUrl={}, id={}, email={}, reason={}", baseUrl, id, userEmail, form.getReason());
        return doWorkFlowAction(form.getReason(), baseUrl, REJECT, id, userEmail, connectorAuth, locale)
                .map(ResponseEntity::ok);
    }

    @GetMapping(
            path = "api/expense/report/{id}/attachment"
    )
    public Mono<ResponseEntity<Flux<DataBuffer>>> fetchAttachment(
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(CONNECTOR_AUTH) String connectorAuth,
            @PathVariable("id") String reportId,
            Locale locale
    ) {

        final String userEmail = AuthUtil.extractUserEmail(authorization);
        logger.debug("fetchAttachment called: baseUrl={}, userEmail={}, reportId={}", baseUrl, userEmail, reportId);

        return  fetchLoginIdFromUserEmail(userEmail, baseUrl, connectorAuth)
                        .switchIfEmpty(Mono.error(new UserNotFoundException("User with email " + userEmail + " is not found.")))
                        .flatMap(loginID -> validateReportAgainstCallingUser(baseUrl, reportId, loginID, connectorAuth, locale))
                        .then(fetchRequestData(baseUrl, reportId, connectorAuth))
                        .flatMap(expenseReportResponse -> getAttachment(expenseReportResponse, connectorAuth))
                        .map(clientResponse -> handleClientResponse(clientResponse, locale));
    }

    private Mono<ClientResponse> getAttachment(ExpenseReportResponse report, String connectorAuth) {
        if (StringUtils.isBlank(report.getReportImageURL())) {
            throw new AttachmentURLNotFoundException("Concur expense report with ID " + report.getReportID() + " does not have any attachments.");
        }

        return this.rest.get()
                .uri(report.getReportImageURL())
                .header(AUTHORIZATION, connectorAuth)
                .exchange();
    }

    private ResponseEntity<Flux<DataBuffer>> handleClientResponse(final ClientResponse response, Locale locale) {
        if (response.statusCode().is2xxSuccessful()) {
            return ResponseEntity.ok()
                    .contentType(response.headers().contentType().orElse(APPLICATION_PDF))
                    .header(CONTENT_DISPOSITION,
                            String.format(CONTENT_DISPOSITION_FORMAT, cardTextAccessor.getMessage("hub.concur.attachmentName", locale)))
                    .body(response.bodyToFlux(DataBuffer.class));

        }

        return handleErrorStatus(response);
    }

    private ResponseEntity<Flux<DataBuffer>> handleErrorStatus(final ClientResponse response) {
        final HttpStatus status = response.statusCode();
        final String backendStatus = Integer.toString(response.rawStatusCode());

        logger.error("Concur backend returned the status code [{}] and reason phrase [{}] ", status, status.getReasonPhrase());

        if (status == UNAUTHORIZED) {
            String body = "{\"error\" : \"invalid_connector_token\"}";
            final DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(body.getBytes(StandardCharset.UTF_8));
            return ResponseEntity.status(BAD_REQUEST)
                    .header(BACKEND_STATUS, backendStatus)
                    .contentType(APPLICATION_JSON)
                    .body(Flux.just(dataBuffer));
        } else {
            final ResponseEntity.BodyBuilder builder = ResponseEntity.status(INTERNAL_SERVER_ERROR).header(BACKEND_STATUS, backendStatus);
            response.headers().contentType().ifPresent(builder::contentType);
            return builder.body(response.bodyToFlux(DataBuffer.class));
        }
    }

    @ExceptionHandler(AttachmentURLNotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    @ResponseBody
    public Map<String, String> handleAttachmentURLNotFoundException(AttachmentURLNotFoundException e) {
        return errorOf(e);
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    @ResponseBody
    public Map<String, String> handleUserNotFoundException(UserNotFoundException e) {
        return errorOf(e);
    }

    @ExceptionHandler(ExpenseReportNotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    @ResponseBody
    public ActionFailureResponse handleExpenseReportNotFoundException(ExpenseReportNotFoundException e) {
        logger.debug(e.getMessage());
        return e.getActionFailureResponse();
    }

    @ExceptionHandler(WorkFlowActionFailureException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public ActionFailureResponse handleWorkFlowActionFailureException(WorkFlowActionFailureException e) {
        logger.debug(e.getMessage());
        return e.getActionFailureResponse();
    }

    private Map<String, String> errorOf(Exception e) {
        return Map.of("error", e.getMessage() == null ? e.getClass().getName() : e.getMessage());
    }

}
