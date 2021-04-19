/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.coupa;

import com.nimbusds.jose.util.StandardCharset;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.web.InvalidUserActionException;
import com.vmware.connectors.common.web.UserException;
import com.vmware.connectors.common.web.UserNotFoundException;
import com.vmware.connectors.coupa.domain.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
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
public class HubCoupaController {

    private static final Logger logger = LoggerFactory.getLogger(HubCoupaController.class);

    private static final String COMMENT_KEY = "comment";
    private static final String AUTHORIZATION_HEADER_NAME = "X-COUPA-API-KEY";
    private static final String X_BASE_URL_HEADER = "X-Connector-Base-Url";

    private static final String CONNECTOR_AUTH = "X-Connector-Authorization";
    private static final String CONTENT_DISPOSITION_FORMAT = "attachment; filename=\"%s\"";
    private static final String ACTION_COMMENT = "%s has commented: %s";

    private static final String UNAUTHORIZED_ATTACHMENT_ACCESS = "User with approvable ID: %s is trying to fetch an attachment with ID: %s which does not belong to them.";
    private static final String USER_NOT_FOUND = "User with email id [%s] is not found.";
    private static final String INVALID_USER_ACTION = "User with email id [%s] is not authorized to approve/reject the expense report with approvable id [%s].";

    private static final String ATTACHMENT_TYPE_FILE = "AttachmentFile";
    private final WebClient rest;
    private final CardTextAccessor cardTextAccessor;
    private final String apiKey;

