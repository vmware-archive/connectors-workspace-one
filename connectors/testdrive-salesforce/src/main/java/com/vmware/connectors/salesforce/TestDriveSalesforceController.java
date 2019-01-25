/*
 * Copyright © 2019 VMware, Inc. All rights reserved. This product is protected by
 * copyright and intellectual property laws in the United States and other countries as
 * well as by international treaties. AirWatch products may be covered by one or more
 * patents listed at http://www.vmware.com/go/patents.
 */

package com.vmware.connectors.salesforce;

import com.google.common.annotations.VisibleForTesting;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.Reactive;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DatePrinter;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;


/**
 * This class implements a mobile flows connector for Salesforce according to the specifications of APF-1739.
 * Its <code>/cards/requests</code> endpoint delivers cards for each Opportunity owned by the user
 * (the "user_email" parameter) that another party (the "sender_email" parameter) is related to.
 *
 * The cards display the following fields:
 *
 * Card Heading: Customer Name - Opportunity heading
 * Card Sub-heading: Expected Close Date / Deal Size
 * Icon: Salesforce logo
 * Field: Label - Account, Value - String
 * Field: Label - Account Owner, Value - String
 * Field: Label - Close date, Value - String
 * Field: Label - Sales stage, Value - String
 * Field: Label - Amount, Value - Float
 * Field: Label - Comments, Value - String (latest two comments added to the opportunity)
 *
 * ...and provide the following actions:
 *
 * Action: Label - Update close date
 * Action: Label - Update deal size
 * Action: Label - Update next steps
 *
 * Each of these actions has its own endpoint; they simply update the appropriate value in the Opportunity record
 * and return a 204 status. The "next steps" text is prepended to the previous value of the field, with a date
 * prepended in MM/dd format; the other actions just replace the previous values with the new.
 *
 * This connector expects to run against a standard Opportunity schema with no modified fields.
 */
@RestController
public class TestDriveSalesforceController {

    private static final Logger logger = LoggerFactory.getLogger(TestDriveSalesforceController.class);

    private static final String SALESFORCE_AUTH_HEADER = "X-Connector-Authorization";
    private static final String SALESFORCE_BASE_URL_HEADER = "X-Connector-Base-Url";

    private static final String ROUTING_PREFIX = "x-routing-prefix";

    private static final int COMMENTS_SIZE = 2;

    // Query format to list all opportunities related to a given sender and recipient
    @VisibleForTesting
    static final String QUERY_FMT_RELATED_OPPORTUNITY_IDS =
            "SELECT Opportunity.id FROM OpportunityContactRole " +
                    "WHERE contact.email = '%s' AND Opportunity.StageName != 'Closed Won' " +
                    "AND (Opportunity.Account.Owner.email = '%s' OR Opportunity.Owner.email = '%s')";

    // Query everything needed for making Opportunity cards.
    @VisibleForTesting
    static final String QUERY_FMT_OPPORTUNITY_INFO =
            "SELECT id, name, CloseDate, NextStep, StageName, Account.name, Account.Owner.Name, FORMAT(Opportunity.amount), " +
            "FORMAT(Opportunity.ExpectedRevenue), (SELECT User.Email from OpportunityTeamMembers), " +
            "(SELECT InsertedBy.Name, Body FROM Feeds) FROM opportunity WHERE opportunity.id IN (%s)";

    // Endpoint URL templates for the three card actions
    private static final String URL_TEMPLATE_UPDATE_CLOSE_DATE = "/opportunity/{opportunityId}/closedate";
    private static final String URL_TEMPLATE_UPDATE_DEAL_SIZE = "/opportunity/{opportunityId}/dealsize";
    private static final String URL_TEMPLATE_UPDATE_NEXT_STEP = "/opportunity/{opportunityId}/nextstep";

    private static final String OPPORTUNITY_ID = "opportunityId";

    private static final String TEMPLATE = "templates/generic.hbs";
    private static final String QUERY_PARAM_CLOSEDATE = "closedate";
    private static final String QUERY_PARAM_DEALSIZE = "amount";
    private static final String QUERY_PARAM_NEXTSTEP = "nextstep";
    private static final String QUERY_PARAM_NEXTSTEP_PREVIOUS_VALUE = "nextstep_previous_value";

    private static final DatePrinter mmddDatePrinter = FastDateFormat.getInstance("MM-dd");

    private final String sfSoqlQueryPath;

    private final String sfOpportunityFieldsUpdatePath;

    private final WebClient rest;

    private final CardTextAccessor cardTextAccessor;

