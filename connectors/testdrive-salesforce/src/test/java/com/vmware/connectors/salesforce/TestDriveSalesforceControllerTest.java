/*
 * Copyright © 2019 VMware, Inc. All rights reserved. This product is protected by
 * copyright and intellectual property laws in the United States and other countries as
 * well as by international treaties. AirWatch products may be covered by one or more
 * patents listed at http://www.vmware.com/go/patents.
 */

package com.vmware.connectors.salesforce;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vmware.connectors.salesforce.TestDriveSalesforceController.commaSeparatedListOfEscapedIds;
import static com.vmware.connectors.salesforce.TestDriveSalesforceController.soqlEscape;
import static com.vmware.connectors.utils.IgnoredFieldsReplacer.HASH_PATTERN;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestDriveSalesforceControllerTest extends ControllerTestsBase {

    private static final String X_ROUTING_HEADER = "x-routing-prefix";
    private static final String X_AUTH_HEADER = "X-Connector-Authorization";
    private static final String X_BASE_URL_HEADER = "X-Connector-Base-Url";
    private static final String TRAVIS_ACCOUNT_ID = "0014100000Vc2iPAAR";
    private static final String SOQL_QUERY_PATH = "/services/data/v44.0/query";
    private static final String UPDATE_OPPORTUNITY_PATH = "/services/data/v44.0/sobjects/Opportunity";
    private static final String RESPONSE_OPPORTUNITY_IDS_PATH = "/salesforce/response/successOpportunityIds.json";
    private static final String RESPONSE_OPPORTUNITY_DETAILS_PATH = "/salesforce/response/successOpportunityDetails.json";
    private static final String RESPONSE_OPPORTUNITY_PARSE_TEST_PATH = "/salesforce/response/opportunityParseTest.json";
    private static final String CONNECTOR_REQUEST_DATA_PATH = "/connector/requests/request.json";

    @BeforeEach
    private void setUp() {
        mockBackend.reset();
    }

    @Test
    void testSFOpportunityDeserialization() throws IOException {
        List<SFOpportunity> opps = SFOpportunity.fromJson(fromFile(RESPONSE_OPPORTUNITY_PARSE_TEST_PATH));
        assertThat(opps, notNullValue());
        assertThat(opps.size(), equalTo(3));

        SFOpportunity firstOpp = opps.get(0);
        assertThat(firstOpp, notNullValue());
        assertThat(firstOpp.getId(), equalTo("00641000004IK9WAAW"));

        List<String> firstOppFeeds = firstOpp.getFeedEntries();
        assertThat(firstOppFeeds, notNullValue());
        assertThat(firstOppFeeds.size(), equalTo(1));

        SFOpportunity secondOpp = opps.get(1);
        assertThat(secondOpp, notNullValue());

        List<String> secondOppFeeds = secondOpp.getFeedEntries();
        assertThat(secondOppFeeds, notNullValue());
        assertThat(secondOppFeeds.size(), equalTo(0));
    }

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
    void testMissingRequestHeaders() throws IOException {
        webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header(X_ROUTING_HEADER, "https://hero/connectors/salesforce/")
                .syncBody(fromFile(CONNECTOR_REQUEST_DATA_PATH))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Missing request header '" + X_AUTH_HEADER + "' for method parameter of type String");
    }

    @Test
    void testRequestCardNotAuthorized() throws IOException {
        mockBackend.expect(manyTimes(), requestTo(any(String.class)))
                .andExpect(header(AUTHORIZATION, "Bearer bogus"))
                .andExpect(method(GET))
                .andRespond(withUnauthorizedRequest());

        requestCards("bogus", CONNECTOR_REQUEST_DATA_PATH)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401")
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("/connector/responses/invalid_connector_token.json"));
    }

    @Test
    void testRequestCardInternalServerError() throws IOException {
        mockBackend.expect(manyTimes(), requestTo(any(String.class)))
                .andExpect(method(GET))
                .andRespond(withServerError());
        requestCards("abc", CONNECTOR_REQUEST_DATA_PATH)
                .exchange()
                .expectStatus().isEqualTo(INTERNAL_SERVER_ERROR)
                .expectHeader().valueEquals("X-Backend-Status", "500");
    }

    @DisplayName("Card request invalid token cases")
    @ParameterizedTest(name = "{index} ==> ''{0}''")
    @CsvSource({
            "/connector/requests/emptyRequest.json, /connector/responses/missingEmails.json",
            "/connector/requests/emptyToken.json, /connector/responses/missingEmails.json"
    })
    void testRequestCardsInvalidTokens(String reqFile, String resFile) throws IOException {
        requestCards("abc", reqFile)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON_UTF8)
                .expectBody().json(fromFile(resFile));
    }

    @DisplayName("Card request missing token cases")
    @ParameterizedTest(name = "{index} ==> ''{0}''")
    @ValueSource(strings = {
            "/connector/requests/emptySenderEmail.json",
            "/connector/requests/emptyUserEmail.json"
    })
    void testRequestCardsMissingTokens(String reqFile) throws IOException {
        requestCards("abc", reqFile)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON_UTF8)
                .expectBody().json(fromFile("/connector/responses/missingEmails.json"));
    }

    // There are multiple opportunities found related to the email sender.
    @Test
    void testRequestCardSuccess() throws IOException {
        String requestFile = "/connector/requests/requestOpportunities.json";
        String cardResponseFile = "/connector/responses/responseOpportunityCards.json";

        expectSalesforceRequest(getCardRequestSoql(requestFile))
                .andRespond(withSuccess(fromFile(RESPONSE_OPPORTUNITY_IDS_PATH), APPLICATION_JSON));
        expectSalesforceRequest(getOpportunityDetailsSoql())
                .andRespond(withSuccess(fromFile(RESPONSE_OPPORTUNITY_DETAILS_PATH), APPLICATION_JSON));

        testRequestCards(requestFile, cardResponseFile);
    }

    @DisplayName("Update Salesforce opportunity fields")
    @ParameterizedTest(name = "{index} => uri={0}, body={1}")
    @MethodSource("argumentsForUpdateTest")
    void updateOpportunityFields(String uri, String body) {

        mockBackend.expect(requestTo(UPDATE_OPPORTUNITY_PATH + "/00641000004IK9WAAW"))
                .andExpect(method(PATCH))
                .andExpect(header(AUTHORIZATION, "Bearer abc"))
                .andExpect(header(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess());

        webClient.post()
                .uri(uri)
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(X_AUTH_HEADER, "Bearer abc")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .syncBody(body)
                .exchange()
                .expectStatus().isOk();
    }

    private static Stream<Arguments> argumentsForUpdateTest() {
        return Stream.of(
                Arguments.of("/opportunity/00641000004IK9WAAW/closedate", "closedate=2017-06-03"),
                Arguments.of("/opportunity/00641000004IK9WAAW/dealsize", "amount=32000"),
                Arguments.of("/opportunity/00641000004IK9WAAW/nextstep",
                        "nextstep=3%2F31%20-%20Customer%20was%20shown%20the%20roadmap%20for%20ABC%20product&nextstep_previous_value=Some%20earlier%20step"),
                Arguments.of("/opportunity/00641000004IK9WAAW/nextstep", "nextstep=3%2F31%20-%20Customer%20was%20shown%20the%20roadmap%20for%20ABC%20product")
        );
    }

    @DisplayName("Attempt to update Salesforce opportunity fields with missing data")
    @ParameterizedTest(name = "{index} => uri={0}, body=<<{1}>>")
    @MethodSource("argumentsForUpdateTestWithMissingData")
    void updateOpportunityFieldsWithMissingData(String uri, String body) {

        webClient.post()
                .uri(uri)
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(X_AUTH_HEADER, "Bearer abc")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .syncBody(body)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    private static Stream<Arguments> argumentsForUpdateTestWithMissingData() {
        return Stream.of(
                Arguments.of("/opportunity/00641000004IK9WAAW/closedate", "closedate="),
                Arguments.of("/opportunity/00641000004IK9WAAW/closedate", ""),
                Arguments.of("/opportunity/00641000004IK9WAAW/dealsize", "amount="),
                Arguments.of("/opportunity/00641000004IK9WAAW/dealsize", ""),
                Arguments.of("/opportunity/00641000004IK9WAAW/nextstep", "nextstep="),
                Arguments.of("/opportunity/00641000004IK9WAAW/nextstep", "")
        );
    }

    private void testRequestCards(String requestFile, String responseFile) throws IOException {
        WebTestClient.RequestHeadersSpec<?> spec = requestCards("abc", requestFile);

        String body = spec.exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .map(json -> json.replaceAll("http://localhost:\\d+/", "https://salesforce.acme.com/"))
                .block();
        body = body.replaceAll(HASH_PATTERN, "test-hash");

        assertThat(fromFile(responseFile), sameJSONAs(body).allowingAnyArrayOrdering());
    }

    private WebTestClient.RequestHeadersSpec<?> requestCards(String authToken, String filePath) throws IOException {
        return webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header(X_AUTH_HEADER, "Bearer " + authToken)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(X_ROUTING_HEADER, "https://hero/connectors/salesforce/")
                .headers(ControllerTestsBase::headers)
                .syncBody(fromFile(filePath));
    }

    private ResponseActions expectSalesforceRequest(String soqlQuery) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(SOQL_QUERY_PATH).queryParam("q", soqlQuery);
        URI tmp = builder.build().toUri();
        return mockBackend.expect(requestTo(tmp))
                .andExpect(method(GET))
                .andExpect(header(AUTHORIZATION, "Bearer abc"));
    }

    private String getCardRequestSoql(String requestBodyFilePath) throws IOException {
        DocumentContext ctx = JsonPath.parse(fromFile(requestBodyFilePath));
        String senderEmail = ctx.read("$.tokens.sender_email[0]");
        String userEmail = ctx.read("$.tokens.user_email[0]");
        return String.format(TestDriveSalesforceController.QUERY_FMT_RELATED_OPPORTUNITY_IDS,
                soqlEscape(senderEmail), soqlEscape(userEmail), soqlEscape(userEmail));
    }

    private String getOpportunityDetailsSoql() throws IOException {
        List<String> oppIDs = JsonPath.read(fromFile(RESPONSE_OPPORTUNITY_IDS_PATH), "$.records[*].Opportunity.Id");
        return String.format(TestDriveSalesforceController.QUERY_FMT_OPPORTUNITY_INFO, commaSeparatedListOfEscapedIds(oppIDs));
    }

}
