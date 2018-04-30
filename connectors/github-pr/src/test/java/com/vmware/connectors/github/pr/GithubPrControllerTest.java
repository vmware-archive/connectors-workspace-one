/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.github.pr;

import com.google.common.collect.ImmutableList;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

class GithubPrControllerTest extends ControllerTestsBase {

    private static final String GITHUB_AUTH_TOKEN = "test-auth-token";



    @ParameterizedTest
    @ValueSource(strings = {
            "/cards/requests",
            "/api/v1/test-owner/test-repo/1234/close",
            "/api/v1/test-owner/test-repo/1234/merge",
            "/api/v1/test-owner/test-repo/1234/approve",
            "/api/v1/test-owner/test-repo/1234/comment",
            "/api/v1/test-owner/test-repo/1234/request-changes"})
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
                /*
                 * There are 3 links because of the patch and diff links.  The
                 * Set<String> in the cards contract will take care of the
                 * duplication.
                 */
                "https://github.com/vmware/connectors-workspace-one/pull/3",
                "https://github.com/vmware/connectors-workspace-one/pull/3",
                "https://github.com/vmware/connectors-workspace-one/pull/3"
        );
        testRegex("pull_request_urls", fromFile("fake/regex/pr-email.txt"), expected);
    }

    private WebTestClient.ResponseSpec doPost(
            String path,
            MediaType contentType,
            String authToken,
            String content
    )  {
        return doPost(path, contentType, authToken, content, null);
    }

    private WebTestClient.ResponseSpec doPost(
            String path,
            MediaType contentType,
            String authToken,
            String content,
            String language
    ) {

        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(path)
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(contentType)
                .accept(APPLICATION_JSON)
                .header("x-github-pr-base-url", mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/github-pr/")
                .headers(ControllerTestsBase::headers)
                .syncBody(content);

        if (authToken != null) {
            spec = spec.header("x-github-pr-authorization", "Bearer " + authToken);
        }

        if (language != null) {
            spec = spec.header(ACCEPT_LANGUAGE, language);
        }

        return spec.exchange();
    }

    private WebTestClient.ResponseSpec requestCards(String authToken, String content) throws Exception {
        return requestCards(authToken, content, null);
    }

    private WebTestClient.ResponseSpec requestCards(String authToken, String content, String language) throws Exception {
        return doPost(
                        "/cards/requests",
                        APPLICATION_JSON,
                        authToken,
                        content,
                        language
                );
    }

    private WebTestClient.ResponseSpec close(String authToken, String reason)  {
        return doPost(
                        "/api/v1/vmware/test-repo/99/close",
                        APPLICATION_FORM_URLENCODED,
                        authToken,
                        reason == null ? "" : String.format("reason=%s", reason)
        );
    }

    private WebTestClient.ResponseSpec merge(String authToken, String sha) {
        return doPost(
                        "/api/v1/vmware/test-repo/99/merge",
                        APPLICATION_FORM_URLENCODED,
                        authToken,
                        sha == null ? "" : String.format("sha=%s", sha)
                );
    }

    private WebTestClient.ResponseSpec approve(String authToken) {
        return doPost(
                        "/api/v1/vmware/test-repo/99/approve",
                        APPLICATION_FORM_URLENCODED,
                        authToken,
                        ""
                );
    }

    private WebTestClient.ResponseSpec comment(String authToken, String message) {
        return doPost(
                        "/api/v1/vmware/test-repo/99/comment",
                        APPLICATION_FORM_URLENCODED,
                        authToken,
                        message == null ? "" : String.format("message=%s", message)
                );
    }

    private WebTestClient.ResponseSpec requestChanges(String authToken, String request) {
        return doPost(
                        "/api/v1/vmware/test-repo/99/request-changes",
                        APPLICATION_FORM_URLENCODED,
                        authToken,
                        request == null ? "" : String.format("request=%s", request)
                );
    }

    /////////////////////////////
    // Request Cards
    /////////////////////////////

    @Test
    void testRequestCardsUnauthorized() throws Exception {
        mockBackend.expect(ExpectedCount.manyTimes(), requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        requestCards(GITHUB_AUTH_TOKEN, fromFile("requests/valid/cards/card.json"))
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401");
    }

    @Test
    void testRequestCardsAuthHeaderMissing() throws Exception {
        requestCards(null, fromFile("requests/valid/cards/card.json"))
                .expectStatus().isBadRequest();
    }

    @DisplayName("Card request success cases")
    @ParameterizedTest(name = "{index} ==> Language=''{0}''")
    @CsvSource({
            StringUtils.EMPTY + ", responses/success/cards/card.json",
            "xx, responses/success/cards/card_xx.json"})
    void testRequestCardsSuccess(String acceptLanguage, String responseFile) throws Exception {
        trainGithubForCards();

        String body = requestCards(GITHUB_AUTH_TOKEN, fromFile("requests/valid/cards/card.json"), acceptLanguage)
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block();
        assertThat(body, sameJSONAs(fromFile(responseFile)).allowingAnyArrayOrdering());
    }

    private void trainGithubForCards() throws Exception {
        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/1"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("fake/cards/small-merged-pr.json"), APPLICATION_JSON));

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/2"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("fake/cards/small-open-pr.json"), APPLICATION_JSON));

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/3"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("fake/cards/big-closed-pr.json"), APPLICATION_JSON));

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/0-not-found"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withStatus(NOT_FOUND));
    }

    @Test
    void testRequestCardsEmptyPrUrlsSuccess() throws Exception {
        requestCards(GITHUB_AUTH_TOKEN, fromFile("requests/valid/cards/empty-pr-urls.json"))
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("responses/success/cards/empty-pr-urls.json"));
    }

    @Test
    void testRequestCardsMissingPrUrls() throws Exception {
        requestCards(GITHUB_AUTH_TOKEN, fromFile("requests/valid/cards/missing-pr-urls.json"))
                 .expectStatus().isBadRequest();
    }

    @DisplayName("Card request invalid token cases")
    @ParameterizedTest(name = "{index} ==> ''{0}''")
    @CsvSource({"requests/invalid/cards/empty-tokens.json, responses/error/cards/empty-tokens.json",
            "requests/invalid/cards/missing-tokens.json, responses/error/cards/missing-tokens.json"})
    void testRequestCardsInvalidTokens(String reqFile, String resFile) throws Exception {
        requestCards(GITHUB_AUTH_TOKEN, fromFile(reqFile))
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile(resFile));
    }

    /////////////////////////////
    // Approve Action
    /////////////////////////////

    @Test
    void testApproveActionUnauthorized()  {
        mockBackend.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        approve(GITHUB_AUTH_TOKEN)
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401");
    }

    @Test
    void testApproveAuthHeaderMissing() {
        approve(null).expectStatus().isBadRequest();
    }

    @Test
    void testApproveActionSuccess() throws Exception {
        String fakeResponse = fromFile("fake/actions/approve/success.json");

        String expected = fromFile("responses/actions/approve/success.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99/reviews"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                // Note: This doesn't verify that we don't get an explicit null, see SPR-16339
                .andExpect(MockRestRequestMatchers.jsonPath("$.body").doesNotExist())
                .andExpect(MockRestRequestMatchers.jsonPath("$.event", is("APPROVE")))
                .andRespond(withSuccess(fakeResponse, APPLICATION_JSON));

        approve(GITHUB_AUTH_TOKEN)
                .expectStatus().isOk()
                .expectBody().json(expected);
    }

    @Test
    void testApproveActionFailed() throws Exception {
        String fakeResponse = fromFile("fake/actions/approve/failed.json");

        String expected = fromFile("responses/actions/approve/failed.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99/reviews"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                // Note: This doesn't verify that we don't get an explicit null, see SPR-16339
                .andExpect(MockRestRequestMatchers.jsonPath("$.body").doesNotExist())
                .andExpect(MockRestRequestMatchers.jsonPath("$.event", is("APPROVE")))
                .andRespond(
                        withStatus(UNPROCESSABLE_ENTITY)
                                .contentType(APPLICATION_JSON)
                                .body(fakeResponse)
                );

        approve(GITHUB_AUTH_TOKEN)
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals("x-backend-status", "422")
                .expectBody().json(expected);
    }

    /////////////////////////////
    // Close Action
    /////////////////////////////

    @Test
    void testCloseActionUnauthorized() {
        mockBackend.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        close(GITHUB_AUTH_TOKEN, "test-close-reason")
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401");
    }

    @Test
    void testCloseAuthHeaderMissing() {
        close(null, "test-close-reason")
                .expectStatus().isBadRequest();
    }

    @Test
    void testCloseActionSuccess() throws Exception {
        String fakeCommentResponse = fromFile("fake/actions/close/comment-success.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99/reviews"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body", is("test-close-reason")))
                .andExpect(MockRestRequestMatchers.jsonPath("$.event", is("COMMENT")))
                .andRespond(withSuccess(fakeCommentResponse, APPLICATION_JSON));

        String fakeCloseResponse = fromFile("fake/actions/close/close-success.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(PATCH))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.state", is("closed")))
                .andRespond(withSuccess(fakeCloseResponse, APPLICATION_JSON));

        String expected = fromFile("responses/actions/close/close-success.json");

        close(GITHUB_AUTH_TOKEN, "test-close-reason")
                .expectStatus().isOk()
                .expectBody().json(expected);
    }

    @Test
    void testCloseActionNoReasonSuccess() throws Exception {
        String fakeResponse = fromFile("fake/actions/close/close-success-no-reason.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(PATCH))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.state", is("closed")))
                .andRespond(withSuccess(fakeResponse, APPLICATION_JSON));

        String expected = fromFile("responses/actions/close/close-success-no-reason.json");

        close(GITHUB_AUTH_TOKEN, null)
                .expectStatus().isOk()
                .expectBody().json(expected);
    }

    @Test
    void testCloseActionCommentFailed() throws Exception {
        String fakeResponse = fromFile("fake/actions/close/comment-failed.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99/reviews"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body", is("test-close-reason")))
                .andExpect(MockRestRequestMatchers.jsonPath("$.event", is("COMMENT")))
                .andRespond(
                        withStatus(NOT_FOUND)
                                .contentType(APPLICATION_JSON)
                                .body(fakeResponse)
                );

        String expected = fromFile("responses/actions/close/comment-failed.json");

        close(GITHUB_AUTH_TOKEN, "test-close-reason")
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals("x-backend-status", "404")
                .expectBody().json(expected);
    }

    @Test
    void testCloseActionCloseFailed() throws Exception {
        String fakeCommentResponse = fromFile("fake/actions/close/comment-success.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99/reviews"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body", is("test-close-reason")))
                .andExpect(MockRestRequestMatchers.jsonPath("$.event", is("COMMENT")))
                .andRespond(withSuccess(fakeCommentResponse, APPLICATION_JSON));

        String fakeCloseResponse = fromFile("fake/actions/close/close-failed.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(PATCH))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.state", is("closed")))
                .andRespond(
                        withStatus(SERVICE_UNAVAILABLE)
                                .contentType(APPLICATION_JSON)
                                .body(fakeCloseResponse)
                );

        String expected = fromFile("responses/actions/close/close-failed.json");

        close(GITHUB_AUTH_TOKEN, "test-close-reason")
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals("x-backend-status", "503")
                .expectBody().json(expected);
    }

    /////////////////////////////
    // Comment Action
    /////////////////////////////

    @Test
    void testCommentActionUnauthorized() {
        mockBackend.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        comment(GITHUB_AUTH_TOKEN, "test-comment")
                 .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401");
    }

    @Test
    void testCommentAuthHeaderMissing() {
        comment(null, "test-comment")
               .expectStatus().isBadRequest();
    }

    @Test
    void testCommentActionSuccess() throws Exception {
        String fakeResponse = fromFile("fake/actions/comment/success.json");

        String expected = fromFile("responses/actions/comment/success.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99/reviews"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body", is("test-comment")))
                .andExpect(MockRestRequestMatchers.jsonPath("$.event", is("COMMENT")))
                .andRespond(withSuccess(fakeResponse, APPLICATION_JSON));

        comment(GITHUB_AUTH_TOKEN, "test-comment")
                .expectStatus().isOk()
                .expectBody().json(expected);
    }

    @Test
    void testCommentActionMissingComment() {
        comment(GITHUB_AUTH_TOKEN, null)
               .expectStatus().isBadRequest();
    }

    @Test
    void testCommentActionFailed() throws Exception {
        String fakeResponse = fromFile("fake/actions/comment/failed.json");

        String expected = fromFile("responses/actions/comment/failed.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99/reviews"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body", is("test-comment")))
                .andExpect(MockRestRequestMatchers.jsonPath("$.event", is("COMMENT")))
                .andRespond(
                        withStatus(NOT_FOUND)
                                .contentType(APPLICATION_JSON)
                                .body(fakeResponse)
                );

        comment(GITHUB_AUTH_TOKEN, "test-comment")
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals("x-backend-status", "404")
                .expectBody().json(expected);
    }

    /////////////////////////////
    // Request Changes Action
    /////////////////////////////

    @Test
    void testRequestChangesActionUnauthorized() throws Exception {
        mockBackend.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        requestChanges(GITHUB_AUTH_TOKEN, "test-request-changes")
                 .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401");
    }

    @Test
    void testRequestChangesAuthHeaderMissing() {
        requestChanges(null, "test-request-changes")
                .expectStatus().isBadRequest();
    }

    @Test
    void testRequestChangesActionSuccess() throws Exception {
        String fakeResponse = fromFile("fake/actions/request-changes/success.json");

        String expected = fromFile("responses/actions/request-changes/success.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99/reviews"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body", is("test-request-changes")))
                .andExpect(MockRestRequestMatchers.jsonPath("$.event", is("REQUEST_CHANGES")))
                .andRespond(withSuccess(fakeResponse, APPLICATION_JSON));

        requestChanges(GITHUB_AUTH_TOKEN, "test-request-changes")
                .expectStatus().isOk()
                .expectBody().json(expected);
    }

    @Test
    void testRequestChangesActionMissingRequestChanges() throws Exception {
        requestChanges(GITHUB_AUTH_TOKEN, null)
                .expectStatus().isBadRequest();
    }

    @Test
    void testRequestChangesActionFailed() throws Exception {
        String fakeResponse = fromFile("fake/actions/request-changes/failed.json");

        String expected = fromFile("responses/actions/request-changes/failed.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99/reviews"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body", is("test-requested-changes")))
                .andExpect(MockRestRequestMatchers.jsonPath("$.event", is("REQUEST_CHANGES")))
                .andRespond(
                        withStatus(NOT_FOUND)
                                .contentType(APPLICATION_JSON)
                                .body(fakeResponse)
                );

        requestChanges(GITHUB_AUTH_TOKEN, "test-requested-changes")
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals("x-backend-status", "404")
                .expectBody().json(expected);
    }

    /////////////////////////////
    // Merge Action
    /////////////////////////////

    @Test
    void testMergeActionUnauthorized() {
        mockBackend.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        merge(GITHUB_AUTH_TOKEN, "test-sha")
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401");
    }

    @Test
    void testMergeAuthHeaderMissing() {
        merge(null, "test-sha")
                .expectStatus().isBadRequest();
    }

    @Test
    void testMergeActionMissingSha() {
        merge(GITHUB_AUTH_TOKEN, null)
                .expectStatus().isBadRequest();
    }

    @Test
    void testMergeActionSuccess() throws Exception {
        String fakeResponse = fromFile("fake/actions/merge/success.json");

        String expected = fromFile("responses/actions/merge/success.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99/merge"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(PUT))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.sha", is("test-sha")))
                .andRespond(withSuccess(fakeResponse, APPLICATION_JSON));

        merge(GITHUB_AUTH_TOKEN, "test-sha")
                .expectStatus().isOk()
                .expectBody().json(expected);
    }

    @Test
    void testMergeActionFailed() throws Exception {
        String fakeResponse = fromFile("fake/actions/merge/failed.json");

        String expected = fromFile("responses/actions/merge/failed.json");

        mockBackend.expect(requestTo("/repos/vmware/test-repo/pulls/99/merge"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITHUB_AUTH_TOKEN))
                .andExpect(method(PUT))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.sha", is("test-sha")))
                .andRespond(
                        withStatus(METHOD_NOT_ALLOWED)
                                .contentType(APPLICATION_JSON)
                                .body(fakeResponse)
                );

        merge(GITHUB_AUTH_TOKEN, "test-sha")
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals("x-backend-status", "405")
                .expectBody().json(expected);
    }

}
