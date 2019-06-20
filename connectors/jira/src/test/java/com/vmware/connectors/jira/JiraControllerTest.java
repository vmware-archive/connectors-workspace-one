/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.jira;

import com.google.common.collect.ImmutableList;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

/**
 * Created by Rob Worsnop on 12/9/16.
 */
class JiraControllerTest extends ControllerTestsBase {

    @Value("classpath:jira/responses/APF-27.json")
    private Resource apf27;

    @Value("classpath:jira/responses/APF-28.json")
    private Resource apf28;

    @Value("classpath:jira/responses/myself.json")
    private Resource myself;

    @ParameterizedTest
    @ValueSource(strings = {
            "/cards/requests",
            "/api/v1/issues/1234/comment",
            "/api/v1/issues/1234/watchers"})
    void testProtectedResource(String uri) throws Exception {
        testProtectedResource(POST, uri);
    }

    @Test
    void testDiscovery() throws Exception {
        testConnectorDiscovery();
    }

    @Test
    void testRegex() throws Exception {
        List<String> expected = ImmutableList.of(
                "ABC-1",
                "ABCDEFGHIJ-123",
//                "ABCDEFGHIJK-456", // Should not match because too many letters
//                "ABC-3", // Should not match due to trailing "A"
                "ABC-4",
                "ABC-5",
//                "ABC-6", // Should not match due to leading "x"
                "ABC-7",
//                "ABC-8", // Should not match due to leading "1"
                "ABC-9",
                "ABC-10",
                "ABC-11",
//                "ABC-12", // Should not match due to trailing "X"
                "ABC-13",
                "ABC-14",
//                "ABC-15", // Should not match due to trailing "x"
                "ABC-16",
                "ABC-17",
                "ABC-18",
//                "ABC-19", // Should not match due to trailing "x"
//                "ABC-20", // Should not match due to leading "x"
//                "D-2", // should not match
//                "MM-2", // should not match
//                "ZGW-2", // should not match
//                "XV-2", // should not match
//                "F-2", // should not match
//                "SA-2", // should not match
                "ABC-21"
        );

        testRegex("issue_id", fromFile("/regex/email.txt"), expected);
    }

    @Test
    void testRequestWithEmptyIssue() throws Exception {
        testRequestCards("emptyIssue.json", "noResults.json", null);
    }

    @ParameterizedTest(name = "{index} ==> ''{0}''")
    @DisplayName("Missing parameter cases")
    @CsvSource({
            "emptyRequest.json, noResults.json",
            "emptyToken.json, noResults.json"})
    void testRequestCardsWithMissingParameter(String requestFile, String responseFile) throws Exception {
        requestCards("abc", requestFile)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
            .expectBody().json(fromFile("connector/responses/" + responseFile));
    }

    @DisplayName("Card request success cases")
    @ParameterizedTest(name = "{index} ==> Language=''{0}''")
    @CsvSource({
            ", success.json",
            "xx, success_xx.json"})
    void testRequestCardsSuccess(String lang, String resFile) throws Exception {
        expect("APF-27").andRespond(withSuccess(apf27, APPLICATION_JSON));
        expect("APF-28").andRespond(withSuccess(apf28, APPLICATION_JSON));
        testRequestCards("request.json", resFile, lang);
    }

