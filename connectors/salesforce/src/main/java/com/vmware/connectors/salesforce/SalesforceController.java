/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.salesforce;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.vmware.connectors.common.model.Message;
import com.vmware.connectors.common.model.MessageThread;
import com.vmware.connectors.common.model.UserRecord;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.CardAction;
import com.vmware.connectors.common.payloads.response.CardActionInputField;
import com.vmware.connectors.common.payloads.response.CardBody;
import com.vmware.connectors.common.payloads.response.CardBodyField;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.utils.Async;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import rx.Observable;
import rx.Single;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@RestController
public class SalesforceController {
    private final static Logger logger = LoggerFactory.getLogger(SalesforceController.class);
    private final static String SALESFORCE_AUTH_HEADER = "x-salesforce-authorization";
    private final static String SALESFORCE_BASE_URL_HEADER = "x-salesforce-base-url";
    private final static String ROUTING_PREFIX = "x-routing-prefix";
    private static final String ADD_CONVERSATIONS_PATH = "/conversations";
    private static final String CONVERSATION_TYPE = "email";

    private final static String WHITE_SPACE = " ";
    private final static String NEW_LINE = "\n";
    private final static int SIZE = 256;

    // Get all Accounts owned by the user that have an existing Contact with the same domain as the sender's email
    //     but *not* those where the sender is already a Contact
    // Unfortunately, SOQL doesn't have a "SELECT DISTINCT" query, so we get back one row per Contact, not one per Account,
    //     so an Account with four Contacts will yield four results
    // We will have to eliminate duplicate Accounts and filter out those Accounts where the sender is already a Contact
    private static final String QUERY_FMT_ACCOUNT =
            "SELECT email, account.id, account.name FROM contact WHERE email LIKE '%%%s' AND account.owner.email = '%s'";
    // Query format to get contact details of email sender, from contact list owned by the user.
    private static final String QUERY_FMT_CONTACT =
            "SELECT name, account.name, MobilePhone FROM contact WHERE email = '%s' AND contact.owner.email = '%s'";
    // Query format to get list of all opportunity details that are related to the email sender.
    private static final String QUERY_FMT_CONTACT_OPPORTUNITY =
            "SELECT Opportunity.name, role, FORMAT(Opportunity.amount), Opportunity.probability from " +
                    "OpportunityContactRole WHERE contact.email='%s' AND opportunity.owner.email='%s'";
    // Query format to get list of all opportunities that are related to an account.
    private static final String QUERY_FMT_ACCOUNT_OPPORTUNITY =
            "SELECT id, name FROM opportunity WHERE account.id = '%s'";

    private static final String QUERY_FMT_CONTACT_ID =
            "SELECT id FROM contact WHERE email = '%s' AND contact.owner.email = '%s'";


    private static final String ADD_CONTACT_PATH = "accounts/{accountId}/contacts";


    private final String sfSearchAccountPath;

    private final String sfAddContactPath;

    private final String sfOpportunityContactLinkPath;

    private final String sfOpportunityTaskLinkPath;

    private final String sfAttachmentTasklinkPath;

    private final AsyncRestOperations rest;

    private final CardTextAccessor cardTextAccessor;

