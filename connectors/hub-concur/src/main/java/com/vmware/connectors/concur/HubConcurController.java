/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardAction;
import com.vmware.connectors.common.payloads.response.CardActionInputField;
import com.vmware.connectors.common.payloads.response.CardActionKey;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.CardBodyField;
import com.vmware.connectors.common.payloads.response.CardBodyFieldType;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.CommonUtils;
import com.vmware.connectors.common.web.UserException;
import com.vmware.connectors.concur.domain.ExpenseReportResponse;
import com.vmware.connectors.concur.domain.PendingApprovalResponse;
import com.vmware.connectors.concur.domain.PendingApprovalsVO;
import com.vmware.connectors.concur.domain.UserDetailsResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML;

@RestController
public class HubConcurController {

    private static final Logger logger = LoggerFactory.getLogger(HubConcurController.class);

    private static final String X_AUTH_HEADER = "X-Connector-Authorization";
    private static final String X_BASE_URL_HEADER = "X-Connector-Base-Url";

    private static final String COMMENT_KEY = "comment";
    private static final String REASON_KEY = "reason";

    private static final String APPROVE = "APPROVE";
    private static final String REJECT = "Send Back to Employee";

    private final WebClient rest;
    private final CardTextAccessor cardTextAccessor;
    private final Resource concurRequestTemplate;
    private final String serviceAccountAuthHeader;

