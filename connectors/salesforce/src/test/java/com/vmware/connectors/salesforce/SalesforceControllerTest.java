/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.salesforce;


import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SalesforceControllerTest extends ControllerTestsBase {

    private static final String QUERY_FMT_ACCOUNT =
            "SELECT email, account.id, account.name FROM contact WHERE email LIKE '%%%s' AND account.owner.email = '%s'";
    private static final String QUERY_FMT_CONTACT =
            "SELECT name, account.name, MobilePhone FROM contact WHERE email = '%s'";

    private static final String QUERY_FMT_ACCOUNT_OPPORTUNITY = "SELECT id, name FROM opportunity WHERE account.id = '%s'";

    private static final String QUERY_FMT_CONTACT_OPPORTUNITY = "SELECT Opportunity.Id FROM OpportunityContactRole " +
            "WHERE contact.email = '%s' AND Opportunity.StageName NOT IN ('Closed Lost', 'Closed Won')";

    private static final String QUERY_FMT_OPPORTUNITY_INFO = "SELECT id, name, CloseDate, NextStep, StageName, " +
            "Account.name, Account.Owner.Name, FORMAT(Opportunity.amount), FORMAT(Opportunity.ExpectedRevenue), (SELECT User.Email from OpportunityTeamMembers), " +
            "(SELECT InsertedBy.Name, Body from Feeds) FROM opportunity WHERE opportunity.id IN ('%s')";

    private static final String QUERY_FMT_CONTACT_ID =
            "SELECT id FROM contact WHERE email = '%s' AND contact.owner.email = '%s'";

    private static final String QUERY_FMT_SE_ACTIVITY = "SELECT AW_Account_Issues__c, AW_Product_Issues__c, AW_Sales_Engineer_Description__c," +
            "  AW_SE_Manual_Override__c, AW_SE_Stage__c, AW_SE_Status__c, Id, Name, Account.Name, Account.Owner.Name," +
            " (SELECT User.Email from OpportunityTeamMembers WHERE User.Email = '%s') FROM Opportunity WHERE StageName NOT IN  ('Closed Lost', 'Closed Won') AND Id IN ('%s')";

    private static final String TRAVIS_ACCOUNT_ID = "0014100000Vc2iPAAR";

    private static final String SOQL_QUERY_PATH = "/services/data/v39.0/query";

    private static final String ADD_CONTACT_PATH = "/services/data/v39.0/sobjects/Contact";
    private static final String LINK_OPPORTUNITY_TASK_PATH = "/services/data/v39.0/sobjects/Task";
    private static final String LINK_ATTACHMENT_TASK_PATH = "/services/data/v39.0/sobjects/Attachment";
    private static final String LINK_OPPORTUNITY_PATH = "/services/data/v39.0/sobjects/OpportunityContactRole";
    private static final String UPDATE_OPPORTUNITY_PATH = "/services/data/v39.0/sobjects/Opportunity/0067F00000BplCHQAZ";

    @Value("classpath:salesforce/response/successContact.json")
    private Resource sfResponseContactExists;

    @Value("classpath:salesforce/response/successContactOpportunityIds.json")
    private Resource sfResponseContactOppIds;

    @Value("classpath:salesforce/response/successContactOpportunityInfo.json")
    private Resource sfResponseContactOppInfo;

    @Value("classpath:salesforce/response/successContactWithoutPhone.json")
    private Resource sfResponseContactWithoutPhone;

    @Value("classpath:salesforce/response/zeroRecords.json")
    private Resource sfResponseZeroRecords;

    @Value("classpath:salesforce/response/successAccount.json")
    private Resource sfResponseAccounts;

    @Value("classpath:salesforce/response/newContactCreated.json")
    private Resource sfResponseContactCreated;

    @Value("classpath:salesforce/response/accGeorceMichaelOpportunities.json")
    private Resource sfResponseGeorgeMichaelOpportunities;

    @Value("classpath:salesforce/response/accLeoDCaprioOpportunities.json")
    private Resource sfResponseLeoDCaprioOpportunities;

    @Value("classpath:salesforce/response/accWordHowardOpportunities.json")
    private Resource sfResponseWordHowardOpportunities;

    @Value("classpath:salesforce/response/newTaskCreated.json")
    private Resource sfResponseTaskCreated;

    @Value("classpath:salesforce/response/newAttachmentCreated.json")
    private Resource sfResponseAttachmentCreated;

    @Value("classpath:salesforce/response/existingContactId.json")
    private Resource sfExistingContactId;

    @Value("classpath:salesforce/response/opportunitiesWithCustomFields.json")
    private Resource sfOpportunitiesWithCustomFields;

    @ParameterizedTest
    @ValueSource(strings = {
            "/cards/requests",
            "/accounts/" + TRAVIS_ACCOUNT_ID + "/contacts"})
    void testProtectedResource(String uri) throws Exception {
        testProtectedResource(POST, uri);
    }

    @Test
    void testDiscovery() throws Exception {
        testConnectorDiscovery();
    }

    @Test
    void testRegex() throws Exception {
        // https://blogs.msdn.microsoft.com/testing123/2009/02/06/email-address-test-cases/
        List<String> expected = ImmutableList.of(
                // VALID
                "email001@domain.com",
                "firstname.lastname002@domain.com",
                "email003@subdomain.domain.com",
                "firstname+lastname004@domain.com",
                // TODO - the next 3 are valid apparently, but not worth fussing over
//                "email005@123.123.123.123",
//                "email006@[123.123.123.123]",
//                "\"email007\"@domain.com",
                "1234567890008@domain.com",
                "email009@domain-one.com",
                "_______010@domain.com",
                "email011@domain.name",
                "email012@domain.co.jp",
                "firstname-lastname013@domain.com",

                // INVALID? (in 2009 at least)
//                "plainaddress014",
//                "#@%^%#$@#$015@#.com",
//                "@016domain.com",
                "email017@domain.com",
//                "email018.domain.com",
                "domain019@domain.com",
                ".email020@domain.com", // Not ideal, but not a big deal
                "email021.@domain.com", // Not ideal, but not a big deal
                "email..email022@domain.com", // Not ideal, but not a big deal
//                "023あいうえお@domain.com",
                "email024@domain.com",
//                "email025@domain",
                "email026@-domain.com", // Not ideal, but not a big deal
                "email027@domain.web",
//                "email028@111.222.333.44444",
                "email029@domain..com" // Not ideal, but not a big deal
        );
        testRegex("sender_email", fromFile("/regex/email.txt"), expected);
    }

    @Test
    void testMissingRequestHeaders() throws Exception {
        webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-routing-prefix", "https://hero/connectors/salesforce/")
                .syncBody(fromFile("/connector/requests/request.json"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Missing request header 'x-salesforce-authorization' for method parameter of type String");
    }

    @DisplayName("Card request invalid token cases")
    @ParameterizedTest(name = "{index} ==> ''{0}''")
    @CsvSource({"/connector/requests/emptyRequest.json, connector/responses/emptyRequest.json",
            "/connector/requests/emptyToken.json, connector/responses/emptyToken.json"})
    void testRequestCardsInvalidTokens(String reqFile, String resFile) throws Exception {
       requestCards("abc", reqFile)
               .exchange()
               .expectStatus().isBadRequest()
               .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON_UTF8)
               .expectBody().json(fromFile(resFile));
    }

    // Assuming no opportunities found related to the email sender.
    @DisplayName("Card request contact found without Opportunities.")
    @ParameterizedTest(name = "{index} ==> Response=''{2}'', Language=''{3}''")
    @MethodSource("contactCardTestArgProvider")
    void testRequestCardSuccess(Resource contactResponse, String resFile, String lang) throws Exception {
        final String requestFile = "/connector/requests/request.json";
        expectSalesforceRequest(getContactRequestSoql(requestFile))
                .andRespond(withSuccess(contactResponse, APPLICATION_JSON));

        expectSalesforceRequest(String.format(QUERY_FMT_CONTACT_OPPORTUNITY, "john.doe@abc.com"))
                .andRespond(withSuccess(sfResponseZeroRecords, APPLICATION_JSON));

        testRequestCards(requestFile, resFile, lang);
     }

    private Stream<Arguments> contactCardTestArgProvider() {
        return Stream.of(
                Arguments.of(sfResponseContactWithoutPhone, "successWithoutPhone.json", null),
                Arguments.of(sfResponseContactExists, "successSenderDetails.json", null),
                Arguments.of(sfResponseContactExists, "successSenderDetails_xx.json", "xx")
        );
    }

    // There are multiple opportunities found related to the email sender.
    @DisplayName("Card request contact found with Opportunities.")
    @ParameterizedTest
    @CsvSource({
            "successCardsForSender.json, " + StringUtils.EMPTY,
            "successCardsForSender_xx.json, xx"})
    void testRequestCardSuccess(String resFile, String lang) throws Exception {
        testCardRequestSuccess(resFile, lang);
    }

    @DisplayName("Card request sender related accounts cases")
    @ParameterizedTest(name = "{index} ==> Language=''{1}''")
    @CsvSource({
            "successRelatedAccounts.json, " + StringUtils.EMPTY,
            "successRelatedAccounts_xx.json, xx"})
    void testRequestCardRelatedAccountsSuccess(String resFile, String lang) throws Exception {
        /* In this case email sender details are not present in salesforce.
        Collect info about the accounts related to the sender's domain. */
        final String requestFile = "/connector/requests/request.json";
        expectSalesforceRequest(getContactRequestSoql(requestFile))
                .andRespond(withSuccess(sfResponseZeroRecords, APPLICATION_JSON));
        expectSalesforceRequest(getAccountRequestSoql(requestFile))
                .andRespond(withSuccess(sfResponseAccounts, APPLICATION_JSON));

        // Opportunity requests for each account.
        expectSalesforceRequest(getAccountOpportunitySoql("001Q0000012gRPoIAM"))
                .andRespond(withSuccess(sfResponseGeorgeMichaelOpportunities, APPLICATION_JSON));
        expectSalesforceRequest(getAccountOpportunitySoql("001Q0000012gkPHIAY"))
                .andRespond(withSuccess(sfResponseLeoDCaprioOpportunities, APPLICATION_JSON));
        expectSalesforceRequest(getAccountOpportunitySoql("001Q0000012glcuIAA"))
                .andRespond(withSuccess(sfResponseWordHowardOpportunities, APPLICATION_JSON));

        testRequestCards(requestFile, resFile, lang);
     }

    @Test
    void testRequestCardNotAuthorized() throws Exception {
        mockBackend.expect(manyTimes(), requestTo(any(String.class)))
                .andExpect(header(AUTHORIZATION, "Bearer bogus"))
                .andExpect(method(GET))
                .andRespond(withUnauthorizedRequest());

        requestCards("bogus", "/connector/requests/request.json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401")
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("/connector/responses/invalid_connector_token.json"));
    }

    @Test
    void testRequestCardInternalServerError() throws Exception {
        final String requestFile = "/connector/requests/request.json";
        mockBackend.expect(manyTimes(), requestTo(any(String.class)))
                .andExpect(method(GET))
                .andRespond(withServerError());
        requestCards("abc", requestFile)
                .exchange()
                .expectStatus().isEqualTo(INTERNAL_SERVER_ERROR)
                .expectHeader().valueEquals("X-Backend-Status", "500");
    }

    @Test
    void testAddContact() throws Exception {
        mockBackend.expect(requestTo(ADD_CONTACT_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.AccountId", is(TRAVIS_ACCOUNT_ID)))
                .andExpect(MockRestRequestMatchers.jsonPath("$.Email", is("test@email.com")))
                .andExpect(MockRestRequestMatchers.jsonPath("$.LastName", is("k")))
                .andExpect(MockRestRequestMatchers.jsonPath("$.FirstName", is("prabhu")))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andRespond(withSuccess(sfResponseContactCreated, APPLICATION_JSON));
        mockBackend.expect(requestTo(LINK_OPPORTUNITY_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andRespond(withSuccess());
        requestAddContact("abc", TRAVIS_ACCOUNT_ID, "/salesforce/request/contact.txt")
                .expectStatus().isOk();
    }

    @Test
    void testAddConversations() throws Exception {
        Map<String, String> userContactDetailMap = getUserContactDetails("/salesforce/request/conversations.txt");
        expectSalesforceRequest(String.format(QUERY_FMT_CONTACT_ID, userContactDetailMap.get("contact_email"),
                userContactDetailMap.get("user_email")))
                .andRespond(withSuccess(sfExistingContactId, APPLICATION_JSON));
        mockBackend.expect(requestTo(LINK_OPPORTUNITY_TASK_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andRespond(withSuccess(sfResponseTaskCreated, APPLICATION_JSON));
        mockBackend.expect(requestTo(LINK_ATTACHMENT_TASK_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andRespond(withSuccess(sfResponseAttachmentCreated, APPLICATION_JSON));
        requestAddConversationAsAttcahment("abc", "/salesforce/request/conversations.txt")
                .expectStatus().isOk();
    }

    @Test
    void testGetImage() {
        webClient.get()
                .uri("/images/connector.png")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentLength(7314)
                .expectHeader().contentType(IMAGE_PNG)
                .expectBody().consumeWith(result -> assertThat(
                        result.getResponseBody(), equalTo(bytesFromFile("/static/images/connector.png"))));
    }

    // There are multiple opportunities found related to the email sender.
    @DisplayName("Card request contact found with opportunities and custom fields related to opportunities.")
    @ParameterizedTest
    @CsvSource({
            "successCardsForSender.json, " + StringUtils.EMPTY,
            "successCardsForSender_xx.json, xx"})
    void cardRequestWithOpportunityIds(String resFile, String lang) throws Exception {
        final String requestFile = "/connector/requests/requestUber.json";

        expectSalesforceRequest(getContactRequestSoql(requestFile))
                .andRespond(withSuccess(sfResponseContactExists, APPLICATION_JSON));

        expectSalesforceRequest(String.format(QUERY_FMT_CONTACT_OPPORTUNITY, "travis@uber.com"))
                .andRespond(withSuccess(sfResponseContactOppIds, APPLICATION_JSON));

        expectSalesforceRequest(String.format(QUERY_FMT_OPPORTUNITY_INFO, "0064100000BU5dOAAT\', \'0064100000O93EVAAZ"))
                .andRespond(withSuccess(sfResponseContactOppInfo, APPLICATION_JSON));

//        expectSalesforceRequest(String.format(QUERY_FMT_SE_ACTIVITY, "jjeff@vmware.com", "0064100000O9DnXAAV\', \'0064100000O93EVAAZ"))
//                .andRespond(withSuccess(sfOpportunitiesWithCustomFields, APPLICATION_JSON));

        testRequestCards("/connector/requests/requestUber.json", resFile, lang);
    }

    private void testCardRequestSuccess(String resFile, String lang) throws Exception {
        final String requestFile = "/connector/requests/requestUber.json";

        expectSalesforceRequest(getContactRequestSoql(requestFile))
                .andRespond(withSuccess(sfResponseContactExists, APPLICATION_JSON));

        expectSalesforceRequest(String.format(QUERY_FMT_CONTACT_OPPORTUNITY, "travis@uber.com"))
                .andRespond(withSuccess(sfResponseContactOppIds, APPLICATION_JSON));

        expectSalesforceRequest(String.format(QUERY_FMT_OPPORTUNITY_INFO, "0064100000BU5dOAAT\', \'0064100000O93EVAAZ"))
                .andRespond(withSuccess(sfResponseContactOppInfo, APPLICATION_JSON));

        testRequestCards(requestFile, resFile, lang);
    }

    @DisplayName("Update Salesforce opportunity fields")
    @ParameterizedTest(name = "{index} => uri={0}, body={1}")
    @CsvSource({
            "/opportunity/0067F00000BplCHQAZ/closedate, closedate=2017-06-03",
            "/opportunity/0067F00000BplCHQAZ/nextstep, nextstep=3/31 - Customer was shown the roadmap for ABC product"
    })
    void updateOpportunityFields(final String uri, final String body) {
        mockSalesforceOpportunityAPI();

        webClient.post()
                .uri(uri)
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header("x-salesforce-authorization", "Bearer abc")
                .header("x-salesforce-base-url", mockBackend.url(""))
                .syncBody(body)
                .exchange()
                .expectStatus().isOk();
    }

    @DisplayName("Update VmWare Salesforce opportunity custom fields")
    @ParameterizedTest(name = "{index} => uri={0}, body={1}")
    @CsvSource({
            "/opportunity/vmw/AW_SE_Stage__c/0067F00000BplCHQAZ, custom_field_value=Contact customer for further details",
            "/opportunity/vmw/AW_SE_Status__c/0067F00000BplCHQAZ, custom_field_value=Opportunity Won",
            "/opportunity/vmw/AW_Account_Issues__c/0067F00000BplCHQAZ, custom_field_value=Sales lead to take decision.",
            "/opportunity/vmw/AW_Product_Issues__c/0067F00000BplCHQAZ, custom_field_value=Some features are not available in the product.",
            "/opportunity/vmw/AW_Sales_Engineer_Description__c/0067F00000BplCHQAZ, custom_field_value=5/30- Testing replacement of competitor companies.",
    })
    void updateOpportunityCustomFields(final String uri, final String body) {
        mockSalesforceOpportunityAPI();

        webClient.post()
                .uri(uri)
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header("x-salesforce-authorization", "Bearer abc")
                .header("x-salesforce-base-url", mockBackend.url(""))
                .syncBody(body)
                .exchange()
                .expectStatus().isOk();
    }

    private void mockSalesforceOpportunityAPI() {
        mockBackend.expect(requestTo(UPDATE_OPPORTUNITY_PATH))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andExpect(header(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess());
    }

    private void testRequestCards(String requestFile, String responseFile, String acceptLanguage) throws Exception {
        WebTestClient.RequestHeadersSpec<?> spec = requestCards("abc", requestFile);
        if (acceptLanguage != null) {
            spec = spec.header(ACCEPT_LANGUAGE, acceptLanguage);
        }
        String body = spec.exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block();
        assertThat(body, sameJSONAs(fromFile("connector/responses/" + responseFile)).allowingAnyArrayOrdering());
    }

    private WebTestClient.RequestHeadersSpec<?> requestCards(String authToken, String filePath) throws Exception {
        return webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-salesforce-authorization", "Bearer " + authToken)
                .header("x-salesforce-base-url", mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/salesforce/")
                .headers(ControllerTestsBase::headers)
                .syncBody(fromFile(filePath));
    }

    private WebTestClient.ResponseSpec requestAddContact(String authToken, String accountId, String filePath) throws Exception {
        return webClient.post()
                .uri(String.format("/accounts/%s/contacts", accountId))
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .accept(APPLICATION_JSON)
                .header("x-salesforce-authorization", "Bearer " + authToken)
                .header("x-salesforce-base-url", mockBackend.url(""))
                .syncBody(fromFile(filePath))
                .exchange();
    }

    private WebTestClient.ResponseSpec requestAddConversationAsAttcahment(String authToken, String filePath) throws Exception {
        return webClient.post()
                .uri("/conversations")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .accept(APPLICATION_JSON)
                .header("x-salesforce-authorization", "Bearer " + authToken)
                .header("x-salesforce-base-url", mockBackend.url(""))
                .syncBody(fromFile(filePath))
                .exchange();
    }

    private ResponseActions expectSalesforceRequest(String soqlQuery) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(SOQL_QUERY_PATH).queryParam("q", soqlQuery);
        URI tmp = builder.build().toUri();
        return mockBackend.expect(requestTo(tmp))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc"));
    }

    private String getContactRequestSoql(String filePath) throws IOException {
        DocumentContext ctx = JsonPath.parse(fromFile(filePath));
        return String.format(QUERY_FMT_CONTACT, senderEmail(ctx));
    }

    private String getAccountRequestSoql(String filePath) throws IOException {
        DocumentContext ctx = JsonPath.parse(fromFile(filePath));
        final String senderDomain = '@' + StringUtils.substringAfterLast(senderEmail(ctx), "@");
        return String.format(QUERY_FMT_ACCOUNT, senderDomain, userEmail(ctx));
    }

    private String senderEmail(DocumentContext ctx) {
        return ctx.read("$.tokens.sender_email[0]");
    }

    private String userEmail(DocumentContext ctx) {
        return ctx.read("$.tokens.user_email[0]");
    }


    private Map<String, String> getUserContactDetails(String filePath) throws IOException {
        List<String> tokens = Arrays.asList(fromFile(filePath).split("\\&"));
        return tokens.stream().filter(string -> string.contains("_email")).
                map(string -> Pair.of(string.split("=")[0], string.split("=")[1])).
                collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    // SOQL for finding list of Opportunities related to an account.
    private String getAccountOpportunitySoql(String accountId) throws IOException {
        return String.format(QUERY_FMT_ACCOUNT_OPPORTUNITY, accountId);
    }
}