    @Test
    void testAuthSuccess() {
        mockBackend.expect(requestTo("/rest/api/2/myself"))
                .andExpect(method(HEAD))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"))
                .andRespond(withSuccess("foo", TEXT_HTML));

        webClient.head()
                .uri("/test-auth")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_AUTH_HEADER, "Bearer abc")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void testAuthFail() {
        mockBackend.expect(requestTo("/rest/api/2/myself"))
                .andExpect(method(HEAD))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"))
                .andRespond(withUnauthorizedRequest());

        webClient.head()
                .uri("/test-auth")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_AUTH_HEADER, "Bearer abc")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("x-backend-status", "401");
    }

    /*
    Give more priority to x-auth header if more than one request-headers are missing.
     */
    @Test
    void testMissingRequestHeaders() throws Exception {
        webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-routing-prefix", "https://hero/connectors/jira/")
                .syncBody(fromFile("/jira/requests/request.json"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Missing request header '" + X_AUTH_HEADER + "' for method parameter of type String");
    }

    @Test
    void testRequestCardsNotAuthorized() throws Exception {
        mockBackend.expect(times(2), requestTo(any(String.class)))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer bogus"))
                .andExpect(method(GET))
                .andRespond(withUnauthorizedRequest());
        requestCards("bogus", "request.json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401")
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("/connector/responses/invalid_connector_token.json"));
    }

    @Test
    void testRequestCardsOneNotFound() throws Exception {
        expect("APF-27").andRespond(withSuccess(apf27, APPLICATION_JSON));
        expect("BOGUS-999").andRespond(withStatus(NOT_FOUND));

        String body = requestCards("abc", "oneCardNotFound.json")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .map(json -> json.replaceAll("http://localhost:\\d+/", "https://jira.acme.com"))
                .block();
        body = body.replaceAll("[a-z0-9]{40,}", "test-hash");
        assertThat(body,  sameJSONAs(fromFile("connector/responses/APF-27.json")));
    }

    @Test
    void testRequestCardsOneServerError() throws Exception {
        expect("POISON-PILL").andRespond(withServerError());
        expect("APF-27").andRespond(withSuccess(apf27, APPLICATION_JSON));
        requestCards("abc", "oneServerError.json")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals("X-Backend-Status", "500");
     }

    @Test
    void testAddComment() {
        mockBackend.expect(requestTo("/rest/api/2/issue/1234/comment"))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().string("{\"body\":\"Hello\"}"))
                .andRespond(withStatus(CREATED));

        webClient.post()
                .uri("/api/v1/issues/1234/comment")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(X_AUTH_HEADER, "Bearer abc")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .syncBody("body=Hello")
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void testAddCommentWith401() {
        webClient.post()
                .uri("/api/v1/issues/1234/comment")
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(X_AUTH_HEADER, "Bearer abc")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .syncBody("body=Hello")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void testAddCommentWithMissingConnectorAuthorization() throws Exception {
        webClient.post()
                .uri("/api/v1/issues/1234/comment")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(X_BASE_URL_HEADER, "https://jira.acme.com")
                .syncBody("body=Hello")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testAddCommentWithBackend401() throws Exception {
        mockBackend.expect(requestTo("/rest/api/2/issue/1234/comment"))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer bogus"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().string("{\"body\":\"Hello\"}"))
                .andRespond(withStatus(UNAUTHORIZED));
        webClient.post()
                .uri("/api/v1/issues/1234/comment")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(X_AUTH_HEADER, "Bearer bogus")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .syncBody("body=Hello")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(fromFile("/connector/responses/invalid_connector_token.json"));
    }

    @Test
    void testAddWatcher() {
        mockBackend.expect(requestTo("/rest/api/2/myself"))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"))
                .andExpect(method(GET))
                .andRespond(withSuccess(myself, APPLICATION_JSON));
        mockBackend.expect(requestTo("/rest/api/2/issue/1234/watchers"))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().string("\"harshas\""))
                .andRespond(withStatus(NO_CONTENT));


        webClient.post()
                .uri("/api/v1/issues/1234/watchers")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_AUTH_HEADER, "Bearer abc")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .exchange()
                .expectStatus().isNoContent();
     }

    @Test
    void testAddWatcherWith401() {
        webClient.post()
                .uri("/api/v1/issues/1234/watchers")
                .header(AUTHORIZATION, "Bearer invalid")
                .header(X_AUTH_HEADER, "Bearer abc")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void testAddWatcherWithMissingConnectorAuthorization() throws Exception {
        webClient.post()
                .uri("/api/v1/issues/1234/watchers")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                 .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testAddWatcherWithBackend401() throws Exception {
        mockBackend.expect(requestTo("/rest/api/2/myself"))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer bogus"))
                .andExpect(method(GET))
                .andRespond(withStatus(UNAUTHORIZED));


        webClient.post()
                .uri("/api/v1/issues/1234/watchers")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_AUTH_HEADER, "Bearer bogus")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(fromFile("/connector/responses/invalid_connector_token.json"));
    }

    @Test
    void testGetImage() throws Exception {
        webClient.get()
                .uri("/images/connector.png")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentLength(11851)
                .expectHeader().contentType(IMAGE_PNG_VALUE)
                .expectBody(byte[].class).isEqualTo(bytesFromFile("/static/images/connector.png"));
    }

    private ResponseActions expect(String issue) {
        return mockBackend.expect(requestTo("/rest/api/2/issue/" + issue))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"));
    }

    private void testRequestCards(String requestFile, String responseFile, String acceptLanguage) throws Exception {
        WebTestClient.RequestHeadersSpec<?> spec = requestCards("abc", requestFile);
        if (StringUtils.isNotBlank(acceptLanguage)) {
            spec = spec.header(ACCEPT_LANGUAGE, acceptLanguage);
        }
        String body = spec.exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .map(json -> json.replaceAll("http://localhost:\\d+/", "https://jira.acme.com"))
                .block();
        body = body.replaceAll("[a-z0-9]{40,}", "test-hash");
        assertThat(body,  sameJSONAs(fromFile("connector/responses/" + responseFile)).allowingAnyArrayOrdering());

    }

    private WebTestClient.RequestHeadersSpec<?> requestCards(String authToken, String requestfile) throws IOException {
        return webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .headers(ControllerTestsBase::headers)
                .header(X_AUTH_HEADER, "Bearer " + authToken)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/jira/")
                .syncBody(fromFile("/jira/requests/" + requestfile));
    }

}
