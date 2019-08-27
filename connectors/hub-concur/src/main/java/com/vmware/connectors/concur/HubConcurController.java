/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.nimbusds.jose.util.StandardCharset;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.web.UserException;
import com.vmware.connectors.concur.domain.*;
import com.vmware.connectors.concur.exception.AttachmentURLNotFoundException;
import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ClientResponse;
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
import java.util.Map;
import java.util.stream.Collectors;

import static com.vmware.connectors.common.utils.CommonUtils.BACKEND_STATUS;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpStatus.*;
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

    private static final String CONTENT_DISPOSITION_FORMAT = "Content-Disposition: inline; filename=\"%s.pdf\"";

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
        // TODO: APF-2324 - Modify this logic to extract the password grant credentials from service account creds.
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
                        .addItems(buildItems(locale, entry))
                        .build()
                )
                .collect(Collectors.toList());
    }

    private List<CardBodyFieldItem> buildItems(final Locale locale, final ExpenseEntriesVO expenseEntry) {
        final List<CardBodyFieldItem> items = new ArrayList<>();

        addItem("hub.concur.expense.type.name", expenseEntry.getExpenseTypeName(), locale, items);
        addItem("hub.concur.transaction.date", expenseEntry.getTransactionDate(), locale, items);
        addItem("hub.concur.vendor.name", expenseEntry.getVendorDescription(), locale, items);
        addItem("hub.concur.city.of.purchase", expenseEntry.getLocationName(), locale, items);
        addItem("hub.concur.payment.type", expenseEntry.getPaymentTypeCode(), locale, items);
        addItem("hub.concur.amount", formatCurrency(expenseEntry.getPostedAmount(),
                locale, expenseEntry.getTransactionCurrencyName()), locale, items);

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
                        .setAttachmentName(reportID)
                        .setTitle(cardTextAccessor.getMessage("hub.concur.report.image.url", locale))
                        .setActionType(HttpMethod.GET)
                        .setActionURL(String.format(ATTACHMENT_URL, routingPrefix, reportID))
                        .setType(CardBodyFieldType.ATTACHMENT_URL)
                        .setContentType(APPLICATION_PDF_VALUE) // Concur always returns a PDF file. It consolidates all the attachments into a single PDF file.
                        .build());

        return builder.build();
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
            String loginID,
            String connectorAuth
    ) {
        return fetchAllApprovals(baseUrl, loginID, connectorAuth)
                .filter(expense -> expense.getId().equals(reportId))
                .filter(expense -> expense.getApproverLoginID().equals(loginID))
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
            path = "api/expense/report/{id}/attachment"
    )
    public Mono<ResponseEntity<Flux<DataBuffer>>> fetchAttachment(
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(CONNECTOR_AUTH) String connectorAuth,
            @PathVariable("id") String reportId
    ) {
        final String userEmail = AuthUtil.extractUserEmail(authorization);
        logger.debug("fetchAttachment called: baseUrl={}, userEmail={}, reportId={}", baseUrl, userEmail, reportId);
        final String authHeader = getAuthHeader(connectorAuth);

        return fetchLoginIdFromUserEmail(userEmail, baseUrl, authHeader)
                .flatMap(loginID -> validateUser(baseUrl, reportId, loginID, authHeader))
                .then(fetchRequestData(baseUrl, reportId, authHeader))
                .flatMap(expenseReportResponse -> getAttachment(expenseReportResponse, connectorAuth))
                .map(clientResponse -> handleClientResponse(clientResponse, reportId));
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

    private ResponseEntity<Flux<DataBuffer>> handleClientResponse(final ClientResponse response, final String reportId) {
        if (response.statusCode().is2xxSuccessful()) {
            return ResponseEntity.ok()
                    .contentType(response.headers().contentType().orElse(APPLICATION_PDF))
                    .header(CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_FORMAT, reportId))
                    .body(response.bodyToFlux(DataBuffer.class));

        }

        return handleErrorStatus(response);
    }

    private ResponseEntity<Flux<DataBuffer>> handleErrorStatus(final ClientResponse response) {
        final HttpStatus status = response.statusCode();
        final String backendStatus = Integer.toString(response.rawStatusCode());

        if (status == UNAUTHORIZED) {
            String body = "{\"error\" : \"invalid_connector_token\"}";
            final DataBuffer dataBuffer = convertStringToDataBuffer(body);
            return ResponseEntity.status(BAD_REQUEST)
                    .header(BACKEND_STATUS, backendStatus)
                    .contentType(APPLICATION_JSON)
                    .body(Flux.just(dataBuffer));
        } else {
            final ResponseEntity.BodyBuilder builder = ResponseEntity.status(INTERNAL_SERVER_ERROR).header(BACKEND_STATUS, backendStatus);
            final MediaType contentType = response.headers().contentType().orElse(null);
            if (contentType != null) {
                builder.contentType(contentType);
            }
            return builder.body(response.bodyToFlux(DataBuffer.class));
        }
    }

    private DataBuffer convertStringToDataBuffer(String body) {
        final NettyDataBufferFactory factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
        return factory.wrap(body.getBytes(StandardCharset.UTF_8));
    }

    @ExceptionHandler(AttachmentURLNotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    @ResponseBody
    public Map<String, String> handleAttachmentURLNotFoundException(AttachmentURLNotFoundException e) {
        return Map.of("message", e.getMessage());
    }
}
