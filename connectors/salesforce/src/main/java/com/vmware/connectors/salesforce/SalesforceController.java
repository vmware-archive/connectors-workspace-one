/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.salesforce;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.vmware.connectors.common.json.JsonDocument;
import com.vmware.connectors.common.model.Message;
import com.vmware.connectors.common.model.MessageThread;
import com.vmware.connectors.common.model.UserRecord;
import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.*;
import com.vmware.connectors.common.utils.Async;
import com.vmware.connectors.common.utils.CardTextAccessor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.AsyncRestOperations;
import rx.Observable;
import rx.Single;

import javax.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@RestController
public class SalesforceController {

    private static final Logger logger = LoggerFactory.getLogger(SalesforceController.class);

    private static final String SALESFORCE_AUTH_HEADER = "x-salesforce-authorization";
    private static final String SALESFORCE_BASE_URL_HEADER = "x-salesforce-base-url";
    private static final String ROUTING_PREFIX = "x-routing-prefix";
    private static final String ADD_CONVERSATIONS_PATH = "/conversations";
    private static final String CONVERSATION_TYPE = "email";

    // TODO: concatenating strings into a SOQL query like this may provide an avenue for a SOQL-injection attack

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
    public SalesforceController(
            AsyncRestOperations rest,
            CardTextAccessor cardTextAccessor,
            @Value("${sf.searchAccountsPath}") String sfSearchAccountPath,
            @Value("${sf.addContactPath}") String sfAddContactPath,
            @Value("${sf.opportunityContactLinkPath}") String sfOpportunityContactLinkPath,
            @Value("${sf.opportunityTaskLinkPath}") String sfOpportunityTaskLinkPath,
            @Value("${sf.attachmentTasklinkPath}") String sfAttachmentTasklinkPath
    ) {
        this.rest = rest;
        this.cardTextAccessor = cardTextAccessor;
        this.sfSearchAccountPath = sfSearchAccountPath;
        this.sfAddContactPath = sfAddContactPath;
        this.sfOpportunityContactLinkPath = sfOpportunityContactLinkPath;
        this.sfOpportunityTaskLinkPath = sfOpportunityTaskLinkPath;
        this.sfAttachmentTasklinkPath = sfAttachmentTasklinkPath;
    }

    ///////////////////////////////////////////////////////////////////
    // Methods common to both the cards request and the actions
    ///////////////////////////////////////////////////////////////////