    @Autowired
    public HubConcurController(
            WebClient rest,
            CardTextAccessor cardTextAccessor,
            @Value("classpath:static/templates/concur-request-template.xml") Resource concurRequestTemplate,
            @Value("${concur.service-account-auth-header}") String serviceAccountAuthHeader
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
    public Mono<Cards> getCards(
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(X_AUTH_HEADER) String vidmAuthHeader,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @RequestHeader("X-Routing-Prefix") String routingPrefix,
            Locale locale,
            HttpServletRequest request
    ) {
        String userEmail = AuthUtil.extractUserEmail(authorization);
        if (StringUtils.isBlank(userEmail)) {
            logger.error("User email  is empty in jwt access token.");
            // TODO: This returns an empty object,can we throw an exception or return it as
            // a bad request?
            return Mono.just(new Cards());
        }

        logger.debug("getCards called: baseUrl={}, userEmail={}", baseUrl, userEmail);
        return fetchCards(baseUrl, locale, routingPrefix, request, userEmail);
    }

    private Mono<Cards> fetchCards(
            String baseUrl,
            Locale locale,
            String routingPrefix,
            HttpServletRequest request,
            String userEmail
    ) {
        logger.debug("fetchCards called: baseUrl={}, routingPrefix={} for userName = {}", baseUrl, routingPrefix, userEmail);

        return fetchLoginIdFromUserEmail(userEmail, baseUrl)
                .flatMapMany(userDetails -> Flux.fromIterable(userDetails.getItems()))
                .flatMap(
                        userDetail ->
                                fetchAllRequests(baseUrl, userDetail.getLoginId())
                                        .flatMapMany(expenses -> Flux.fromIterable(expenses.getPendingApprovals()))
                                        .flatMap(expense -> fetchRequestData(baseUrl, expense.getId()))
                                        .map(report -> makeCards(routingPrefix, locale, report, request))
                                        .reduce(new Cards(), this::addCard)
                )
                .next();
    }

    private Mono<UserDetailsResponse> fetchLoginIdFromUserEmail(
            String userEmail,
            String baseUrl
    ) {
        return rest.get()
                .uri(baseUrl + "/api/v3.0/common/users?primaryEmail={userEmail}", userEmail)
                .header(AUTHORIZATION, serviceAccountAuthHeader)
                .accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono(UserDetailsResponse.class);
    }

    private Mono<PendingApprovalResponse> fetchAllRequests(
            String baseUrl,
            String userEmail
    ) {
        int limit = 50;
        String userFilter = "all";
        return rest.get()
                .uri(baseUrl + "/api/v3.0/expense/reportdigests?approverLoginID={userEmail}&limit={limit}&user={userFilter}",
                        userEmail, limit, userFilter)
                .header(AUTHORIZATION, serviceAccountAuthHeader)
                .accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono(PendingApprovalResponse.class);
    }

    private Mono<ExpenseReportResponse> fetchRequestData(
            String baseUrl,
            String reportId
    ) {
        logger.debug("fetchRequestData called: baseUrl={}, reportId={}", baseUrl, reportId);

        return rest.get()
                .uri(baseUrl + "/api/expense/expensereport/v2.0/report/{reportId}", reportId)
                .header(AUTHORIZATION, serviceAccountAuthHeader)
                .accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono(ExpenseReportResponse.class);
    }

    private Card makeCards(
            String routingPrefix,
            Locale locale,
            ExpenseReportResponse report,
            HttpServletRequest request
    ) {
        String reportId = report.getReportID();
        String reportName = report.getReportName();

        logger.debug("makeCard called: routingPrefix={}, reportId={}, reportName={}", routingPrefix, reportId, reportName);

        Card.Builder builder = new Card.Builder()
                .setName("Concur")
                .setHeader(cardTextAccessor.getMessage("hub.concur.header", locale, reportName))
                .setBody(
                        new CardBody.Builder()
                                .addField(makeSubmissionDateField(locale, report))
                                .addField(makeRequestedByField(locale, report))
                                .addField(makeCostCenterField(locale, report))
                                .addField(makeExpenseAmountField(locale, report))
                                .build()
                )
                .addAction(makeApproveAction(routingPrefix, locale, reportId))
                .addAction(makeDeclineAction(routingPrefix, locale, reportId));

        CommonUtils.buildConnectorImageUrl(builder, request);

        return builder.build();
    }

    private CardBodyField makeSubmissionDateField(
            Locale locale,
            ExpenseReportResponse report
    ) {
        return new CardBodyField.Builder()
                .setType(CardBodyFieldType.GENERAL)
                .setTitle(cardTextAccessor.getMessage("hub.concur.submissionDate", locale))
                .setDescription(report.getSubmitDate())
                .build();
    }

    private CardBodyField makeRequestedByField(
            Locale locale,
            ExpenseReportResponse report
    ) {
        return new CardBodyField.Builder()
                .setType(CardBodyFieldType.GENERAL)
                .setTitle(cardTextAccessor.getMessage("hub.concur.requester", locale))
                .setDescription(report.getEmployeeName())
                .build();
    }

    private CardBodyField makeCostCenterField(
            Locale locale,
            ExpenseReportResponse report
    ) {
        return new CardBodyField.Builder()
                .setType(CardBodyFieldType.GENERAL)
                .setTitle(cardTextAccessor.getMessage("hub.concur.costCenter", locale))
                .setDescription(report.getCostCenter())
                .build();
    }

    private CardBodyField makeExpenseAmountField(
            Locale locale,
            ExpenseReportResponse report
    ) {
        return new CardBodyField.Builder()
                .setType(CardBodyFieldType.GENERAL)
                .setTitle(cardTextAccessor.getMessage("hub.concur.expenseAmount", locale))
                .setDescription(formatCurrency(report.getReportTotal(), locale, report.getCurrencyCode()))
                .build();
    }

    private String formatCurrency(
            String amount,
            Locale locale,
            String currencyCode
    ) {
        Locale localeToUse;
        if (Locale.ENGLISH.equals(locale)) {
            // Defaulting "en" locale to "en-US" if they didn't specify country
            localeToUse = Locale.US;
        } else {
            localeToUse = locale;
        }
        // TODO - does it make sense to format this as currency when we don't really
        // know the unit?

        // APF-1547 As suggested appending the currency code with the amount -

        return String.format(
                "%s %s",
                currencyCode,
                NumberFormat.getNumberInstance(localeToUse).format(Double.parseDouble(amount))
        );

    }

    private CardAction makeApproveAction(
            String routingPrefix,
            Locale locale,
            String reportId
    ) {
        return new CardAction.Builder()
                .setActionKey(CardActionKey.USER_INPUT)
                .setLabel(cardTextAccessor.getMessage("hub.concur.approve.label", locale))
                .setCompletedLabel(cardTextAccessor.getMessage("hub.concur.approve.completedLabel", locale))
                .setPrimary(true)
                .setMutuallyExclusiveSetId("approval-actions")
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + "api/expense/" + reportId + "/approve")
                .addUserInputField(
                        new CardActionInputField.Builder().setFormat("textarea")
                                .setId(COMMENT_KEY)
                                .setLabel(cardTextAccessor.getMessage("hub.concur.approve.comment.label", locale))
                                .build()
                )
                .build();
    }