    @Autowired
    public TestDriveSalesforceController(
            WebClient rest,
            CardTextAccessor cardTextAccessor,
            @Value("${sf.soqlQueryPath}") String sfSoqlQueryPath,
            @Value("${sf.opportunityFieldsUpdatePath}") final String sfOpportunityFieldsUpdatePath) {

        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.sfSoqlQueryPath = sfSoqlQueryPath;
        this.sfOpportunityFieldsUpdatePath = sfOpportunityFieldsUpdatePath;
    }

    ///////////////////////////////////////////////////////////////////
    // Cards request methods
    ///////////////////////////////////////////////////////////////////

    @SuppressWarnings("unused")
    @PostMapping(
            path = "/cards/requests",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Cards>> getCards(
            @RequestHeader(SALESFORCE_AUTH_HEADER) String auth,
            @RequestHeader(SALESFORCE_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ROUTING_PREFIX) String routingPrefix,
            Locale locale,
            @Valid @RequestBody CardRequest cardRequest,
            final HttpServletRequest request) {

        // Sender email and user email are required, and sender email has to at least have a non-final @ in it
        final String sender = cardRequest.getTokenSingleValue("sender_email");
        final String user = cardRequest.getTokenSingleValue("user_email");

        logger.debug("Sender email: {} and User email: {} for Salesforce server: {} ", sender, user, baseUrl);

        String bareSenderDomain = StringUtils.substringAfterLast(sender, "@");
        // TODO: implement a better system of validating domain names than "yup, it's not empty"
        if (StringUtils.isBlank(bareSenderDomain) || StringUtils.isBlank(user)) {
            logger.warn("Either sender email or user email is blank");
            return Mono.just(new ResponseEntity<>(BAD_REQUEST));
        }

        return queryRelatedOpportunityIDs(auth, baseUrl, user, sender)
                .map(this::parseOpportunityIDs)
                .flatMap(idsList -> queryOpportunityDetails(auth, baseUrl, idsList))
                .flatMapIterable(this::parseOpportunityObjects)
                .map(opportunity -> buildCardForRelatedOpportunity(routingPrefix, locale, opportunity))
                .collectList()
                .map(this::toCards)
                .map(ResponseEntity::ok)
                .subscriberContext(Reactive.setupContext());
    }

    // Get a list of relevant opportunity ID's
    private Mono<String> queryRelatedOpportunityIDs(String auth, String baseUrl, String userEmail, String senderEmail) {
        String soql = String.format(QUERY_FMT_RELATED_OPPORTUNITY_IDS,
                soqlEscape(senderEmail), soqlEscape(userEmail), soqlEscape(userEmail));
        return rest.get()
                .uri(makeSoqlQueryUri(baseUrl, soql))
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(String.class);
    }

    // Get the details of one or more Opportunities
    private Mono<String> queryOpportunityDetails(String auth, String baseUrl, List<String> idsList) {
        if (CollectionUtils.isEmpty(idsList)) {
            return Mono.just("");
        }
        String soql = String.format(QUERY_FMT_OPPORTUNITY_INFO, commaSeparatedListOfEscapedIds(idsList));
        return rest.get()
                .uri(makeSoqlQueryUri(baseUrl, soql))
                .header(AUTHORIZATION, auth)
                .retrieve()
                .bodyToMono(String.class);
    }

    @VisibleForTesting
    static String commaSeparatedListOfEscapedIds(List<String> idsList) {
        return idsList.stream()
                .map(id -> "'" + soqlEscape(id) + "'")
                .collect(Collectors.joining(","));
    }

    /**
     * Escape special characters to prevent user input from SOQL injection:
     *
     * https://developer.salesforce.com/page/Secure_Coding_SQL_Injection
     * https://developer.salesforce.com/docs/atlas.en-us.soql_sosl.meta/soql_sosl/sforce_api_calls_soql_select_quotedstringescapes.htm
     * https://developer.salesforce.com/docs/atlas.en-us.soql_sosl.meta/soql_sosl/sforce_api_calls_soql_select_reservedcharacters.htm
     *
     * @param value the user input to escape
     * @return the escaped string value that should be safe to string concat into a SOQL query
     */
    @VisibleForTesting
    static String soqlEscape(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\'", "\\\'");
    }

    // Take the JSON response from Salesforce and pull out all the opportunity IDs
    private List<String> parseOpportunityIDs(String json) {
        Configuration conf = Configuration.builder().options(Option.DEFAULT_PATH_LEAF_TO_NULL).build();
        return JsonPath.using(conf).parse(json).read( "$.records[*].Opportunity.Id");
    }

    // Break the Salesforce response up into individual records
    private Iterable<SFOpportunity> parseOpportunityObjects(String json) {
        return SFOpportunity.fromJson(json);
    }

    // Assemble a card for an Opportunity
    private Card buildCardForRelatedOpportunity(String routingPrefix, Locale locale, SFOpportunity opportunity) {

        final CardBody.Builder cardBodyBuilder = new CardBody.Builder();

        addFieldIfNotNull(cardBodyBuilder, "opportunity.account", opportunity.getAccountName(), locale);
        addFieldIfNotNull(cardBodyBuilder, "opportunity.account.owner", opportunity.getAccountOwner(), locale);
        addFieldIfNotNull(cardBodyBuilder, "opportunity.closedate", opportunity.getCloseDate(), locale);
        addFieldIfNotNull(cardBodyBuilder, "opportunity.stage", opportunity.getStageName(), locale);
        addFieldIfNotNull(cardBodyBuilder, "opportunity.amount", opportunity.getAmount(), locale);

        addCommentsField(cardBodyBuilder, opportunity.getFeedEntries(), locale);

        final Card.Builder cardBuilder = new Card.Builder()
                .setTemplate(routingPrefix + TEMPLATE)
                .setHeader(cardTextAccessor.getMessage("opportunity.oppheader", locale, opportunity.getName()),
                        cardTextAccessor.getMessage("opportunity.closedate", locale) + " " + opportunity.getCloseDate(),
                        cardTextAccessor.getMessage("opportunity.expected.revenue", locale) + " " + opportunity.getExpectedRevenue())
                .setBody(cardBodyBuilder.build());

        addCardActions(routingPrefix, locale, cardBuilder, opportunity.getId(), opportunity.getNextStep());

        return cardBuilder.build();
    }

    // Add a field to the card only if its value is not empty
    private void addFieldIfNotNull(CardBody.Builder builder, String messageKey, String fieldValue, Locale locale) {
        if (StringUtils.isNotBlank(fieldValue)) {
            builder.addField(buildGeneralBodyField(messageKey, fieldValue, locale));
        }
    }

    // Assemble and add the comments field
    private void addCommentsField(CardBody.Builder cardBodyBuilder, List<String> feedComments, Locale locale) {

        if (feedComments != null && !feedComments.isEmpty()) {
            CardBodyField.Builder cardFieldBuilder = new CardBodyField.Builder();

            cardFieldBuilder.setTitle(cardTextAccessor.getMessage("opportunity.comments", locale))
                    .setType(CardBodyFieldType.COMMENT);

            feedComments.stream()
                    .limit(COMMENTS_SIZE)
                    .forEach(comment -> cardFieldBuilder.addContent(Map.of("text", comment)));

            cardBodyBuilder.addField(cardFieldBuilder.build());
        }
    }

    // Create a field for a single label and value
    private CardBodyField buildGeneralBodyField(String titleMessageKey, String description, Locale locale) {
        if (StringUtils.isBlank(description)) {
            return null;
        }
        return new CardBodyField.Builder()
                .setTitle(cardTextAccessor.getMessage(titleMessageKey, locale))
                .setType(CardBodyFieldType.GENERAL)
                .setDescription(description)
                .build();
    }

    // Add actions to the card
    private void addCardActions(String routingPrefix, Locale locale, Card.Builder card, String opportunityId, String previousNextStepText) {

        CardAction.Builder updateNextStepBuilder = buildCardAction(
                buildActionUrlFromTemplate(URL_TEMPLATE_UPDATE_NEXT_STEP, routingPrefix, opportunityId),
                "opportunity.update.nextstep",
                "opportunity.update.nextstep.field.label",
                QUERY_PARAM_NEXTSTEP,
                locale
                );
        updateNextStepBuilder.addRequestParam(QUERY_PARAM_NEXTSTEP_PREVIOUS_VALUE, previousNextStepText);
        card.addAction(updateNextStepBuilder.build());

        CardAction.Builder updateDealSizeBuilder = buildCardAction(
                buildActionUrlFromTemplate(URL_TEMPLATE_UPDATE_DEAL_SIZE, routingPrefix, opportunityId),
                "opportunity.update.dealsize",
                "opportunity.update.dealsize.field.label",
                QUERY_PARAM_DEALSIZE,
                locale
        );
        card.addAction(updateDealSizeBuilder.build());

        CardAction.Builder updateCloseDateBuilder = buildCardAction(
                buildActionUrlFromTemplate(URL_TEMPLATE_UPDATE_CLOSE_DATE, routingPrefix, opportunityId),
                "opportunity.update.closedate",
                "opportunity.update.closedate.field.label",
                QUERY_PARAM_CLOSEDATE,
                locale
        );
        card.addAction(updateCloseDateBuilder.build());
    }

    // Build an individual action
    private CardAction.Builder buildCardAction(String actionUrl, String actionLabel, String fieldLabel, String paramName, Locale locale) {
        return new CardAction.Builder()
                .setLabel(this.cardTextAccessor.getActionLabel(actionLabel, locale))
                .setActionKey(CardActionKey.USER_INPUT)
                .setType(HttpMethod.POST)
                .setUrl(actionUrl)
                .setAllowRepeated(true)
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setId(paramName)
                                .setLabel(this.cardTextAccessor.getMessage(fieldLabel, locale))
                                .build()
                );
    }

