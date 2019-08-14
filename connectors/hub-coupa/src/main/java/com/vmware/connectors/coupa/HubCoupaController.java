/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.coupa;

import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.web.UserException;
import com.vmware.connectors.coupa.domain.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.*;

@RestController
public class HubCoupaController {

    private static final Logger logger = LoggerFactory.getLogger(HubCoupaController.class);

    private static final String COMMENT_KEY = "comment";
    private static final String AUTHORIZATION_HEADER_NAME = "X-COUPA-API-KEY";
    private static final String X_BASE_URL_HEADER = "X-Connector-Base-Url";

    private static final String CONNECTOR_AUTH = "X-Connector-Authorization";

    private static final String ATTACHMENT_URL = "/api/users/{user_id}/attachments/{attachment_id}";

    private static final String UNAUTHORIZED_ATTACHMENT_ACCESS = "User with approvable ID: %s is trying to fetch an attachment with ID: %s which does not belong to them.";

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
                .flatMap(u -> getApprovalDetails(baseUrl, u.getId(), userEmail, connectorAuth))
                .map(req -> makeCards(routingPrefix, locale, req))
                .reduce(new Cards(), this::addCard);
    }

    private Flux<UserDetails> getUserDetails(String userEmail, String baseUrl, String connectorAuth) {
        return rest.get()
                .uri(baseUrl + "/api/users?email={userEmail}", userEmail)
                .accept(APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER_NAME, connectorAuth)
                .retrieve()
                .bodyToFlux(UserDetails.class);
    }

    private Flux<RequisitionDetails> getApprovalDetails(
            String baseUrl,
            String userId,
            String userEmail,
            String connectorAuth
    ) {
        logger.debug("Getting approval details for the user id :: {}", userId);

        return rest.get()
                .uri(baseUrl + "/api/approvals?approver_id={userId}&status=pending_approval", userId)
                .accept(APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER_NAME, connectorAuth)
                .retrieve()
                .bodyToFlux(ApprovalDetails.class)
                .flatMap(ad -> getRequisitionDetails(baseUrl, ad.getApprovableId(), userEmail, connectorAuth));
    }

    private Flux<RequisitionDetails> getRequisitionDetails(
            String baseUrl,
            String approvableId,
            String userEmail,
            String connectorAuth
    ) {
        logger.trace("Fetching Requisition details for {} and user {} ", approvableId, userEmail);

        return rest.get()
                .uri(baseUrl + "/api/requisitions?id={approvableId}&status=pending_approval", approvableId)
                .accept(APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER_NAME, connectorAuth)
                .retrieve()
                .bodyToFlux(RequisitionDetails.class)
                .filter(requisition -> userEmail.equals(requisition.getCurrentApproval().getApprover().getEmail()));
    }

    private Card makeCards(
            String routingPrefix,
            Locale locale,
            RequisitionDetails requestDetails
    ) {
        String requestId = requestDetails.getId();
        String reportName = requestDetails.getRequisitionLinesList().get(0).getDescription();

        logger.trace("makeCard called: routingPrefix={}, requestId={}, reportName={}",
                routingPrefix, requestId, reportName);

        Card.Builder builder = new Card.Builder()
                .setName("Coupa")
                .setHeader(cardTextAccessor.getMessage("hub.coupa.header", locale, reportName))
                .setBody(buildCardBody(locale, requestDetails))
                .setBackendId(requestId)
                .addAction(makeApprovalAction(routingPrefix, requestId, locale,
                        true, "api/approve/", "hub.coupa.approve", "hub.coupa.approve.comment.label"))
                .addAction(makeApprovalAction(routingPrefix, requestId, locale,
                        false, "api/decline/", "hub.coupa.decline", "hub.coupa.decline.reason.label"));

        builder.setImageUrl("https://s3.amazonaws.com/vmw-mf-assets/connector-images/hub-coupa.png");
        return builder.build();
    }

    private CardBody buildCardBody(Locale locale, RequisitionDetails requestDetails) {
        final CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .addField(makeGeneralField(locale, "hub.coupa.requestDescription", requestDetails.getRequisitionDescription()))
                .addField(makeGeneralField(locale, "hub.coupa.requester", getRequestorName(requestDetails)))
                .addField(makeGeneralField(locale, "hub.coupa.expenseAmount", getFormattedAmount(requestDetails.getMobileTotal())))
                .addField(makeGeneralField(locale, "hub.coupa.justification", requestDetails.getJustification()));

        if (!CollectionUtils.isEmpty(requestDetails.getRequisitionLinesList())) {
            buildRequisitionDetails(requestDetails, locale).forEach(cardBodyBuilder::addField);
        }

        return cardBodyBuilder.build();
    }

    private List<CardBodyField> buildRequisitionDetails(RequisitionDetails requisitionDetails, Locale locale) {
        return requisitionDetails.getRequisitionLinesList()
                .stream()
                .map(lineDetails -> new CardBodyField.Builder()
                        .setType(CardBodyFieldType.SECTION)
                        .setTitle(cardTextAccessor.getMessage("hub.coupa.item.name", locale, lineDetails.getDescription()))
                        .addItems(buildItems(requisitionDetails, lineDetails, locale))
                        .build())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("PMD.NcssCount")
    private List<CardBodyFieldItem> buildItems(RequisitionDetails requisitionDetails, RequisitionLineDetails lineDetails, Locale locale) {
        final List<CardBodyFieldItem> items = new ArrayList<>();

        addItem("hub.coupa.item.name", lineDetails.getDescription(), locale, items);
        addItem("hub.coupa.item.quantity", lineDetails.getQuantity(), locale, items);
        addItem("hub.coupa.unit.price", lineDetails.getUnitPrice(), locale, items);
        addItem("hub.coupa.total.price", lineDetails.getTotal(), locale, items);
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

    private static String getFormattedAmount(String amount) {
        if (StringUtils.isBlank(amount)) {
            return amount;
        }

        BigDecimal amt = new BigDecimal(amount);
        DecimalFormat formatter = new DecimalFormat("#,###.00");

        return formatter.format(amt);
    }

    private CardAction makeApprovalAction(
            String routingPrefix,
            String requestId,
            Locale locale,
            boolean primary,
            String apiPath,
            String buttonLabelKey,
            String commentLabelKey
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
                                .setId(COMMENT_KEY)
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
            @Valid CommentForm form,
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

        return getRequisitionDetails(baseUrl, approvableId, userEmail, connectorAuth)
                .switchIfEmpty(Mono.error(new UserException("User Not Found")))
                .flatMap(requisitionDetails -> makeActionRequest(requisitionDetails.getCurrentApproval().getId(), baseUrl, action, reason, connectorAuth))
                .next();
    }

    private Mono<String> makeActionRequest(
            String id,
            String baseUrl,
            String action,
            String reason,
            String connectorAuth
    ) {
        return rest.put()
                .uri(baseUrl + "/api/approvals/{id}/{action}?reason={reason}", id, action, reason)
                .header(AUTHORIZATION_HEADER_NAME, connectorAuth)
                .accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorMap(WebClientResponseException.class, this::handleClientError);
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
            @Valid CommentForm form,
            @PathVariable("id") String id
    ) {
        String userEmail = AuthUtil.extractUserEmail(authorization);
        logger.debug("declineRequest called: baseUrl={},  id={}, comment={}", baseUrl, id, form.getComment());

        validateEmailAddress(userEmail);

        if (isServiceAccountCredentialEmpty(connectorAuth)) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return makeCoupaRequest(form.getComment(), baseUrl, "reject", id, userEmail, getAuthHeader(connectorAuth))
                .map(ResponseEntity::ok);
    }

    @GetMapping(
            path = "/api/user/{user_id}/attachment/{attachment_id}",
            produces = APPLICATION_OCTET_STREAM_VALUE
    )
    public Flux<DataBuffer> fetchAttachment(@RequestHeader(AUTHORIZATION) final String authorization,
                                            @RequestHeader(CONNECTOR_AUTH) final String connectorAuth,
                                            @RequestHeader(X_BASE_URL_HEADER) final String baseUrl,
                                            @PathVariable("user_id") final String userId,
                                            @PathVariable("attachment_id") final String attachmentId) {
        final String userEmail = AuthUtil.extractUserEmail(authorization);
        logger.debug("fetchAttachment called: baseUrl={}, userEmail={}, userId={}, attachmentId={}", baseUrl, userEmail, userEmail, attachmentId);

        validateEmailAddress(userEmail);

        return getRequisitionDetails(baseUrl, userId, userEmail, connectorAuth)
                .switchIfEmpty(Mono.error(new UserException(String.format(UNAUTHORIZED_ATTACHMENT_ACCESS, userId, attachmentId))))
                .flatMap(ignore -> getAttachment(connectorAuth, getAttachmentURI(baseUrl, userId, attachmentId)));
    }

    private Flux<DataBuffer> getAttachment(String connectorAuth, URI attachmentUri) {
        return this.rest.get()
                .uri(attachmentUri)
                .header(AUTHORIZATION_HEADER_NAME, connectorAuth)
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToFlux(DataBuffer.class);
    }

    private URI getAttachmentURI(final String baseUrl,
                                 final String userId,
                                 final String attachmentId) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(ATTACHMENT_URL)
                .buildAndExpand(
                        Map.of(
                                "user_id", userId,
                                "attachment_id", attachmentId
                        )
                )
                .toUri();
    }
}