    private CardAction makeDeclineAction(
            String routingPrefix,
            Locale locale,
            String reportId
    ) {
        return new CardAction.Builder()
                .setActionKey(CardActionKey.USER_INPUT)
                .setLabel(cardTextAccessor.getMessage("hub.concur.decline.label", locale))
                .setCompletedLabel(cardTextAccessor.getMessage("hub.concur.decline.completedLabel", locale))
                .setPrimary(false)
                .setMutuallyExclusiveSetId("approval-actions")
                .setType(HttpMethod.POST)
                .setUrl(routingPrefix + "api/expense/" + reportId + "/decline")
                .addUserInputField(
                        new CardActionInputField.Builder().setFormat("textarea")
                                .setId(REASON_KEY)
                                .setLabel(cardTextAccessor.getMessage("hub.concur.decline.reason.label", locale))
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
            path = "/api/expense/{id}/approve",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<String> approveRequest(
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(X_AUTH_HEADER) String vidmAuthHeader,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @PathVariable("id") String id,
            @RequestParam(COMMENT_KEY) String comment
    ) {
        logger.debug("approveRequest called: baseUrl={},  id={}, comment={}", baseUrl, id, comment);

        String userEmail = AuthUtil.extractUserEmail(authorization);
        if (StringUtils.isBlank(userEmail)) {
            logger.error("User email  is empty in jwt access token.");
            // Can I throw an exception here if useremail isnt found in the token or return
            // it as a bad request?
            return Mono.empty();
        }

        return makeConcurRequest(comment, baseUrl, APPROVE, id, userEmail);
    }

    private Mono<String> makeConcurRequest(
            String reason,
            String baseUrl,
            String action,
            String reportId,
            String userEmail
    ) {
        // TODO - APF-1546: privilege check based on the user in the JWT
        String concurRequestTemplate = getConcurRequestTemplate(reason, action);

        return fetchLoginIdFromUserEmail(userEmail, baseUrl)
                .flatMapMany(userDetails -> Flux.fromIterable(userDetails.getItems()))
                .flatMap(userDetail -> validateUser(baseUrl, reportId, userDetail.getLoginId()))
                .flatMap(expense -> fetchRequestData(baseUrl, reportId))
                .map(ExpenseReportResponse::getWorkflowActionURL)
                .flatMap(
                        url ->
                                rest.post()
                                        .uri(url)
                                        .header(AUTHORIZATION, serviceAccountAuthHeader)
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

    private Mono<PendingApprovalsVO> validateUser(
            String baseUrl,
            String reportId,
            String userEmail
    ) {
        // APF-1546: privilege check based on the user in the JWT

        return fetchAllRequests(baseUrl, userEmail)
                .flatMapMany(expenses -> Flux.fromIterable(expenses.getPendingApprovals()))
                // Check if the approverlogin and report is equal to that passed in the request
                .filter(expense -> expense.getId().equals(reportId) && expense.getApproverLoginID().equals(userEmail))
                .next()
                .switchIfEmpty(Mono.error(new UserException("Not Found"))); // CustomException
    }

    @PostMapping(
            path = "/api/expense/{id}/decline",
            consumes = APPLICATION_FORM_URLENCODED_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<String> declineRequest(
            @RequestHeader(AUTHORIZATION) String authorization,
            @RequestHeader(X_AUTH_HEADER) String vidmAuthHeader,
            @RequestHeader(X_BASE_URL_HEADER) String baseUrl,
            @PathVariable("id") String id,
            @RequestParam(REASON_KEY) String reason
    ) {
        logger.debug("declineRequest called: baseUrl={}, id={}, reason={}", baseUrl, id, reason);

        String userEmail = AuthUtil.extractUserEmail(authorization);
        if (StringUtils.isBlank(userEmail)) {
            logger.error("User email  is empty in jwt access token.");
            // TODO: Can I throw an exception here if useremail isnt found in the token or
            // return it as a bad request?
            return Mono.empty();
        }

        return makeConcurRequest(reason, baseUrl, REJECT, id, userEmail);
    }

}