    // Assemble the URL for the action
    private String buildActionUrlFromTemplate(String template, String routingPrefix, String opportunityId) {
        return fromHttpUrl(routingPrefix)
                .path(template)
                .buildAndExpand(opportunityId)
                .encode()
                .toUriString();
    }

    // Assemble the Salesforce query into a URL
    private URI makeSoqlQueryUri(String baseUrl, String soql) {
        return fromHttpUrl(baseUrl)
                .path(sfSoqlQueryPath)
                .queryParam("q", soql)
                .build()
                .toUri();
    }

    // Wrap a list of Card objects into a Cards object
    private Cards toCards(List<Card> cards) {
        Cards c = new Cards();
        c.getCards().addAll(cards);
        return c;
    }

    ///////////////////////////////////////////////////////////////////
    // Action methods
    ///////////////////////////////////////////////////////////////////

    // Update the CloseDate field of the Opportunity
    @SuppressWarnings("unused")
    @PostMapping(
            path = URL_TEMPLATE_UPDATE_CLOSE_DATE,
            consumes = APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<ResponseEntity<Void>> updateCloseDate(
            @RequestHeader(SALESFORCE_AUTH_HEADER) final String auth,
            @RequestHeader(SALESFORCE_BASE_URL_HEADER) final String baseUrl,
            @PathVariable(OPPORTUNITY_ID) final String opportunityId,
            @RequestParam(QUERY_PARAM_CLOSEDATE) final String closeDate) {

        // CloseDate should in the format "YYYY-MM-DD".
        final Map<String, String> body = Map.of("CloseDate", closeDate);

        return updateOpportunityField(baseUrl, auth, opportunityId, body);
    }

    // Update the DealSize field of the Opportunity
    @SuppressWarnings("unused")
    @PostMapping(
            path = URL_TEMPLATE_UPDATE_DEAL_SIZE,
            consumes = APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<ResponseEntity<Void>> updateDealSize(
            @RequestHeader(SALESFORCE_AUTH_HEADER) final String auth,
            @RequestHeader(SALESFORCE_BASE_URL_HEADER) final String baseUrl,
            @PathVariable(OPPORTUNITY_ID) final String opportunityId,
            @RequestParam(QUERY_PARAM_DEALSIZE) final String newAmount) {

        final Map<String, String> body = Map.of("Amount", newAmount);

        return updateOpportunityField(baseUrl, auth, opportunityId, body);
    }

    // Update the NextStep field of the Opportunity
    @SuppressWarnings("unused")
    @PostMapping(
            path = URL_TEMPLATE_UPDATE_NEXT_STEP,
            consumes = APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<ResponseEntity<Void>> updateNextStep(
            @RequestHeader(SALESFORCE_AUTH_HEADER) final String auth,
            @RequestHeader(SALESFORCE_BASE_URL_HEADER) final String baseUrl,
            @PathVariable(OPPORTUNITY_ID) final String opportunityId,
            @RequestParam(QUERY_PARAM_NEXTSTEP) final String nextStep,
            @RequestParam(QUERY_PARAM_NEXTSTEP_PREVIOUS_VALUE) final String previousNextStepText) {

        Map<String, String> body = null;

        if (StringUtils.isNotBlank(nextStep)) {
            String newNextStep = mmddDatePrinter.format(new Date()) + ": " + nextStep;
            if (StringUtils.isNotBlank(previousNextStepText)) {
                newNextStep = newNextStep + "\n" + previousNextStepText;
            }

            body = Map.of("NextStep", newNextStep);
        }

        return updateOpportunityField(baseUrl, auth, opportunityId, body);
    }

    // Build and send the update request
    private Mono<ResponseEntity<Void>> updateOpportunityField(final String baseUrl,
                                              final String auth,
                                              final String opportunityId,
                                              final Object body) {
        return rest.patch()
                .uri(buildOpportunityUri(baseUrl, sfOpportunityFieldsUpdatePath, opportunityId))
                .header(AUTHORIZATION, auth)
                .contentType(APPLICATION_JSON)
                .syncBody(body)
                .exchange()
                .flatMap(Reactive::checkStatus)
                .flatMap(response -> response.toEntity(Void.class));
    }

    // Insert the opportunity ID into the URL
    private URI buildOpportunityUri(final String baseUrl, final String path, final String opportunityId) {
        return fromHttpUrl(baseUrl)
                .path(path)
                .path(opportunityId)
                .build()
                .toUri();
    }
}