    @Autowired
    public SalesforceController(AsyncRestOperations rest, CardTextAccessor cardTextAccessor,
                                @Value("${sf.searchAccountsPath}") String sfSearchAccountPath,
                                @Value("${sf.addContactPath}") String sfAddContactPath,
                                @Value("${sf.opportunityContactLinkPath}") String sfOpportunityContactLinkPath,
                                @Value("${sf.opportunityTaskLinkPath}") String sfOpportunityTaskLinkPath,
                                @Value("${sf.attachmentTasklinkPath}") String sfAttachmentTasklinkPath) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.sfSearchAccountPath = sfSearchAccountPath;
        this.sfAddContactPath = sfAddContactPath;
        this.sfOpportunityContactLinkPath = sfOpportunityContactLinkPath;
        this.sfOpportunityTaskLinkPath = sfOpportunityTaskLinkPath;
        this.sfAttachmentTasklinkPath = sfAttachmentTasklinkPath;
    }

    @PostMapping(path = "/cards/requests", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public Single<ResponseEntity<Cards>> getCards(
            @RequestHeader(name = SALESFORCE_AUTH_HEADER) String sfAuth,
            @RequestHeader(name = SALESFORCE_BASE_URL_HEADER) String sfBaseUrl,
            @RequestHeader(name = ROUTING_PREFIX) String routingPrefix,
            @Valid @RequestBody CardRequest cardRequest) {
        // Sender email and user email are required, and sender email has to at least have a non-final @ in it
        final String senderEmail = cardRequest.getTokenSingleValue("sender_email");
        final String userEmail = cardRequest.getTokenSingleValue("user_email");
        logger.debug("Sender email: {} and User email: {} for Salesforce server: {} ", senderEmail, userEmail, sfBaseUrl);
        final String senderDomain = '@' + StringUtils.substringAfterLast(senderEmail, "@");
        // TODO: implement a better system of validating domain names than "yup, it's not empty"
        if (StringUtils.isBlank(senderDomain) || StringUtils.isBlank(userEmail)) {
            logger.warn("Either sender email or user email is blank for url: {}", sfBaseUrl);
            return Single.just(new ResponseEntity<>(BAD_REQUEST));
        }

        // TODO: concatenating strings into a SOQL query like this may provide an avenue for a SOQL-injection attack
        String contactDetailsSoql = String.format(QUERY_FMT_CONTACT, senderEmail, userEmail);
        UriComponentsBuilder contactQueryUrlBuilder = fromHttpUrl(sfBaseUrl).path(sfSearchAccountPath).queryParam("q", contactDetailsSoql);
        String opportunityDetailsSoql = String.format(QUERY_FMT_CONTACT_OPPORTUNITY, senderEmail, userEmail);
        UriComponentsBuilder opportunityQueryUrlBuilder = fromHttpUrl(sfBaseUrl).path(sfSearchAccountPath).queryParam("q", opportunityDetailsSoql);
        String accountDetailsSoql = String.format(QUERY_FMT_ACCOUNT, senderDomain, userEmail);
        UriComponentsBuilder accountQueryUrlBuilder = fromHttpUrl(sfBaseUrl).path(sfSearchAccountPath).queryParam("q", accountDetailsSoql);

        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, sfAuth);

        ListenableFuture<ResponseEntity<JsonDocument>> contactsFuture =
                rest.exchange(contactQueryUrlBuilder.build().toUri(),
                        HttpMethod.GET, new HttpEntity<String>(headers), JsonDocument.class);
        ListenableFuture<ResponseEntity<JsonDocument>> opportunitiesFuture =
                rest.exchange(opportunityQueryUrlBuilder.build().toUri(),
                        HttpMethod.GET, new HttpEntity<String>(headers), JsonDocument.class);
        ListenableFuture<ResponseEntity<JsonDocument>> accountsFuture =
                rest.exchange(accountQueryUrlBuilder.build().toUri(),
                        HttpMethod.GET, new HttpEntity<String>(headers), JsonDocument.class);

        return Async.toSingle(contactsFuture)
                .map(HttpEntity::getBody)
                .flatMap(contactDetails -> getCards(contactDetails, senderEmail, sfBaseUrl, routingPrefix, headers,
                        opportunitiesFuture, accountsFuture))
                .map(ResponseEntity::ok);
    }

    private Single<Cards> getCards(JsonDocument contactDetails, String senderEmail, String sfBaseUrl, String routingPrefix, HttpHeaders headers,
                                   ListenableFuture<ResponseEntity<JsonDocument>> opportunitiesFuture,
                                   ListenableFuture<ResponseEntity<JsonDocument>> accountsFuture) {
        int contactsSize = contactDetails.read("$.totalSize");
        if (contactsSize > 0) {
            // Contact already exists in the salesforce account. Return a card to show the sender information.
            logger.debug("Returning contact info for email: {} ", senderEmail);
            return Async.toSingle(opportunitiesFuture)
                    .map(entity -> createUserDetailsCard(contactDetails, entity.getBody(), routingPrefix));
        } else {
            // Contact doesn't exist in salesforce. Return a card to show accounts that are related to sender domain.
            logger.debug("Returning accounts info for domain: {} ", StringUtils.substringAfterLast(senderEmail, "@"));
            return Async.toSingle(accountsFuture)                                       // run the query
                    .map(entity -> getUniqueAccounts(entity, senderEmail))              // filter the result
                    .flatMap(list -> addRelatedOpportunities(list, sfBaseUrl, headers)) // fetch and add opp to each account.
                    .map(list -> createRelatedAccountsCards(list, senderEmail, routingPrefix));        // convert into Cards
        }
    }

    @PostMapping(path = ADD_CONTACT_PATH, consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public Single<ResponseEntity<Void>> addContact(
            @RequestHeader(name = SALESFORCE_AUTH_HEADER) String sfAuth,
            @RequestHeader(name = SALESFORCE_BASE_URL_HEADER) String sfBaseUrl,
            @PathVariable String accountId,
            @RequestParam(name = "contact_email") String contactEmail,
            @RequestParam(name = "first_name", required = false) String firstName,
            @RequestParam(name = "last_name") String lastName,
            @RequestParam(name = "opportunity_ids", required = false) Set<String> opportunityIds) {

        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, sfAuth);
        headers.set(CONTENT_TYPE, APPLICATION_JSON_VALUE);

        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("AccountId", accountId);
        bodyMap.put("Email", contactEmail);
        bodyMap.put("LastName", lastName);
        if (StringUtils.isNotBlank(firstName)) {
            bodyMap.put("FirstName", firstName);
        }

        logger.debug("Adding contact: {} with Salesforce server: {}", bodyMap, sfBaseUrl);

        UriComponentsBuilder uriComponentsBuilder = fromHttpUrl(sfBaseUrl).path(sfAddContactPath);
        ListenableFuture<ResponseEntity<JsonDocument>> future = rest.exchange(
                uriComponentsBuilder.build().toUri(), HttpMethod.POST, new HttpEntity<>(bodyMap, headers), JsonDocument.class);
        /*
        Once we start using salesforce API version 34, following things can be done in a single network call.
           - Create a new contact
           - Link Opportunities to the contact (from 1-n).
         More : https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/dome_composite_sobject_tree_flat.htm
         */
        return Async.toSingle(future)
                .flatMap(entity -> linkOpportunitiesToContact(entity, opportunityIds, sfBaseUrl, headers))
                .map(entity -> ResponseEntity.status(entity.getStatusCode()).build());
    }

    @PostMapping(path = ADD_CONVERSATIONS_PATH, consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public Single<ResponseEntity<Void>> addEmailConversation(
            @RequestHeader(name = SALESFORCE_AUTH_HEADER) String sfAuth,
            @RequestHeader(name = SALESFORCE_BASE_URL_HEADER) String sfBaseUrl,
            @RequestParam("user_email") String userEmail,
            @RequestParam("contact_email") String contactEmail,
            @RequestParam("email_conversations") String conversations,
            @RequestParam("attachment_name") String attachmentName,
            @RequestParam("opportunity_ids") Set<String> opportunityIds) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, sfAuth);
        byte[] formattedConversations = formatMessages(conversations).getBytes("UTF-8");
        String contactIdSoql = String.format(QUERY_FMT_CONTACT_ID, contactEmail, userEmail);
        UriComponentsBuilder contactQueryUrlBuilder = fromHttpUrl(sfBaseUrl).path(sfSearchAccountPath)
                .queryParam("q", contactIdSoql);
        ListenableFuture<ResponseEntity<JsonDocument>> contactsFuture = rest.exchange(
                contactQueryUrlBuilder.build().toUri(), HttpMethod.GET,
                new HttpEntity<String>(headers), JsonDocument.class);
        return Async.toSingle(contactsFuture).flatMap(entity -> linkContactIdToOpportunity(
                entity, opportunityIds,
                formattedConversations, attachmentName,
                sfBaseUrl, sfAuth)).map(entity ->
                ResponseEntity.status(entity.getStatusCode()).build());
    }

    private Single<ResponseEntity<Map<String, HttpStatus>>> linkContactIdToOpportunity(
            ResponseEntity<JsonDocument> entity,
            Set<String> opportunityIds,
            byte[] conversations,
            String attachmentName,
            String sfBaseUrl, String sfAuth) {
        List<String> contactId = entity.getBody().read("$..Id");
        return Observable.from(opportunityIds)
                .flatMap(opportunityId -> addEmailConversationToOpportunity(
                        contactId.get(0),
                        opportunityId,
                        conversations,
                        attachmentName,
                        sfAuth,
                        sfBaseUrl).toObservable())
                .toMap(Pair::getRight, pair -> pair.getLeft().getStatusCode())
                .map(ResponseEntity::ok)
                .toSingle();
    }

    private Single<Pair<ResponseEntity<Void>, String>> addEmailConversationToOpportunity(
            String contactId,
            String opportunityId,
            byte[] conversation,
            String attachmentName,
            String sfAuth,
            String sfBaseUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, sfAuth);
        UriComponentsBuilder uriComponentsBuilder = fromHttpUrl(sfBaseUrl).path(sfOpportunityTaskLinkPath);
        headers.set(AUTHORIZATION, sfAuth);
        headers.set(CONTENT_TYPE, APPLICATION_JSON_VALUE);
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("WhatId", opportunityId);
        bodyMap.put("Subject", CONVERSATION_TYPE);
        bodyMap.put("WhoId", contactId);
        ListenableFuture<ResponseEntity<JsonDocument>> future = rest.exchange(
                uriComponentsBuilder.build().toUri(), HttpMethod.POST,
                new HttpEntity<>(bodyMap, headers), JsonDocument.class);
        return Async.toSingle(future).flatMap(entity -> linkAttachmentToTask(
                entity, conversation, attachmentName, sfBaseUrl, sfAuth)).
                map(entity -> Pair.of(ResponseEntity.
                        status(entity.getLeft().getStatusCode()).build(), opportunityId));
    }


    private Single<Pair<ResponseEntity<Void>, String>> linkAttachmentToTask(
            ResponseEntity<JsonDocument> entity,
            byte[] conversations,
            String attachmentName,
            String sfBaseUrl,
            String sfAuth) {
        HttpHeaders headers = new HttpHeaders();
        String parentId = entity.getBody().read("$.id");
        headers.set(AUTHORIZATION, sfAuth);
        headers.set(CONTENT_TYPE, APPLICATION_JSON_VALUE);
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("Body", Base64Utils.encodeToString(conversations));
        bodyMap.put("ParentId", parentId);
        bodyMap.put("Name", attachmentName);
        bodyMap.put("ContentType", TEXT_PLAIN_VALUE);
        UriComponentsBuilder uriComponentsBuilder = fromHttpUrl(sfBaseUrl).path(sfAttachmentTasklinkPath);
        ListenableFuture<ResponseEntity<JsonDocument>> future = rest.exchange(
                uriComponentsBuilder.build().toUri(), HttpMethod.POST,
                new HttpEntity<>(bodyMap, headers), JsonDocument.class);
        return Async.toSingle(future).map(attachmentEntity ->
                Pair.of(ResponseEntity.status(attachmentEntity.
                        getStatusCode()).build(), parentId));
    }

    // Link each opportunity to the contact.
    private Single<ResponseEntity<Map<String, HttpStatus>>> linkOpportunitiesToContact(ResponseEntity<JsonDocument> addContactResponse,
                                                                                       Set<String> opportunityIds, String sfBaseUrl,
                                                                                       HttpHeaders headers) {
        if (null == opportunityIds) {
            // No opportunity is available to link
            return Single.just(ResponseEntity.status(addContactResponse.getStatusCode()).build());
        } else {
            String contactId = addContactResponse.getBody().read("$.id");

            return Observable.from(opportunityIds)
                    .flatMap(oppId -> oppToContact(sfBaseUrl, headers, oppId, contactId).toObservable())
                    .toMap(Pair::getRight, pair -> pair.getLeft().getStatusCode())
                    .map(ResponseEntity::ok)
                    .toSingle();
        }
    }

    private Single<Pair<ResponseEntity<Void>, String>> oppToContact(String sfBaseUrl, HttpHeaders headers, String oppId, String contactId) {
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("OpportunityId", oppId);
        bodyMap.put("ContactId", contactId);
        UriComponentsBuilder uriComponentsBuilder = fromHttpUrl(sfBaseUrl).path(sfOpportunityContactLinkPath);
        ListenableFuture<ResponseEntity<String>> oppLinkFuture = rest.exchange(
                uriComponentsBuilder.build().toUri(), HttpMethod.POST, new HttpEntity<>(bodyMap, headers), String.class);
        return Async.toSingle(oppLinkFuture)
                .map(entity -> Pair.of(ResponseEntity.status(entity.getStatusCode()).build(), oppId));
    }

    // Convert the JSON response into a Set of unique Accounts, *excluding* those Accounts that
    // already have the email sender as a Contact
    private List<SFAccount> getUniqueAccounts(ResponseEntity<JsonDocument> result, String senderEmail) {

        // We use these Sets to filter out duplicate entries
        Set<SFAccount> uniqueAccounts = new HashSet<>();
        Set<SFAccount> accountsWithExistingContact = new HashSet<>();

        List<Map<String, Object>> contactRecords = result.getBody().read("$.records");

        for (Map<String, Object> acctRecord : contactRecords) {
            // Get a reusable context for JsonPath parsing
            DocumentContext ctx = JsonPath.parse(acctRecord);

            // Create an object for the Account to which this Contact belongs
            String acctId = ctx.read("$.Account.Id", String.class);
            String acctName = ctx.read("$.Account.Name", String.class);
            SFAccount acct = new SFAccount(acctId, acctName);

            // Add the Account to the set of all Accounts - this is how we filter out duplicate Accounts
            uniqueAccounts.add(acct);

            // If the Contact has the same email address as the sender, then we don't want to prompt the user to add
            // the sender as a new Contact, so we keep a separate Set of those accounts
            String contactEmail = ctx.read("$.Email", String.class);
            if (senderEmail.equalsIgnoreCase(contactEmail)) {
                accountsWithExistingContact.add(acct);
            }
        }

        // Now subtract out the accounts where the contact already exists...
        uniqueAccounts.removeAll(accountsWithExistingContact);

        // and return the remainder in the form of a List
        return new ArrayList<>(uniqueAccounts);
    }

    private Single<List<SFAccount>> addRelatedOpportunities(List<SFAccount> uniqueAccounts, String sfBaseUrl,
                                                            HttpHeaders headers) {
        // Fetch list of opportunities related to each account. Update the account objects with the result.
        return Observable.from(uniqueAccounts)
                .flatMap(sfAccount -> setAccOpportunities(sfAccount, sfBaseUrl, headers).toObservable())
                .toList()
                .toSingle();
    }

    private Single<SFAccount> setAccOpportunities(SFAccount sfAccount, String sfBaseUrl, HttpHeaders headers) {
        String accOppSoql = String.format(QUERY_FMT_ACCOUNT_OPPORTUNITY, sfAccount.getId());
        UriComponentsBuilder accOppQueryUrlBuilder = fromHttpUrl(sfBaseUrl).path(sfSearchAccountPath).queryParam("q", accOppSoql);
        ListenableFuture<ResponseEntity<JsonDocument>> accOppFuture =
                rest.exchange(accOppQueryUrlBuilder.build().toUri(),
                        HttpMethod.GET, new HttpEntity<String>(headers), JsonDocument.class);

        return Async.toSingle(accOppFuture)
                .map(entity -> setAccOpportunities(entity.getBody(), sfAccount));
    }

    private SFAccount setAccOpportunities(JsonDocument accOpportunityResponse, SFAccount sfAccount) {
        List<SFOpportunity> accRelatedOpportunities = new ArrayList<>();
        List<Object> oppObjects = accOpportunityResponse.read("$.records");
        for (Object oppObject : oppObjects) {
            DocumentContext ctx = JsonPath.parse(oppObject);
            String id = ctx.read("$.Id");
            String name = ctx.read("$.Name");
            SFOpportunity sfOpportunity = new SFOpportunity(id, name);
            accRelatedOpportunities.add(sfOpportunity);
        }
        sfAccount.setAccOpportunities(accRelatedOpportunities);
        return sfAccount;
    }


    // Create card for showing information about the email sender, related opportunities.
    private Cards createUserDetailsCard(JsonDocument contactDetails, JsonDocument opportunityDetails, String routingPrefix) {
        Cards cards = new Cards();
        final String GENERAL_CARD_FIELD_TYPE = "GENERAL";
        String contactName = contactDetails.read("$.records[0].Name");
        String contactPhNo = contactDetails.read("$.records[0].MobilePhone");
        String contactAccountName = contactDetails.read("$.records[0].Account.Name");

        CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .setDescription(cardTextAccessor.getMessage("senderinfo.body"));

        CardBodyField.Builder cardFieldBuilder = new CardBodyField.Builder()
                .setTitle(cardTextAccessor.getMessage("senderinfo.name"))
                .setDescription(contactName)
                .setType(GENERAL_CARD_FIELD_TYPE);
        cardBodyBuilder.addField(cardFieldBuilder.build());

        cardFieldBuilder
                .setTitle(cardTextAccessor.getMessage("senderinfo.account"))
                .setDescription(contactAccountName)
                .setType(GENERAL_CARD_FIELD_TYPE);
        cardBodyBuilder.addField(cardFieldBuilder.build());

        cardFieldBuilder
                .setTitle(cardTextAccessor.getMessage("senderinfo.phone"))
                .setDescription(contactPhNo)
                .setType(GENERAL_CARD_FIELD_TYPE);
        cardBodyBuilder.addField(cardFieldBuilder.build());

        // Fill in the opportunity details.
        // Opportunity amount is an optional field in salesforce.
        int totalOpportunities = opportunityDetails.read("$.totalSize");
        for (int oppIndex = 0; oppIndex < totalOpportunities; oppIndex++) {
            String oppName = opportunityDetails.read(String.format("$.records[%d].Opportunity.Name", oppIndex));
            String oppRole = opportunityDetails.read(String.format("$.records[%d].Role", oppIndex));
            String oppProbability = Double.toString(opportunityDetails
                    .read(String.format("$.records[%d].Opportunity.Probability", oppIndex))).concat("%");
            String oppAmount = opportunityDetails.read(String.format("$.records[%d].Opportunity.Amount", oppIndex));
            cardFieldBuilder
                    .setTitle(cardTextAccessor.getMessage("senderinfo.opportunity.title"))
                    .setDescription(oppName)
                    .setType(GENERAL_CARD_FIELD_TYPE);
            cardBodyBuilder.addField(cardFieldBuilder.build());
            cardFieldBuilder
                    .setTitle(cardTextAccessor.getMessage("senderinfo.opportunity.role"))
                    .setDescription(oppRole)
                    .setType(GENERAL_CARD_FIELD_TYPE);
            cardBodyBuilder.addField(cardFieldBuilder.build());
            cardFieldBuilder
                    .setTitle(cardTextAccessor.getMessage("senderinfo.opportunity.probability"))
                    .setDescription(oppProbability)
                    .setType(GENERAL_CARD_FIELD_TYPE);
            cardBodyBuilder.addField(cardFieldBuilder.build());
            if (StringUtils.isNotBlank(oppAmount)) {
                cardFieldBuilder
                        .setTitle(cardTextAccessor.getMessage("senderinfo.opportunity.amount"))
                        .setDescription(oppAmount)
                        .setType(GENERAL_CARD_FIELD_TYPE);
                cardBodyBuilder.addField(cardFieldBuilder.build());
            }
        }

        Card.Builder cardBuilder = new Card.Builder()
                .setName("Salesforce")
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(cardTextAccessor.getMessage("senderinfo.header"), null)
                .setBody(cardBodyBuilder.build());

        cards.getCards().add(cardBuilder.build());
        return cards;
    }

    // Create a Card for each unique account, account related opportunities
    private Cards createRelatedAccountsCards(List<SFAccount> accounts, String contactEmail, String routingPrefix) {
        Cards cards = new Cards();

        for (SFAccount acct : accounts) {

            String acctId = acct.getId();
            String accountName = acct.getName();
            List<SFOpportunity> opportunities = acct.getAccOpportunities();
            String addContactLink = routingPrefix + ADD_CONTACT_PATH.replace("{accountId}", acctId);

            CardAction.Builder actionBuilder = new CardAction.Builder()
                    .setLabel(cardTextAccessor.getActionLabel("addcontact.add"))
                    .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("addcontact.add"))
                    .setActionKey("USER_INPUT")
                    .setUrl(addContactLink)
                    .setType(HttpMethod.POST)
                    .addRequestParam("contact_email", contactEmail);

            CardActionInputField.Builder inputFieldBuilder = new CardActionInputField.Builder();

            inputFieldBuilder.setId("first_name")
                    .setLabel("First name")
                    .setMinLength(1);
            actionBuilder.addUserInputField(inputFieldBuilder.build());

            inputFieldBuilder.setId("last_name")
                    .setLabel("Last name")
                    .setMinLength(1);
            actionBuilder.addUserInputField(inputFieldBuilder.build());

            if (!opportunities.isEmpty()) {  // There exists some opportunities related to this account.
                inputFieldBuilder.setId("opportunity_ids")
                        .setLabel(cardTextAccessor.getMessage("account.opportunity.label"))
                        .setFormat("select")
                        .setMinLength(0);
                for (SFOpportunity sfOpportunity : opportunities) {
                    inputFieldBuilder.addOption(sfOpportunity.getId(), sfOpportunity.getName());
                }
                actionBuilder.addUserInputField(inputFieldBuilder.build());
            }

            Card.Builder cardBuilder = new Card.Builder()
                    .setName("Salesforce")
                    .setTemplate(routingPrefix + "templates/generic.hbs")
                    .setHeader(cardTextAccessor.getMessage("addcontact.header"), null)
                    .setBody(cardTextAccessor.getMessage("addcontact.body", contactEmail, accountName))
                    .addAction(actionBuilder.build());

            cards.getCards().add(cardBuilder.build());
        }
        return cards;
    }

    private String formatMessages(String conversations) throws IOException {
        MessageThread messageThread = MessageThread.parse(conversations);
        List<Message> messageList = messageThread.getMessages();
        StringBuilder sb = messageList.stream().map(this::formatSingleMessage)
                .collect(StringBuilder::new, (container, element) ->
                        container.append(element).append(NEW_LINE), StringBuilder::append);
        return sb.toString();
    }

    private String formatSingleMessage(Message message) {
        StringBuilder fmtMessage = new StringBuilder(SIZE);
        //Adding Sender info
        StringBuilder senderInfo = new StringBuilder();
        senderInfo.append("Sender Name:")
                .append(WHITE_SPACE)
                .append(message.getSender().getFirstName())
                .append(WHITE_SPACE)
                .append(message.getSender().getLastName())
                .append(NEW_LINE);
        //Adding Subject
        StringBuilder subjectInfo = new StringBuilder();
        subjectInfo.append("Subject:")
                .append(message.getSubject())
                .append(NEW_LINE);
        //Adding Recipient info
        StringBuilder recipientsInfo = new StringBuilder();
        recipientsInfo.append(message.getRecipients().stream().
                map(this::getRecipientInfo).
                collect((Supplier<StringBuilder>) StringBuilder::new,
                        (container, element) -> container.append(element)
                                .append(NEW_LINE), StringBuilder::append));
        //Adding Date
        StringBuilder dateInfo = new StringBuilder();
        dateInfo.append("Date:")
                .append(message.getSentDate())
                .append(NEW_LINE);
        //Adding Text Message
        StringBuilder messageInfo = new StringBuilder();
        messageInfo.append("Message:").append(message.getText()).append(NEW_LINE);
        return fmtMessage.append(senderInfo).append(subjectInfo)
                .append(recipientsInfo).append(dateInfo)
                .append(messageInfo).toString();
    }

    private String getRecipientInfo(UserRecord userRecord) {
        StringBuilder recipientInfo = new StringBuilder(SIZE);
        recipientInfo.append("Recipient Name:").append(WHITE_SPACE).append(userRecord.getFirstName())
                .append(WHITE_SPACE)
                .append(userRecord.getLastName())
                .append(NEW_LINE)
                .append("Recipient Email:")
                .append(WHITE_SPACE)
                .append(userRecord.getEmailAddress());
        return recipientInfo.toString();
    }

    // This class is a Memento encapsulating the things we need to know about a Salesforce account.
    // Instances of this class are immutable.
    @AutoProperty
    private static class SFAccount {
        private final String id;
        private final String name;
        private List<SFOpportunity> opportunities;

        SFAccount(String id, String name) {
            this.id = id;
            this.name = name;
        }

        String getId() {
            return id;
        }

        String getName() {
            return name;
        }

        public List<SFOpportunity> getAccOpportunities() {
            return opportunities;
        }

        public void setAccOpportunities(List<SFOpportunity> opportunities) {
            this.opportunities = opportunities;
        }

        @Override
        public boolean equals(Object o) {
            return Pojomatic.equals(this, o);
        }

        @Override
        public int hashCode() {
            return Pojomatic.hashCode(this);
        }

        @Override
        public String toString() {
            return Pojomatic.toString(this);
        }
    }

    private static class SFOpportunity {
        private final String id;
        private final String name;

        public SFOpportunity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