    @Autowired
    public HubCoupaController(
            WebClient rest,
            CardTextAccessor cardTextAccessor,
            @Value("${coupa.api-key:}") String apiKey
    ) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.apiKey = apiKey;
    }

    @PostMapping(
            path = "/cards/requests",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
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

        validateEmailAddress(userEmail);

        if (isServiceAccountCredentialEmpty(connectorAuth)) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return getPendingApprovals(userEmail, baseUrl, routingPrefix, getAuthHeader(connectorAuth), locale)
                .map(ResponseEntity::ok);
    }

    private boolean isServiceAccountCredentialEmpty(final String connectorAuth) {
        if (StringUtils.isBlank(this.apiKey) && StringUtils.isBlank(connectorAuth)) {
            logger.debug("X-Connector-Authorization should not be empty if service credentials are not present in the config file");
            return true;
        } else {
            return false;
        }
    }

    private String getAuthHeader(final String connectorAuth) {
        if (StringUtils.isBlank(this.apiKey))  {
            return connectorAuth;
        } else {
            return this.apiKey;
        }
    }

    private void validateEmailAddress(String userEmail) {
        if (StringUtils.isBlank(userEmail)) {
            logger.error("User email is empty in jwt access token.");
            throw new UserException("User Not Found");
        }
    }

    private Mono<Cards> getPendingApprovals(
            String userEmail,
            String baseUrl,
            String routingPrefix,
            String connectorAuth,
            Locale locale
    ) {
        logger.debug("Getting user id of {}", userEmail);

        return getUserDetails(userEmail, baseUrl, connectorAuth)
                .flatMap(user -> getApprovalDetails(baseUrl, user.getId(), connectorAuth)
                        .flatMap(ad -> fetchAndFilterRequisitionDetails(baseUrl, ad.getApprovableId(), userEmail, connectorAuth))
                        .map(req -> makeCards(baseUrl, routingPrefix, locale, req, user.getId())))
                .reduce(new Cards(), this::addCard)
                .defaultIfEmpty(new Cards());
    }

    private Flux<UserDetails> getUserDetails(String userEmail, String baseUrl, String connectorAuth) {
        return rest.get()
                .uri(baseUrl + "/api/users?email={userEmail}", userEmail)
                .accept(APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER_NAME, connectorAuth)
                .retrieve()
                .bodyToFlux(UserDetails.class);
    }

    private Flux<ApprovalDetails> getApprovalDetails(
            String baseUrl,
            String userId,
            String connectorAuth
    ) {
        logger.debug("Getting approval details for the user id :: {}", userId);

        return rest.get()
                .uri(baseUrl + "/api/approvals?approver_id={userId}&status=pending_approval", userId)
                .accept(APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER_NAME, connectorAuth)
                .retrieve()
                .bodyToFlux(ApprovalDetails.class);
    }

    private Flux<RequisitionDetails> fetchAndFilterRequisitionDetails(
            String baseUrl,
            String approvableId,
            String userEmail,
            String connectorAuth
    ) {
        logger.trace("Fetching Requisition details for {} and user {} ", approvableId, userEmail);

        return getRequisitionDetails(baseUrl, approvableId, connectorAuth)
                .filter(requisition -> userEmail.equals(requisition.getCurrentApproval().getApprover().getEmail()));
    }

    private Flux<RequisitionDetails> getRequisitionDetails(final String baseUrl,
                                                           final String approvableId,
                                                           final String connectorAuth) {
        return rest.get()
                .uri(baseUrl + "/api/requisitions?id={approvableId}&status=pending_approval", approvableId)
                .accept(APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER_NAME, connectorAuth)
                .retrieve()
                .bodyToFlux(RequisitionDetails.class);
    }

    private Card makeCards(
            String baseUrl,
            String routingPrefix,
            Locale locale,
            RequisitionDetails requestDetails,
            String userId
    ) {
        String requestId = requestDetails.getId();
        String reportName = requestDetails.getRequisitionLinesList().get(0).getDescription();

        logger.trace("makeCard called: routingPrefix={}, requestId={}, reportName={}",
                routingPrefix, requestId, reportName);

        Card.Builder builder = new Card.Builder()
                .setName("Coupa")
                .setHeader(
                        new CardHeader(
                                cardTextAccessor.getMessage("hub.coupa.header", locale, requestDetails.getCurrentApproval().getApprovableId()),
                                null,
                                new CardHeaderLinks(
                                        UriComponentsBuilder.fromUriString(baseUrl)
                                                .path("/requisition_headers/")
                                                .path(requestId)
                                                .toUriString(),
                                        null
                                )
                        )
                )
                .setBody(buildCardBody(routingPrefix, requestDetails, userId, locale))
                .setBackendId(requestId)
                .addAction(makeApprovalAction(routingPrefix, requestId, locale,
                        true, "api/approve/", "hub.coupa.approve",
                        "hub.coupa.approve.comment.label", 0))
                .addAction(makeApprovalAction(routingPrefix, requestId, locale,
                        false, "api/decline/", "hub.coupa.decline",
                        "hub.coupa.decline.reason.label", 1));

        builder.setImageUrl("https://s3.amazonaws.com/vmw-mf-assets/connector-images/hub-coupa.png");
        return builder.build();
    }

    private CardBody buildCardBody(
            String routingPrefix,
            RequisitionDetails requestDetails,
            String userId,
            Locale locale
    ) {
        final CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .addField(makeGeneralField(locale, "hub.coupa.requester", getRequestorName(requestDetails)))
                .addField(makeGeneralField(locale, "hub.coupa.expenseAmount", getFormattedAmount(requestDetails.getCurrency().getCode(), requestDetails.getMobileTotal())))
                .addField(makeGeneralField(locale, "hub.coupa.cost.center", requestDetails.getRequestorCostCenter()))
                .addField(makeGeneralField(locale, "hub.coupa.requestDescription", requestDetails.getRequisitionDescription()))
                .addField(makeGeneralField(locale, "hub.coupa.justification", requestDetails.getJustification()));

        if (!CollectionUtils.isEmpty(requestDetails.getRequisitionLinesList())) {
            buildRequisitionDetails(requestDetails, locale).forEach(cardBodyBuilder::addField);

            buildAttachments(requestDetails, cardBodyBuilder, routingPrefix, userId, locale);
        }

        return cardBodyBuilder.build();
    }

    private List<CardBodyField> buildRequisitionDetails(RequisitionDetails requisitionDetails, Locale locale) {
        return requisitionDetails.getRequisitionLinesList()
                .stream()
                .map(lineDetails -> new CardBodyField.Builder()
                        .setType(CardBodyFieldType.SECTION)
                        .setTitle(lineDetails.getDescription())
                        .addItems(buildItems(requisitionDetails, lineDetails, locale))
                        .build())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("PMD.NcssCount")
    private List<CardBodyFieldItem> buildItems(RequisitionDetails requisitionDetails, RequisitionLineDetails lineDetails, Locale locale) {
        final List<CardBodyFieldItem> items = new ArrayList<>();

        addItem("hub.coupa.item.quantity", lineDetails.getQuantity(), locale, items);
        addItem("hub.coupa.unit.price", getFormattedAmount(requisitionDetails.getCurrency().getCode(), lineDetails.getUnitPrice()), locale, items);
        addItem("hub.coupa.total.price", getFormattedAmount(requisitionDetails.getCurrency().getCode(),lineDetails.getTotal()), locale, items);
        addItem("hub.coupa.commodity", lineDetails.getCommodity().getName(), locale, items);
        addItem("hub.coupa.supplier.part.number", lineDetails.getSupplier().getCompanyCode(), locale, items);

        addItem("hub.coupa.need.by", lineDetails.getNeedByDate(), locale, items);
        addItem("hub.coupa.payment.terms", lineDetails.getPaymentTerm().getCode(), locale, items);
        addItem("hub.coupa.shipping", lineDetails.getShippingTerm().getCode(), locale, items);
        addItem("hub.coupa.sap.group.material.id", lineDetails.getSapMaterialGroupId(), locale, items);
        addItem("hub.coupa.billing.address", getShippingDetails(requisitionDetails.getShipToAddress()), locale, items);
        addItem("hub.coupa.billing.account", requisitionDetails.getShipToAddress().getLocationCode(), locale, items);

        return items;
    }

    private void buildAttachments(final RequisitionDetails requisitionDetails,
                                  final CardBody.Builder builder,
                                  final String routingPrefix,
                                  final String userId,
                                  final Locale locale) {
        if (CollectionUtils.isEmpty(requisitionDetails.getAttachments())) {
            logger.debug("No attachments found for coupa report with request ID: {}", requisitionDetails.getId());
            return;
        }

        final CardBodyField.Builder attachmentField = new CardBodyField.Builder()
                .setTitle(this.cardTextAccessor.getMessage("hub.coupa.attachments", locale))
                .setType(CardBodyFieldType.SECTION);

        final String approvableId = requisitionDetails.getApprovals().iterator().next().getApprovableId();
        for (Attachment attachment: requisitionDetails.getAttachments()) {
            // APF-3045: We only support AttachmentFile
            // remove this check after handling other types
            if (ATTACHMENT_TYPE_FILE.equalsIgnoreCase(attachment.getType())) {
                CardBodyFieldItem fieldItem = buildAttachmentItem(attachment, requisitionDetails.getId(), routingPrefix, userId, approvableId, locale);
                attachmentField.addItem(fieldItem);
            }

        }

        builder.addField(attachmentField.build());
    }

    private CardBodyFieldItem buildAttachmentItem(final Attachment attachment,
                                                  final String reportId,
                                                  final String routingPrefix,
                                                  final String userId,
                                                  final String approvableId,
                                                  final Locale locale) {
        final String fileName = StringUtils.substringAfterLast(attachment.getFile(), "/");

        if (fileName == null) {
            logger.error("No fileName found for attachment: file={}, reportId={}, userId={}, approvableId={} ", attachment.getFile(), reportId, userId, approvableId);
        }

        final String contentType = getContentType(fileName, reportId, attachment.getId());

        return new CardBodyFieldItem.Builder()
                .setAttachmentName(fileName)
                .setTitle(cardTextAccessor.getMessage("hub.coupa.report.title", locale))
                .setAttachmentMethod(HttpMethod.GET)
                .setAttachmentUrl(getAttachmentUrl(routingPrefix, userId, approvableId, fileName, attachment.getId()))
                .setType(CardBodyFieldType.ATTACHMENT_URL)
                .setAttachmentContentType(contentType)
                .build();
    }

    private String getAttachmentUrl(String routingPrefix,
                                    String userId,
                                    String approvableId,
                                    String fileName,
                                    String attachmentId) {
        return UriComponentsBuilder.fromUriString(routingPrefix)
                .path("/api/user/{user_id}/{approvable_id}/attachment/{file_name}/{attachment_id}")
                .buildAndExpand(
                        Map.of(
                                "user_id", userId,
                                "approvable_id", approvableId,
                                "file_name", fileName,
                                "attachment_id", attachmentId
                        )
                ).toUriString();
    }

    private String getContentType(final String fileName,
                                  final String reportId,
                                  final String attachmentId) {
        try {
            final Path path = new File(fileName).toPath();
            return Files.probeContentType(path);
        } catch (IOException e) {
            logger.error("Failed to retrieve the file content type for the report with ID: {} and attachment with ID: {}", reportId, attachmentId, e);
            return null;
        }
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

    private String getShippingDetails(final ShipToAddress shipToAddress) {
        if (shipToAddress == null) {
            return null;
        }

        return shipToAddress.getStreet1() + " "
                + shipToAddress.getStreet2() + " "
                + shipToAddress.getCity() + " "
                + shipToAddress.getPostalCode() + " "
                + shipToAddress.getState();
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

    private static String getRequestorName(RequisitionDetails reqDetails) {
        UserDetails requestedBy = reqDetails.getRequestedBy();

        if (requestedBy == null || StringUtils.isEmpty(requestedBy.getFirstName())) {
            return "";
        }

        return requestedBy.getFirstName() + " " + requestedBy.getLastName();
    }

    private static String getFormattedAmount(String currencyCode, String amount) {
        if (StringUtils.isBlank(amount)) {
            return amount;
        }

        BigDecimal amt = new BigDecimal(amount);
        DecimalFormat formatter = new DecimalFormat("#,###.00");

        return currencyCode + " " + formatter.format(amt);
    }

    private CardAction makeApprovalAction(
            String routingPrefix,
            String requestId,
            Locale locale,
            boolean primary,
            String apiPath,
            String buttonLabelKey,
            String commentLabelKey,
            Integer minLength
    ) {
        return new CardAction.Builder()
                .setActionKey(CardActionKey.USER_INPUT)
                .setLabel(cardTextAccessor.getActionLabel(buttonLabelKey, locale))
                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel(buttonLabelKey, locale))
                .setPrimary(primary)
                .setMutuallyExclusiveSetId("approval-actions")
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + apiPath + requestId)
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setFormat("textarea")
                                .setMinLength(minLength)
                                .setId(COMMENT_KEY)
                                .setMinLength(minLength)
                                .setLabel(cardTextAccessor.getMessage(commentLabelKey, locale))
                                .build()
                )
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
            path = "/api/approve/{id}",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<String>> approveRequest(
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(name = CONNECTOR_AUTH, required = false) String connectorAuth,
            CommentForm form,
            @PathVariable("id") String id
    ) {
        String userEmail = AuthUtil.extractUserEmail(authorization);
        logger.debug("approveRequest called: baseUrl={},  id={}, comment={}", baseUrl, id, form.getComment());

        validateEmailAddress(userEmail);

        if (isServiceAccountCredentialEmpty(connectorAuth)) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return makeCoupaRequest(form.getComment(), baseUrl, "approve", id, userEmail, getAuthHeader(connectorAuth))
                .map(ResponseEntity::ok);
    }

    private Mono<String> makeCoupaRequest(
            String reason,
            String baseUrl,
            String action,
            String approvableId,
            String userEmail,
            String connectorAuth
    ) {
        logger.debug("makeCoupaRequest called for user: userEmail={}, approvableId={}, action={}",
                userEmail, approvableId, action);

        return getUserDetails(userEmail, baseUrl, connectorAuth)
                .switchIfEmpty(Mono.error(new UserNotFoundException(String.format(USER_NOT_FOUND, userEmail))))
                .flatMap(userDetails -> fetchAndFilterRequisitionDetails(baseUrl, approvableId, userDetails.getEmail(), connectorAuth)
                        .switchIfEmpty(Mono.error(new InvalidUserActionException(String.format(INVALID_USER_ACTION, userEmail, approvableId))))
                        .flatMap(requisitionDetails -> makeActionRequest(requisitionDetails.getCurrentApproval().getId(), baseUrl, action, reason, connectorAuth, userDetails.getFullName()))
                )
                .next();
    }

    private Mono<String> makeActionRequest(
            String id,
            String baseUrl,
            String action,
            String reason,
            String connectorAuth,
            String fullName
    ) {
        WebClient.RequestBodySpec actionRequest;
        if (StringUtils.isBlank(reason)) {
            actionRequest = getActionRequestWithoutReason(baseUrl, id, action);
        } else {
            final String modifiedComment = String.format(ACTION_COMMENT, fullName, reason);
            actionRequest = getActionRequestWithReason(baseUrl, id, action, modifiedComment);
        }

        return actionRequest
                .header(AUTHORIZATION_HEADER_NAME, connectorAuth)
                .accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorMap(WebClientResponseException.class, this::handleClientError);
    }

    private WebClient.RequestBodySpec getActionRequestWithReason(
            final String baseUrl,
            final String id,
            final String action,
            final String reason
    ) {
        return rest.put()
                .uri(baseUrl + "/api/approvals/{id}/{action}?reason={reason}", id, action, reason);
    }

    private WebClient.RequestBodySpec getActionRequestWithoutReason(
            final String baseUrl,
            final String id,
            final String action
    ) {
        return rest.put()
                .uri(baseUrl + "/api/approvals/{id}/{action}", id, action);
    }

    private Throwable handleClientError(WebClientResponseException e) {
        logger.error("Exception caught : : {} ", e.getMessage());

        if (HttpStatus.BAD_REQUEST.equals(e.getStatusCode())) {
            return new UserException("Bad Request", e.getStatusCode());
        }

        return e;
    }

    @PostMapping(
            path = "/api/decline/{id}",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<String>> declineRequest(
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(name = CONNECTOR_AUTH, required = false) String connectorAuth,
            CommentForm form,
            @PathVariable("id") String id
    ) {
        String userEmail = AuthUtil.extractUserEmail(authorization);
        logger.debug("declineRequest called: baseUrl={},  id={}, comment={}", baseUrl, id, form.getComment());

        validateEmailAddress(userEmail);

        if (StringUtils.isBlank(form.getComment()) || isServiceAccountCredentialEmpty(connectorAuth)) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return makeCoupaRequest(form.getComment(), baseUrl, "reject", id, userEmail, getAuthHeader(connectorAuth))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/api/user/{user_id}/{approvable_id}/attachment/{file_name}/{attachment_id}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> fetchAttachment(@RequestHeader(AUTHORIZATION) final String authorization,
                                            @RequestHeader(CONNECTOR_AUTH) final String connectorAuth,
                                            @RequestHeader(X_BASE_URL_HEADER) final String baseUrl,
                                            @PathVariable("user_id") final String userId,
                                            @PathVariable("approvable_id") final String approvableId,
                                            @PathVariable("file_name") final String fileName,
                                            @PathVariable("attachment_id") final String attachmentId) {
        final String userEmail = AuthUtil.extractUserEmail(authorization);
        logger.debug("fetchAttachment called: baseUrl={}, userEmail={}, userId={}, attachmentId={}", baseUrl, userEmail, userEmail, attachmentId);

        validateEmailAddress(userEmail);

        final String authHeader = getAuthHeader(connectorAuth);
        return validateUserAttachmentInfo(baseUrl, authHeader, userEmail, userId, approvableId, attachmentId)
                .switchIfEmpty(Mono.error(new UserException(String.format(UNAUTHORIZED_ATTACHMENT_ACCESS, userId, attachmentId))))
                .then(getAttachment(authHeader, getAttachmentURI(baseUrl, approvableId, attachmentId)))
                .map(clientResponse -> handleClientResponse(clientResponse, fileName));
    }

    private Flux<Attachment> validateUserAttachmentInfo(final String baseUrl,
                                                        final String connectorAuth,
                                                        final String userEmail,
                                                        final String userId,
                                                        final String approvableId,
                                                        final String attachmentId) {
        return getUserDetails(userEmail, baseUrl, connectorAuth)
                .filter(userDetails -> userDetails.getId().equals(userId))
                .flatMap(userDetails -> getApprovalDetails(baseUrl, userDetails.getId(), connectorAuth))
                .filter(approvalDetails -> approvableId.equals(approvalDetails.getApprovableId()))
                .flatMap(approvalDetails -> fetchAndFilterRequisitionDetails(baseUrl, approvalDetails.getApprovableId(), userEmail, connectorAuth))
                .switchIfEmpty(Mono.error(new InvalidUserActionException(String.format(UNAUTHORIZED_ATTACHMENT_ACCESS, approvableId, attachmentId))))
                .flatMap(requisitionDetails -> validateAttachment(requisitionDetails.getAttachments(), attachmentId));
    }

    private Flux<Attachment> validateAttachment(List<Attachment> attachments, String attachmentId) {
        return Flux.fromIterable(attachments)
                .filter(attachment -> attachment.getId().equals(attachmentId));
    }

    private ResponseEntity<Flux<DataBuffer>> handleClientResponse(final ClientResponse response, final String fileName) {
        if (response.statusCode().is2xxSuccessful()) {
            return ResponseEntity.ok()
                    .header(CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_FORMAT, fileName))
                    .body(response.bodyToFlux(DataBuffer.class));
        }
        return handleErrorStatus(response);
    }

    private ResponseEntity<Flux<DataBuffer>> handleErrorStatus(final ClientResponse response) {
        final HttpStatus status = response.statusCode();
        final String backendStatus = Integer.toString(response.rawStatusCode());

        logger.error("Coupa backend returned the status code [{}] and reason phrase [{}] ", status, status.getReasonPhrase());

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

    private Mono<ClientResponse> getAttachment(String connectorAuth, URI attachmentUri) {
        return this.rest.get()
                .uri(attachmentUri)
                .header(AUTHORIZATION_HEADER_NAME, connectorAuth)
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .exchange();
    }

    private URI getAttachmentURI(final String baseUrl,
                                 final String approvableId,
                                 final String attachmentId) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/requisitions/{approvable_id}/attachments/{attachment_id}")
                .buildAndExpand(
                        Map.of(
                                "approvable_id", approvableId,
                                "attachment_id", attachmentId
                        )
                )
                .toUri();
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    @ResponseBody
    public Map<String, String> handleUserNotFoundException(UserNotFoundException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(InvalidUserActionException.class)
    @ResponseStatus(UNAUTHORIZED)
    @ResponseBody
    public Map<String, String> handleInvalidUserActionException(InvalidUserActionException e) {
        return Map.of("error", e.getMessage());
    }
}