    private HttpHeaders makeHeaders(String auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, auth);
        return headers;
    }

    private HttpHeaders makeJsonHeaders(String auth) {
        HttpHeaders headers = makeHeaders(auth);
        headers.set(CONTENT_TYPE, APPLICATION_JSON_VALUE);
        return headers;
    }

    /**
     * Retrieve contact data from Salesforce.
     *
     * @param contactSoql - Specify the SOQL to run
     */
    private Single<ResponseEntity<JsonDocument>> retrieveContacts(
            String auth,
            String baseUrl,
            String contactSoql
    ) {
        return Async.toSingle(
                rest.exchange(
                        makeSearchAccountUri(baseUrl, contactSoql),
                        HttpMethod.GET,
                        new HttpEntity<String>(makeHeaders(auth)),
                        JsonDocument.class
                )
        );
    }

    private URI makeSearchAccountUri(
            String baseUrl,
            String soql
    ) {
        return fromHttpUrl(baseUrl)
                .path(sfSearchAccountPath)
                .queryParam("q", soql)
                .build()
                .toUri();
    }

    private URI makeUri(
            String baseUrl,
            String path
    ) {
        return fromHttpUrl(baseUrl)
                .path(path)
                .build()
                .toUri();
    }

    ///////////////////////////////////////////////////////////////////
    // Cards request methods
    ///////////////////////////////////////////////////////////////////

    @PostMapping(
            path = "/cards/requests",
            consumes = APPLICATION_JSON_VALUE,
            produces = APPLICATION_JSON_VALUE
    )
    public Single<ResponseEntity<Cards>> getCards(
            @RequestHeader(SALESFORCE_AUTH_HEADER) String auth,
            @RequestHeader(SALESFORCE_BASE_URL_HEADER) String baseUrl,
            @RequestHeader(ROUTING_PREFIX) String routingPrefix,
            @Valid @RequestBody CardRequest cardRequest
    ) {
        // Sender email and user email are required, and sender email has to at least have a non-final @ in it
        String sender = cardRequest.getTokenSingleValue("sender_email");
        String user = cardRequest.getTokenSingleValue("user_email");
        logger.debug("Sender email: {} and User email: {} for Salesforce server: {} ", sender, user, baseUrl);
        String senderDomain = '@' + StringUtils.substringAfterLast(sender, "@");
        // TODO: implement a better system of validating domain names than "yup, it's not empty"
        if (StringUtils.isBlank(senderDomain) || StringUtils.isBlank(user)) {
            logger.warn("Either sender email or user email is blank for url: {}", baseUrl);
            return Single.just(new ResponseEntity<>(BAD_REQUEST));
        }

        HttpHeaders headers = makeHeaders(auth);

        return retrieveContactInfos(auth, baseUrl, user, sender)
                .map(HttpEntity::getBody)
                .flatMap(contacts -> getCards(contacts, sender, baseUrl, routingPrefix, headers, user, senderDomain))
                .map(this::toCards)
                .map(ResponseEntity::ok);
    }

    // Retrieve contact name, account name, and phone
    private Single<ResponseEntity<JsonDocument>> retrieveContactInfos(
            String auth,
            String baseUrl,
            String userEmail,
            String senderEmail
    ) {
        String contactSoql = String.format(QUERY_FMT_CONTACT, senderEmail, userEmail);

        return retrieveContacts(auth, baseUrl, contactSoql);
    }


    private Single<List<Card>> getCards(
            JsonDocument contactDetails,
            String senderEmail,
            String baseUrl,
            String routingPrefix,
            HttpHeaders headers,
            String userEmail,
            String senderDomain
    ) {
        int contactsSize = contactDetails.read("$.totalSize");
        if (contactsSize > 0) {
            // Contact already exists in the salesforce account. Return a card to show the sender information.
            logger.debug("Returning contact info for email: {} ", senderEmail);
            return makeCardFromContactDetails(headers, baseUrl, routingPrefix, userEmail, senderEmail, contactDetails)
                    .map(ImmutableList::of);
        } else {
            // Contact doesn't exist in salesforce. Return a card to show accounts that are related to sender domain.
            logger.debug("Returning accounts info for domain: {} ", senderDomain);
            return makeCardsFromSenderDomain(headers, baseUrl, routingPrefix, userEmail, senderEmail, senderDomain);
        }
    }

    private Single<Card> makeCardFromContactDetails(
            HttpHeaders headers,
            String baseUrl,
            String routingPrefix,
            String userEmail,
            String senderEmail,
            JsonDocument contactDetails
    ) {
        return retrieveOpportunities(headers, baseUrl, userEmail, senderEmail)
                .map(entity -> createUserDetailsCard(contactDetails, entity.getBody(), routingPrefix));
    }

    private Single<ResponseEntity<JsonDocument>> retrieveOpportunities(
            HttpHeaders headers,
            String baseUrl,
            String userEmail,
            String senderEmail
    ) {
        String soql = String.format(QUERY_FMT_CONTACT_OPPORTUNITY, senderEmail, userEmail);

        return Async.toSingle(
                rest.exchange(
                        makeSearchAccountUri(baseUrl, soql),
                        HttpMethod.GET,
                        new HttpEntity<String>(headers),
                        JsonDocument.class
                )
        );
    }

    // Create card for showing information about the email sender, related opportunities.
    private Card createUserDetailsCard(
            JsonDocument contactDetails,
            JsonDocument opportunityDetails,
            String routingPrefix
    ) {
        String contactName = contactDetails.read("$.records[0].Name");
        String contactPhNo = contactDetails.read("$.records[0].MobilePhone");
        String contactAccountName = contactDetails.read("$.records[0].Account.Name");

        CardBody.Builder cardBodyBuilder = new CardBody.Builder()
                .setDescription(cardTextAccessor.getMessage("senderinfo.body"))
                .addField(buildGeneralBodyField("senderinfo.name", contactName))
                .addField(buildGeneralBodyField("senderinfo.account", contactAccountName))
                .addField(buildGeneralBodyField("senderinfo.phone", contactPhNo));

        addOpportunities(cardBodyBuilder, opportunityDetails);

        return new Card.Builder()
                .setName("Salesforce") // TODO - remove this in APF-536
                .setTemplate(routingPrefix + "templates/generic.hbs")
                .setHeader(cardTextAccessor.getMessage("senderinfo.header"), null)
                .setBody(cardBodyBuilder.build())
                .build();
    }

    private CardBodyField buildGeneralBodyField(
            String titleMessageKey,
            String description
    ) {
        if (StringUtils.isBlank(description)) {
            return null;
        }
        return new CardBodyField.Builder()
                .setTitle(cardTextAccessor.getMessage(titleMessageKey))
                .setType(CardBodyFieldType.GENERAL)
                .setDescription(description)
                .build();
    }

    private void addOpportunities(
            CardBody.Builder cardBodyBuilder,
            JsonDocument opportunityDetails
    ) {
        // Fill in the opportunity details.
        int totalOpportunities = opportunityDetails.read("$.totalSize");

        for (int oppIndex = 0; oppIndex < totalOpportunities; oppIndex++) {

            String oppJsonPathPrefix = String.format("$.records[%d].", oppIndex);

            String oppName = opportunityDetails.read(oppJsonPathPrefix + "Opportunity.Name");
            String oppRole = opportunityDetails.read(oppJsonPathPrefix + "Role");
            String oppProbability = Double.toString(
                    opportunityDetails.read(oppJsonPathPrefix + "Opportunity.Probability")
            ) + "%";
            String oppAmount = opportunityDetails.read(oppJsonPathPrefix + "Opportunity.Amount");

            cardBodyBuilder
                    .addField(buildGeneralBodyField("senderinfo.opportunity.title", oppName))
                    .addField(buildGeneralBodyField("senderinfo.opportunity.role", oppRole))
                    .addField(buildGeneralBodyField("senderinfo.opportunity.probability", oppProbability))
                    .addField(buildGeneralBodyField("senderinfo.opportunity.amount", oppAmount));
        }
    }

    private Single<List<Card>> makeCardsFromSenderDomain(
            HttpHeaders headers,
            String baseUrl,
            String routingPrefix,
            String userEmail,
            String senderEmail,
            String senderDomain
    ) {
        return retrieveAccountDetails(headers, baseUrl, userEmail, senderDomain)
                .map(ResponseEntity::getBody)
                .map(body -> body.<List<Map<String, Object>>>read("$.records"))
                .map(contactRecords -> getUniqueAccounts(contactRecords, senderEmail))
                .flatMap(accounts -> addRelatedOpportunities(accounts, baseUrl, headers))
                .map(list -> createRelatedAccountsCards(list, senderEmail, routingPrefix));
    }

    private Single<ResponseEntity<JsonDocument>> retrieveAccountDetails(
            HttpHeaders headers,
            String baseUrl,
            String userEmail,
            String senderDomain
    ) {
        String soql = String.format(QUERY_FMT_ACCOUNT, senderDomain, userEmail);

        return Async.toSingle(
                rest.exchange(
                        makeSearchAccountUri(baseUrl, soql),
                        HttpMethod.GET,
                        new HttpEntity<String>(headers),
                        JsonDocument.class
                )
        );
    }

    /**
     * Convert into a Set of unique Accounts, *excluding* those Accounts that
     * already have the email sender as a Contact.
     */
    private List<SFAccount> getUniqueAccounts(
            List<Map<String, Object>> contactRecords,
            String senderEmail
    ) {
        // We use these Sets to filter out duplicate entries
        Set<SFAccount> uniqueAccounts = new HashSet<>();
        Set<SFAccount> accountsWithExistingContact = new HashSet<>();

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

    private Single<List<SFAccount>> addRelatedOpportunities(
            List<SFAccount> uniqueAccounts,
            String baseUrl,
            HttpHeaders headers
    ) {
        // Fetch list of opportunities related to each account. Update the account objects with the result.
        return Observable
                .from(uniqueAccounts)
                .flatMapSingle(account -> supplementAccOpportunities(account, baseUrl, headers))
                .toList()
                .toSingle();
    }

    private Single<SFAccount> supplementAccOpportunities(
            SFAccount account,
            String baseUrl,
            HttpHeaders headers
    ) {
        return retrieveAccountOpportunities(headers, baseUrl, account.getId())
                .map(entity -> setAccOpportunities(entity.getBody(), account));
    }

    private  Single<ResponseEntity<JsonDocument>> retrieveAccountOpportunities(
            HttpHeaders headers,
            String baseUrl,
            String accountId
    ) {
        String soql = String.format(QUERY_FMT_ACCOUNT_OPPORTUNITY, accountId);

        return Async.toSingle(
                rest.exchange(
                        makeSearchAccountUri(baseUrl, soql),
                        HttpMethod.GET,
                        new HttpEntity<String>(headers),
                        JsonDocument.class
                )
        );
    }

    private SFAccount setAccOpportunities(
            JsonDocument accOpportunityResponse,
            SFAccount account
    ) {
        return new SFAccount(
                account,
                accOpportunityResponse.<List<Object>>read("$.records")
                        .stream()
                        .map(JsonPath::parse)
                        .map(ctx -> new SFOpportunity(ctx.read("$.Id"), ctx.read("$.Name")))
                        .collect(Collectors.toList())
        );
    }


    // Create a Card for each unique account, account related opportunities
    private List<Card> createRelatedAccountsCards(
            List<SFAccount> accounts,
            String contactEmail,
            String routingPrefix
    ) {
        return accounts
                .stream()
                .map(acct ->
                        new Card.Builder()
                                .setName("Salesforce")
                                .setTemplate(routingPrefix + "templates/generic.hbs")
                                .setHeader(cardTextAccessor.getMessage("addcontact.header"), null)
                                .setBody(cardTextAccessor.getMessage("addcontact.body", contactEmail, acct.getName()))
                                .addAction(createAddContactAction(routingPrefix, contactEmail, acct))
                                .build()
                )
                .collect(Collectors.toList());
    }

    private CardAction createAddContactAction(
            String routingPrefix,
            String contactEmail,
            SFAccount acct
    ) {
        String acctId = acct.getId();
        String addContactLink = routingPrefix + ADD_CONTACT_PATH.replace("{accountId}", acctId);

        CardAction.Builder actionBuilder = new CardAction.Builder()
                .setLabel(cardTextAccessor.getActionLabel("addcontact.add"))
                .setCompletedLabel(cardTextAccessor.getActionCompletedLabel("addcontact.add"))
                .setActionKey(CardActionKey.USER_INPUT)
                .setUrl(addContactLink)
                .setType(HttpMethod.POST)
                .addRequestParam("contact_email", contactEmail)
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setId("first_name")
                                .setLabel("First name")
                                .setMinLength(1)
                                .build()
                )
                .addUserInputField(
                        new CardActionInputField.Builder()
                                .setId("last_name")
                                .setLabel("Last name")
                                .setMinLength(1).build()
                );

        addOpportunitiesSelectInputField(actionBuilder, acct.getAccOpportunities());

        return actionBuilder.build();
    }

    private void addOpportunitiesSelectInputField(
            CardAction.Builder actionBuilder,
            List<SFOpportunity> opportunities
    ) {
        if (!opportunities.isEmpty()) {  // There exists some opportunities related to this account.
            CardActionInputField.Builder inputFieldBuilder = new CardActionInputField.Builder()
                    .setId("opportunity_ids")
                    .setLabel(cardTextAccessor.getMessage("account.opportunity.label"))
                    .setFormat("select")
                    .setMinLength(0);
            for (SFOpportunity sfOpportunity : opportunities) {
                inputFieldBuilder.addOption(sfOpportunity.getId(), sfOpportunity.getName());
            }
            actionBuilder.addUserInputField(inputFieldBuilder.build());
        }
    }

    private Cards toCards(List<Card> cards) {
        Cards c = new Cards();
        c.getCards().addAll(cards);
        return c;
    }

    ///////////////////////////////////////////////////////////////////
    // Add Contact Action methods
    ///////////////////////////////////////////////////////////////////

    @PostMapping(
            path = ADD_CONTACT_PATH,
            consumes = APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<Void>> addContact(
            @RequestHeader(SALESFORCE_AUTH_HEADER) String auth,
            @RequestHeader(SALESFORCE_BASE_URL_HEADER) String baseUrl,
            @PathVariable("accountId") String accountId,
            @RequestParam("contact_email") String contactEmail,
            @RequestParam(name = "first_name", required = false) String firstName,
            @RequestParam("last_name") String lastName,
            @RequestParam(name = "opportunity_ids", required = false) Set<String> opportunityIds
    ) {
        /*
         * Once we start using salesforce API version 34, following things can be done in a single network call.
         *  - Create a new contact
         *  - Link Opportunities to the contact (from 1-n).
         * More : https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/dome_composite_sobject_tree_flat.htm
         */
        HttpHeaders headers = makeJsonHeaders(auth);

        return addContact(headers, baseUrl, accountId, contactEmail, lastName, firstName)
                .flatMap(entity -> linkOpportunitiesToContact(entity, opportunityIds, baseUrl, headers))
                .map(entity -> ResponseEntity.status(entity.getStatusCode()).build());
    }

    private Single<ResponseEntity<JsonDocument>> addContact(
            HttpHeaders headers,
            String baseUrl,
            String accountId,
            String contactEmail,
            String lastName,
            String firstName
    ) {
        Map<String, String> body = new HashMap<>();

        body.put("AccountId", accountId);
        body.put("Email", contactEmail);
        body.put("LastName", lastName);

        if (StringUtils.isNotBlank(firstName)) {
            body.put("FirstName", firstName);
        }

        logger.debug("Adding contact: {} with Salesforce server: {}", body, baseUrl);

        return Async.toSingle(
                rest.exchange(
                        makeUri(baseUrl, sfAddContactPath),
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        JsonDocument.class
                )
            );
    }

    private Single<ResponseEntity<Void>> linkOpportunitiesToContact(
            ResponseEntity<JsonDocument> addContactResponse,
            Set<String> opportunityIds,
            String baseUrl,
            HttpHeaders headers
    ) {
        if (CollectionUtils.isEmpty(opportunityIds)) {
            // No opportunity is available to link
            return Single.just(ResponseEntity.status(addContactResponse.getStatusCode()).build());
        } else {
            String contactId = addContactResponse.getBody().read("$.id");

            return Observable
                    .from(opportunityIds)
                    .flatMapSingle(opportunityId -> linkOpportunityToContact(baseUrl, headers, opportunityId, contactId))
                    .map(ignored -> ResponseEntity.ok().<Void>build())
                    .toSingle();
        }
    }

    private Single<?> linkOpportunityToContact(
            String baseUrl,
            HttpHeaders headers,
            String opportunityId,
            String contactId
    ) {
        Map<String, String> body = ImmutableMap.of(
                "OpportunityId", opportunityId,
                "ContactId", contactId
        );

        return Async.toSingle(
                rest.exchange(
                        makeUri(baseUrl, sfOpportunityContactLinkPath),
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class
                )
        );
    }

    ///////////////////////////////////////////////////////////////////
    // Add Conversation Action methods
    ///////////////////////////////////////////////////////////////////

    @PostMapping(
            path = ADD_CONVERSATIONS_PATH,
            consumes = APPLICATION_FORM_URLENCODED_VALUE
    )
    public Single<ResponseEntity<Void>> addEmailConversation(
            @RequestHeader(SALESFORCE_AUTH_HEADER) String auth,
            @RequestHeader(SALESFORCE_BASE_URL_HEADER) String baseUrl,
            @RequestParam("user_email") String userEmail,
            @RequestParam("contact_email") String contactEmail,
            @RequestParam("email_conversations") String conversations,
            @RequestParam("attachment_name") String attachmentName,
            @RequestParam("opportunity_ids") Set<String> opportunityIds
    ) throws IOException {

        byte[] formattedConversations = formatConversations(conversations).getBytes(StandardCharsets.UTF_8);

        return retrieveContactIds(auth, baseUrl, userEmail, contactEmail)
                .map(ResponseEntity::getBody)
                .map(body -> body.<List<String>>read("$..Id"))
                .map(contactIds -> contactIds.get(0))
                .flatMap(
                        contactId ->
                                linkContactIdToOpportunity(
                                        contactId,
                                        opportunityIds,
                                        formattedConversations,
                                        attachmentName,
                                        baseUrl,
                                        auth
                                )
                )
                .map(ignored -> ResponseEntity.ok().build());
    }

    private String formatConversations(String conversations) throws IOException {
        return MessageThread
                .parse(conversations)
                .getMessages()
                .stream()
                .map(this::formatSingleMessage)
                .collect(Collectors.joining("\n"));
    }

    private String formatSingleMessage(Message message) {
        return String.format(
                "Sender Name: %s %s\n"
                        + "Subject:%s\n"
                        + "%s\n" // recipients
                        + "Date:%s\n"
                        + "Message:%s\n",
                message.getSender().getFirstName(),
                message.getSender().getLastName(),
                message.getSubject(),
                formatRecipients(message),
                message.getSentDate(),
                message.getText()
        );
    }

    private String formatRecipients(Message message) {
        return message.getRecipients()
                .stream()
                .map(this::formatRecipient)
                .collect(Collectors.joining("\n"));
    }

    private String formatRecipient(UserRecord userRecord) {
        return String.format(
                "Recipient Name: %s %s\nRecipientEmail: %s",
                userRecord.getFirstName(),
                userRecord.getLastName(),
                userRecord.getEmailAddress()
        );
    }

    // Only retrieve the contact ID
    private Single<ResponseEntity<JsonDocument>> retrieveContactIds(
            String auth,
            String baseUrl,
            String userEmail,
            String contactEmail
    ) {
        String contactIdSoql = String.format(QUERY_FMT_CONTACT_ID, contactEmail, userEmail);

        return retrieveContacts(auth, baseUrl, contactIdSoql);
    }

    private Single<?> linkContactIdToOpportunity(
            String contactId,
            Set<String> opportunityIds,
            byte[] conversations,
            String attachmentName,
            String baseUrl,
            String auth
    ) {
        return Observable
                .from(opportunityIds)
                .flatMapSingle(
                        opportunityId ->
                                addEmailConversationToOpportunity(
                                        contactId,
                                        opportunityId,
                                        conversations,
                                        attachmentName,
                                        auth,
                                        baseUrl
                                )
                )
                .toSingle();
    }

    private Single<?> addEmailConversationToOpportunity(
            String contactId,
            String opportunityId,
            byte[] conversation,
            String attachmentName,
            String auth,
            String baseUrl
    ) {
        return retrieveOpportunityTaskLink(auth, baseUrl, opportunityId, contactId)
                .map(ResponseEntity::getBody)
                .map(body -> body.<String>read("$.id"))
                .flatMap(parentId -> linkAttachmentToTask(parentId, conversation, attachmentName, baseUrl, auth));
    }

    private Single<ResponseEntity<JsonDocument>> retrieveOpportunityTaskLink(
            String auth,
            String baseUrl,
            String opportunityId,
            String contactId
    ) {
        HttpHeaders headers = makeJsonHeaders(auth);

        Map<String, String> body = ImmutableMap.of(
                "WhatId", opportunityId,
                "Subject", CONVERSATION_TYPE,
                "WhoId", contactId
        );

        return Async.toSingle(
                rest.exchange(
                        makeUri(baseUrl, sfOpportunityTaskLinkPath),
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        JsonDocument.class
                )
        );
    }

    private Single<?> linkAttachmentToTask(
            String parentId,
            byte[] conversations,
            String attachmentName,
            String baseUrl,
            String auth
    ) {
        HttpHeaders headers = makeJsonHeaders(auth);

        Map<String, String> body = ImmutableMap.of(
                "Body", Base64Utils.encodeToString(conversations),
                "Name", attachmentName,
                "ParentId", parentId,
                "ContentType", TEXT_PLAIN_VALUE
        );

        return Async.toSingle(
                rest.exchange(
                        makeUri(baseUrl, sfAttachmentTasklinkPath),
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        String.class
                )
        );
    }

}
