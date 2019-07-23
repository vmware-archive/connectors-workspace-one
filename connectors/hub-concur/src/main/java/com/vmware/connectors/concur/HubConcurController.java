/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.web.UserException;
import com.vmware.connectors.concur.domain.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.*;

@RestController
public class HubConcurController {

    private static final Logger logger = LoggerFactory.getLogger(HubConcurController.class);

    private static final String X_BASE_URL_HEADER = "X-Connector-Base-Url";

    private static final String COMMENT_KEY = "comment";
    private static final String REASON_KEY = "reason";

    private static final String APPROVE = "APPROVE";
    private static final String REJECT = "Send Back to Employee";

    private static final String CONNECTOR_AUTH = "X-Connector-Authorization";

    private static final String ATTACHMENT_URL = "%sapi/expense/report/%s/attachment";

    private final WebClient rest;
    private final CardTextAccessor cardTextAccessor;
    private final Resource concurRequestTemplate;
    private final String serviceAccountAuthHeader;

    @Autowired
    public HubConcurController(
            WebClient rest,
            CardTextAccessor cardTextAccessor,
            @Value("classpath:static/templates/concur-request-template.xml") Resource concurRequestTemplate,
            @Value("${concur.service-account-auth-header:}") String serviceAccountAuthHeader
    ) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.concurRequestTemplate = concurRequestTemplate;
        this.serviceAccountAuthHeader = serviceAccountAuthHeader;
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
            @RequestHeader(name = CONNECTOR_AUTH, required = false) String connectorAuth,
            Locale locale
    ) {
        String userEmail = AuthUtil.extractUserEmail(authorization);
        logger.debug("getCards called: baseUrl={}, routingPrefix={}, userEmail={}", baseUrl, routingPrefix, userEmail);

        if (isServiceAccountCredentialEmpty(connectorAuth)) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return fetchCards(baseUrl, locale, routingPrefix, userEmail, getAuthHeader(connectorAuth))
                .map(ResponseEntity::ok);
    }

    private boolean isServiceAccountCredentialEmpty(final String connectorAuth) {
        if (StringUtils.isBlank(this.serviceAccountAuthHeader) && StringUtils.isBlank(connectorAuth)) {
            logger.debug("X-Connector-Authorization should not be empty if service credentials are not present in the config file");
            return true;
        } else {
            return false;
        }
    }

    private String getAuthHeader(final String connectorAuth) {
        if (StringUtils.isBlank(this.serviceAccountAuthHeader)) {
            return connectorAuth;
        } else {
            return this.serviceAccountAuthHeader;
        }
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
                .map(report -> makeCards(routingPrefix, locale, report))
                .reduce(new Cards(), this::addCard);
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
            String routingPrefix,
            Locale locale,
            ExpenseReportResponse report
    ) {
        String reportId = report.getReportID();
        String reportName = report.getReportName();

        logger.trace("makeCard called: routingPrefix={}, reportId={}, reportName={}", routingPrefix, reportId, reportName);

        Card.Builder builder = new Card.Builder()
                .setName("Concur")
                .setHeader(cardTextAccessor.getMessage("hub.concur.header", locale, reportName))
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
            buildExpenseItemsList(report, locale, routingPrefix).forEach(cardBodyBuilder::addField);
        }
        return cardBodyBuilder.build();
    }

    private List<CardBodyField> buildExpenseItemsList(final ExpenseReportResponse report,
                                                      final Locale locale,
                                                      final String routingPrefix) {
        AtomicBoolean isURLAdded = new AtomicBoolean(false);

        List<CardBodyField> cardBodyFields = new ArrayList<>();
        for (ExpenseEntriesVO entry: report.getExpenseEntriesList()) {
            CardBodyField cardBodyField = new CardBodyField.Builder()
                    .setType(CardBodyFieldType.SECTION)
                    .setTitle(cardTextAccessor.getMessage("hub.concur.business.purpose", locale, entry.getBusinessPurpose()))
                    .addItems(buildItems(locale, entry, isURLAdded, routingPrefix, report.getReportID()))
                    .build();
            cardBodyFields.add(cardBodyField);
        }
        return cardBodyFields;
    }

    private List<CardBodyFieldItem> buildItems(final Locale locale,
                                               final ExpenseEntriesVO expenseEntry,
                                               final AtomicBoolean isURLAdded,
                                               final String routingPrefix,
                                               final String reportID) {
        final List<CardBodyFieldItem> items = new ArrayList<>();
        items.add(makeCardBodyFieldItem(cardTextAccessor.getMessage("hub.concur.expense.type.name", locale), expenseEntry.getExpenseTypeName()));
        items.add(makeCardBodyFieldItem(cardTextAccessor.getMessage("hub.concur.transaction.date", locale),  expenseEntry.getTransactionDate()));
        items.add(makeCardBodyFieldItem(cardTextAccessor.getMessage("hub.concur.vendor.name", locale), expenseEntry.getVendorDescription()));
        items.add(makeCardBodyFieldItem(cardTextAccessor.getMessage("hub.concur.city.of.purchase", locale), expenseEntry.getLocationName()));
        items.add(makeCardBodyFieldItem(cardTextAccessor.getMessage("hub.concur.payment.type", locale), expenseEntry.getPaymentTypeCode()));
        items.add(makeCardBodyFieldItem(cardTextAccessor.getMessage("hub.concur.amount", locale),
                formatCurrency(expenseEntry.getPostedAmount(), locale, expenseEntry.getTransactionCurrencyName())));

        final List<String> attendeesList = expenseEntry.getAttendeesList();
        if (!CollectionUtils.isEmpty(attendeesList)) {
            items.add(makeCardBodyFieldItem(cardTextAccessor.getMessage("hub.concur.attendees", locale), attendeesList.toString()));
        }

        if (!isURLAdded.get()) {
            items.add(buildAttachmentURL(routingPrefix, reportID, locale, isURLAdded));
        }
        return items;
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

    private CardBodyFieldItem buildAttachmentURL(final String routingPrefix,
                                                 final String reportID,
                                                 final Locale locale,
                                                 final AtomicBoolean isURLAdded) {
        isURLAdded.set(true);

        return new CardBodyFieldItem.Builder()
                .setTitle(cardTextAccessor.getMessage("hub.concur.report.image.url", locale))
                .setActionType(HttpMethod.GET)
                .setActionURL(String.format(ATTACHMENT_URL, routingPrefix, reportID))
                .setType(CardBodyFieldType.ATTACHMENT_URL)
                .setContentType(APPLICATION_PDF)
                .build();
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
    public Mono<ResponseEntity<String>> approveRequest(
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(name = CONNECTOR_AUTH, required = false) String connectorAuth,
            @PathVariable("id") String id,
            @Valid CommentForm form
            ) {
        logger.debug("approveRequest called: baseUrl={},  id={}, comment={}", baseUrl, id, form.getComment());

        if (isServiceAccountCredentialEmpty(connectorAuth)) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        String userEmail = AuthUtil.extractUserEmail(authorization);
        return makeConcurRequest(form.getComment(), baseUrl, APPROVE, id, userEmail, getAuthHeader(connectorAuth))
                .map(ResponseEntity::ok);
    }

    private Mono<String> makeConcurRequest(
            String reason,
            String baseUrl,
            String action,
            String reportId,
            String userEmail,
            String connectorAuth
    ) {
        String concurRequestTemplate = getConcurRequestTemplate(reason, action);

        return fetchLoginIdFromUserEmail(userEmail, baseUrl, connectorAuth)
                .flatMapMany(loginId -> validateUser(baseUrl, reportId, loginId, connectorAuth))
                .flatMap(ignored -> fetchRequestData(baseUrl, reportId, connectorAuth))
                .map(ExpenseReportResponse::getWorkflowActionURL)
                .flatMap(
                        url ->
                                rest.post()
                                        .uri(url)
                                        .header(AUTHORIZATION, connectorAuth)
                                        .contentType(APPLICATION_XML)
                                        .accept(APPLICATION_JSON)
                                        .syncBody(concurRequestTemplate)
                                        .retrieve()
                                        .bodyToMono(String.class)
                )
                .next();
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

    private Mono<?> validateUser(
            String baseUrl,
            String reportId,
            String userEmail,
            String connectorAuth
    ) {
        return fetchAllApprovals(baseUrl, userEmail, connectorAuth)
                .filter(expense -> expense.getId().equals(reportId))
                .filter(expense -> expense.getApproverLoginID().equals(userEmail))
                .next()
                .switchIfEmpty(Mono.error(new UserException("Not Found"))); // CustomException
    }

    @PostMapping(
            path = "/api/expense/{id}/decline",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<String>> declineRequest(
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(name = CONNECTOR_AUTH, required = false) String connectorAuth,
            @PathVariable("id") String id,
            @Valid DeclineForm form
    ) {
        logger.debug("declineRequest called: baseUrl={}, id={}, reason={}", baseUrl, id, form.getReason());

        if (isServiceAccountCredentialEmpty(connectorAuth)) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        String userEmail = AuthUtil.extractUserEmail(authorization);
        return makeConcurRequest(form.getReason(), baseUrl, REJECT, id, userEmail, getAuthHeader(connectorAuth))
                .map(ResponseEntity::ok);
    }

    @GetMapping(
            path = "api/expense/report/{id}/attachment",
            produces = APPLICATION_OCTET_STREAM_VALUE
    )
    public Flux<DataBuffer> fetchAttachment(
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(CONNECTOR_AUTH) String connectorAuth,
            @PathVariable("id") String reportId
    ) {
        final String userEmail = AuthUtil.extractUserEmail(authorization);
        logger.debug("fetchAttachment called: baseUrl={}, userEmail={}, reportId={}", baseUrl, userEmail, reportId);

        return fetchRequestData(baseUrl, reportId, "Bearer " + connectorAuth)
                .map(ExpenseReportResponse::getReportImageURL)
                .flatMapMany(reportImageUrl -> getAttachment(reportImageUrl, connectorAuth));
    }

    private Flux<DataBuffer> getAttachment(String reportImageUrl, String connectorAuth) {
        return this.rest.get()
                .uri(reportImageUrl)
                .header(AUTHORIZATION, "Bearer " + connectorAuth)
                .retrieve()
                .bodyToFlux(DataBuffer.class);
    }
}
