/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.salesforce;


import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.vmware.connectors.mock.MockRestServiceServer;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonReplacementsBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vmware.connectors.test.JsonSchemaValidator.isValidHeroCardConnectorResponse;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SalesforceControllerTest extends ControllerTestsBase {

    private static final String QUERY_FMT_ACCOUNT =
            "SELECT email, account.id, account.name FROM contact WHERE email LIKE '%%%s' AND account.owner.email = '%s'";
    private static final String QUERY_FMT_CONTACT =
            "SELECT name, account.name, MobilePhone FROM contact WHERE email = '%s' AND contact.owner.email = '%s'";
    private static final String QUERY_FMT_OPPORTUNITY =
            "SELECT Opportunity.name, role, FORMAT(Opportunity.amount), Opportunity.probability from OpportunityContactRole" +
                    " WHERE contact.email='%s' AND opportunity.owner.email='%s'";
    private static final String QUERY_FMT_ACCOUNT_OPPORTUNITY = "SELECT id, name FROM opportunity WHERE account.id = '%s'";

    private static final String QUERY_FMT_CONTACT_ID =
            "SELECT id FROM contact WHERE email = '%s' AND contact.owner.email = '%s'";


    private static final String SERVER_URL = "https://acme.salesforce.com";

    private static final String ACCOUNT_SEARCH_PATH = "services/data/v20.0/query";

    private static final String ADD_CONTACT_PATH = "services/data/v20.0/sobjects/Contact";
    private static final String LINK_OPPORTUNITY_TASK_PATH = "services/data/v20.0/sobjects/Task";
    private static final String LINK_ATTACHMENT_TASK_PATH = "services/data/v20.0/sobjects/Attachment";
    private static final String LINK_OPPORTUNITY_PATH = "services/data/v20.0/sobjects/OpportunityContactRole";

    @Value("classpath:salesforce/response/successContact.json")
    private Resource sfResponseContactExists;

    @Value("classpath:salesforce/response/successContactWithoutPhone.json")
    private Resource sfResponseContactWithoutPhone;

    @Value("classpath:salesforce/response/zeroRecords.json")
    private Resource sfResponseContactDoesNotExist;

    @Value("classpath:salesforce/response/successOpportunity.json")
    private Resource sfResponseContactOpportunities;

    @Value("classpath:salesforce/response/successAccount.json")
    private Resource sfResponseAccounts;

    @Value("classpath:salesforce/response/successWithoutAmount.json")
    private Resource sfResponseWithoutAmount;

    @Value("classpath:salesforce/response/successWithoutRole.json")
    private Resource sfResponseWithoutRole;

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


    private MockRestServiceServer mockSF;

    @BeforeEach
    void init() throws Exception {
        super.setup();
        mockSF = MockRestServiceServer.bindTo(requestHandlerHolder).ignoreExpectOrder(true).build();
    }

    @Test
    void testProtectedResource() throws Exception {
        testProtectedResource(POST, "/cards/requests");
    }

    @Test
    void testDiscovery() throws Exception {
        testConnectorDiscovery();
    }

    @Test
    void testMissingRequestHeaders() throws Exception {
        perform(post("/cards/requests").with(token(accessToken()))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-routing-prefix", "https://hero/connectors/salesforce/")
                .content(fromFile("/connector/requests/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("Missing request header 'x-salesforce-authorization'")));
    }

    @DisplayName("Card request invalid token cases")
    @ParameterizedTest(name = "{index} ==> ''{0}''")
    @CsvSource({"/connector/requests/emptyRequest.json, connector/responses/emptyRequest.json",
            "/connector/requests/emptyToken.json, connector/responses/emptyToken.json"})
    void testRequestCardsInvalidTokens(String reqFile, String resFile) throws Exception {
        perform(requestCards("abc", reqFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_UTF8))
                .andExpect(content().json(fromFile(resFile)));
    }

    @DisplayName("Card request contact details cases")
    @ParameterizedTest(name = "{index} ==> Response=''{2}'', Language=''{3}''")
    @MethodSource("contactCardTestArgProvider")
    void testRequestCardContactDetailsSuccess(Resource contactResponse, Resource contactOppResponse,
                                              String resFile, String lang) throws Exception {
        final String requestFile = "/connector/requests/request.json";
        expectSalesforceRequest(getContactRequestSoql(requestFile))
                .andRespond(withSuccess(contactResponse, APPLICATION_JSON));
        expectSalesforceRequest(getContactOpportunitySoql(requestFile))
                .andRespond(withSuccess(contactOppResponse, APPLICATION_JSON));

        testRequestCards(requestFile, resFile, lang);
        mockSF.verify();
    }

    private Stream<Arguments> contactCardTestArgProvider() {
        return Stream.of(
                Arguments.of(sfResponseContactExists, sfResponseWithoutAmount, "successWithoutAmount.json", null),
                Arguments.of(sfResponseContactWithoutPhone, sfResponseWithoutRole, "successWithoutPhoneAndRole.json", null),
                Arguments.of(sfResponseContactExists, sfResponseContactOpportunities, "successSenderDetails.json", null),
                Arguments.of(sfResponseContactExists, sfResponseContactOpportunities, "successSenderDetails_xx.json", "xx")
        );
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
                .andRespond(withSuccess(sfResponseContactDoesNotExist, APPLICATION_JSON));
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
        mockSF.verify();
    }

    @Test
    void testRequestCardNotAuthorized() throws Exception {
        mockSF.expect(manyTimes(), requestTo(any(String.class)))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer bogus"))
                .andExpect(method(GET))
                .andRespond(withUnauthorizedRequest());

        perform(requestCards("bogus", "/connector/requests/request.json"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Backend-Status", "401"))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(fromFile("/connector/responses/invalid_connector_token.json")));
        ;
        mockSF.verify();
    }

    @Test
    void testRequestCardInternalServerError() throws Exception {
        final String requestFile = "/connector/requests/request.json";
        mockSF.expect(manyTimes(), requestTo(any(String.class)))
                .andExpect(method(GET))
                .andRespond(withServerError());
        perform(requestCards("abc", requestFile))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Backend-Status", "500"));
        mockSF.verify();
    }

    @Test
    void testAddContact() throws Exception {
        UriComponentsBuilder addContactReqBuilder = fromHttpUrl(SERVER_URL).path(ADD_CONTACT_PATH);
        UriComponentsBuilder addOppToContactReqBuilder = fromHttpUrl(SERVER_URL).path(LINK_OPPORTUNITY_PATH);
        mockSF.expect(requestTo(addContactReqBuilder.build().toUri()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andRespond(withSuccess(sfResponseContactCreated, APPLICATION_JSON));
        mockSF.expect(requestTo(addOppToContactReqBuilder.build().toUri()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andRespond(withSuccess());
        perform(requestAddContact("abc", "0014100000Vc2iPAAR", "/salesforce/request/contact.txt"))
                .andExpect(status().isOk());
        mockSF.verify();
    }

    @Test
    void testAddConversations() throws Exception {
        UriComponentsBuilder addTaskReqBuilder = fromHttpUrl(SERVER_URL).path(LINK_OPPORTUNITY_TASK_PATH);
        UriComponentsBuilder addAttachmentReqBuilder = fromHttpUrl(SERVER_URL).path(LINK_ATTACHMENT_TASK_PATH);
        Map<String, String> userContactDetailMap = getUserContactDetails("/salesforce/request/conversations.txt");
        expectSalesforceRequest(String.format(QUERY_FMT_CONTACT_ID, userContactDetailMap.get("contact_email"),
                userContactDetailMap.get("user_email")))
                .andRespond(withSuccess(sfExistingContactId, APPLICATION_JSON));
        mockSF.expect(requestTo(addTaskReqBuilder.build().toUri()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andRespond(withSuccess(sfResponseTaskCreated, APPLICATION_JSON));
        mockSF.expect(requestTo(addAttachmentReqBuilder.build().toUri()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andRespond(withSuccess(sfResponseAttachmentCreated, APPLICATION_JSON));
        ;
        perform(requestAddConversationAsAttcahment("abc", "/salesforce/request/conversations.txt"))
                .andExpect(status().isOk());
        mockSF.verify();
    }

    @Test
    void testGetImage() throws Exception {
        perform(get("/images/connector.png"))
                .andExpect(status().isOk())
                .andExpect(header().longValue(CONTENT_LENGTH, 7314))
                .andExpect(header().string(CONTENT_TYPE, IMAGE_PNG_VALUE))
                .andExpect((content().bytes(bytesFromFile("/static/images/connector.png"))));
    }

    private void testRequestCards(String requestFile, String responseFile, String acceptLanguage) throws Exception {
        MockHttpServletRequestBuilder builder = requestCards("abc", requestFile);
        if (acceptLanguage != null) {
            builder = builder.header(ACCEPT_LANGUAGE, acceptLanguage);
        }
        perform(builder)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().string(isValidHeroCardConnectorResponse()))
                .andExpect(content().string(JsonReplacementsBuilder.from(
                        fromFile("connector/responses/" + responseFile)).buildForCards()));
    }

    private MockHttpServletRequestBuilder requestCards(String authToken, String filePath) throws Exception {
        return post("/cards/requests").with(token(accessToken()))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-salesforce-authorization", "Bearer " + authToken)
                .header("x-salesforce-base-url", SERVER_URL)
                .header("x-routing-prefix", "https://hero/connectors/salesforce/")
                .content(fromFile(filePath));
    }

    private MockHttpServletRequestBuilder requestAddContact(String authToken, String accountId, String filePath) throws Exception {
        return post(String.format("/accounts/%s/contacts", accountId)).with(token(accessToken()))
                .contentType(APPLICATION_FORM_URLENCODED)
                .accept(APPLICATION_JSON)
                .header("x-salesforce-authorization", "Bearer " + authToken)
                .header("x-salesforce-base-url", SERVER_URL).content(fromFile(filePath));
    }

    private MockHttpServletRequestBuilder requestAddConversationAsAttcahment(String authToken, String filePath) throws Exception {
        return post("/conversations").with(token(accessToken()))
                .contentType(APPLICATION_FORM_URLENCODED)
                .accept(APPLICATION_JSON)
                .header("x-salesforce-authorization", "Bearer " + authToken)
                .header("x-salesforce-base-url", SERVER_URL).content(fromFile(filePath));
    }

    private ResponseActions expectSalesforceRequest(String soqlQuery) throws IOException {
        UriComponentsBuilder builder = fromHttpUrl(SERVER_URL).path(ACCOUNT_SEARCH_PATH).queryParam("q", soqlQuery);
        URI tmp = builder.build().toUri();
        return mockSF.expect(requestTo(tmp))
                .andExpect(method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, "Bearer abc"));
    }

    private String getContactRequestSoql(String filePath) throws IOException {
        DocumentContext ctx = JsonPath.parse(fromFile(filePath));
        return String.format(QUERY_FMT_CONTACT, senderEmail(ctx), userEmail(ctx));
    }

    private String getAccountRequestSoql(String filePath) throws IOException {
        DocumentContext ctx = JsonPath.parse(fromFile(filePath));
        final String senderDomain = '@' + StringUtils.substringAfterLast(senderEmail(ctx), "@");
        return String.format(QUERY_FMT_ACCOUNT, senderDomain, userEmail(ctx));
    }

    // SOQL for finding all opportunities related to the sender contact.
    private String getContactOpportunitySoql(String filePath) throws IOException {
        DocumentContext ctx = JsonPath.parse(fromFile(filePath));
        return String.format(QUERY_FMT_OPPORTUNITY, senderEmail(ctx), userEmail(ctx));
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
