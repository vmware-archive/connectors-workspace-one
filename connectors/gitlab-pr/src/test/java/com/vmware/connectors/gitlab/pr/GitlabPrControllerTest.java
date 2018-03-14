/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.gitlab.pr;

import com.google.common.collect.ImmutableList;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonReplacementsBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GitlabPrControllerTest extends ControllerTestsBase {

    private static final String GITLAB_AUTH_TOKEN = "test-auth-token";

    @Autowired
    private AsyncRestTemplate rest;

    private MockRestServiceServer mockGitlab;

    @BeforeEach
    public void setup() throws Exception {
        super.setup();

        mockGitlab = MockRestServiceServer.bindTo(rest)
                .ignoreExpectOrder(true)
                .build();
    }

    @AfterEach
    void teardown() throws Exception {
        mockGitlab.verify();
    }

    @Test
    void testProtectedResource() throws Exception {
        testProtectedResource(POST, "/cards/requests");
        testProtectedResource(POST, "/api/v1/test-owner/test-repo/1234/close");
        testProtectedResource(POST, "/api/v1/test-owner/test-repo/1234/merge");
        testProtectedResource(POST, "/api/v1/test-owner/test-repo/1234/approve");
        testProtectedResource(POST, "/api/v1/test-owner/test-repo/1234/comment");
    }

    @Test
    void testDiscovery() throws Exception {
        testConnectorDiscovery();
    }

    @Test
    void testRegex() throws Exception {
        List<String> expected = ImmutableList.of(
                "https://gitlab.com/vmware/test-repo/merge_requests/15"
        );
        testRegex("merge_request_urls", fromFile("fake/regex/pr-email.txt"), expected);
    }

    private MockHttpServletRequestBuilder setupPostRequest(
            String path,
            MediaType contentType,
            String authToken,
            String content
    ) throws Exception {
        return setupPostRequest(path, contentType, authToken, content, null);
    }

    private MockHttpServletRequestBuilder setupPostRequest(
            String path,
            MediaType contentType,
            String authToken,
            String content,
            String language
    ) throws Exception {

        MockHttpServletRequestBuilder builder = post(path)
                .with(token(accessToken()))
                .contentType(contentType)
                .accept(APPLICATION_JSON)
                .header("x-gitlab-pr-base-url", "https://gitlab.com")
                .header("x-routing-prefix", "https://hero/connectors/gitlab-pr/")
                .content(content);

        if (authToken != null) {
            builder = builder.header("x-gitlab-pr-authorization", "Bearer " + authToken);
        }

        if (language != null) {
            builder = builder.header(ACCEPT_LANGUAGE, language);
        }

        return builder;
    }

    private ResultActions requestCards(String authToken, String content) throws Exception {
        return requestCards(authToken, content, null);
    }

    private ResultActions requestCards(String authToken, String content, String language) throws Exception {
        return perform(
                setupPostRequest(
                        "/cards/requests",
                        APPLICATION_JSON,
                        authToken,
                        content,
                        language
                )
        );
    }

    private ResultActions close(String authToken, String reason) throws Exception {
        return perform(
                setupPostRequest(
                        "/api/v1/vmware/test-repo/99/close",
                        APPLICATION_FORM_URLENCODED,
                        authToken,
                        reason == null ? "" : String.format("reason=%s", reason)
                )
        );
    }

    private ResultActions merge(String authToken, String sha) throws Exception {
        return perform(
                setupPostRequest(
                        "/api/v1/vmware/test-repo/99/merge",
                        APPLICATION_FORM_URLENCODED,
                        authToken,
                        sha == null ? "" : String.format("sha=%s", sha)
                )
        );
    }

    private ResultActions approve(String authToken, String sha) throws Exception {
        return perform(
                setupPostRequest(
                        "/api/v1/vmware/test-repo/99/approve",
                        APPLICATION_FORM_URLENCODED,
                        authToken,
                        sha == null ? "" : String.format("sha=%s", sha)
                )
        );
    }

    private ResultActions comment(String authToken, String message) throws Exception {
        return perform(
                setupPostRequest(
                        "/api/v1/vmware/test-repo/99/comment",
                        APPLICATION_FORM_URLENCODED,
                        authToken,
                        message == null ? "" : String.format("message=%s", message)
                )
        );
    }

    /////////////////////////////
    // Request Cards
    /////////////////////////////

    @Test
    void testRequestCardsUnauthorized() throws Exception {
        mockGitlab.expect(ExpectedCount.manyTimes(), requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        requestCards(GITLAB_AUTH_TOKEN, fromFile("requests/valid/cards/card.json"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Backend-Status", "401"));
    }

    @Test
    void testRequestCardsAuthHeaderMissing() throws Exception {
        requestCards(null, fromFile("requests/valid/cards/card.json"))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("Card request success cases")
    @ParameterizedTest(name = "{index} ==> Language=''{0}''")
    @CsvSource({
            " , responses/success/cards/card.json",
            "xx, responses/success/cards/card_xx.json"})
    void testRequestCardsSuccess(String acceptLanguage, String responseFile) throws Exception {
        trainGitlabForCards();

        requestCards(GITLAB_AUTH_TOKEN, fromFile("requests/valid/cards/card.json"), acceptLanguage)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(
                        content().string(
                                JsonReplacementsBuilder
                                        .from(fromFile(responseFile))
                                        .buildForCards()
                        )
                );
    }


    private void trainGitlabForCards() throws Exception {
        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/1"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("fake/cards/small-merged-pr.json"), APPLICATION_JSON));

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/2"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("fake/cards/small-open-pr.json"), APPLICATION_JSON));

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/3"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withSuccess(fromFile("fake/cards/big-closed-pr.json"), APPLICATION_JSON));

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/0-not-found"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(GET))
                .andRespond(withStatus(NOT_FOUND));
    }

    @Test
    void testRequestCardsEmptyPrUrlsSuccess() throws Exception {
        requestCards(GITLAB_AUTH_TOKEN, fromFile("requests/valid/cards/empty-pr-urls.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(
                        content().string(
                                JsonReplacementsBuilder
                                        .from(fromFile("responses/success/cards/empty-pr-urls.json"))
                                        .buildForCards()
                        )
                );
    }

    @Test
    void testRequestCardsMissingPrUrlsSuccess() throws Exception {
        requestCards(GITLAB_AUTH_TOKEN, fromFile("requests/valid/cards/missing-pr-urls.json"))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("Card request invalid token cases")
    @ParameterizedTest(name = "{index} ==> ''{0}''")
    @CsvSource({"requests/invalid/cards/empty-tokens.json, responses/error/cards/empty-tokens.json",
            "requests/invalid/cards/missing-tokens.json, responses/error/cards/missing-tokens.json"})
    void testRequestCardsInvalidTokens(String reqFile, String resFile) throws Exception {
        requestCards(GITLAB_AUTH_TOKEN, fromFile(reqFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(fromFile(resFile), false));
    }

    /////////////////////////////
    // Approve Action
    /////////////////////////////

    @Test
    void testApproveActionUnauthorized() throws Exception {
        mockGitlab.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        approve(GITLAB_AUTH_TOKEN, "test-sha")
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Backend-Status", "401"));
    }

    @Test
    void testApproveAuthHeaderMissing() throws Exception {
        approve(null, "test-sha")
                .andExpect(status().isBadRequest());
    }

    @Test
    void testApproveActionMissingSha() throws Exception {
        approve(GITLAB_AUTH_TOKEN, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void testApproveActionSuccess() throws Exception {
        String fakeResponse = fromFile("fake/actions/approve/success.json");

        String expected = fromFile("responses/actions/approve/success.json");

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/99/approve"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.sha", is("test-sha")))
                .andRespond(withSuccess(fakeResponse, APPLICATION_JSON));

        approve(GITLAB_AUTH_TOKEN, "test-sha")
                .andExpect(status().isOk())
                .andExpect(content().json(expected, false));
    }

    @Test
    void testApproveActionFailed() throws Exception {
        String fakeResponse = fromFile("fake/actions/approve/failed.json");

        String expected = fromFile("responses/actions/approve/failed.json");

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/99/approve"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.sha", is("test-sha")))
                .andRespond(
                        /*
                         * Unfortunately, not having the enterprise license causes /approve to
                         * 401 Unauthorized instead of 403 Forbidden.  This makes it impossible
                         * for us to distinguish the difference between "try again after you log in again"
                         * and "you will never be able to do this unless you buy a subscription".
                         */
                        withStatus(UNAUTHORIZED)
                                .contentType(APPLICATION_JSON)
                                .body(fakeResponse)
                );

        approve(GITLAB_AUTH_TOKEN, "test-sha")
                .andExpect(status().isBadRequest())
                .andExpect(header().string("x-backend-status", "401"))
                .andExpect(content().json(expected, false));
    }

    /////////////////////////////
    // Close Action
    /////////////////////////////

    @Test
    void testCloseActionUnauthorized() throws Exception {
        mockGitlab.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        close(GITLAB_AUTH_TOKEN, "test-close-reason")
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Backend-Status", "401"));
    }

    @Test
    void testCloseAuthHeaderMissing() throws Exception {
        close(null, "test-close-reason")
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCloseActionSuccess() throws Exception {
        String fakeCommentResponse = fromFile("fake/actions/close/comment-success.json");

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/99/notes"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body", is("test-close-reason")))
                .andRespond(withSuccess(fakeCommentResponse, APPLICATION_JSON));

        String fakeCloseResponse = fromFile("fake/actions/close/close-success.json");

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/99"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(PUT))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.state_event", is("close")))
                .andRespond(withSuccess(fakeCloseResponse, APPLICATION_JSON));

        String expected = fromFile("responses/actions/close/close-success.json");

        close(GITLAB_AUTH_TOKEN, "test-close-reason")
                .andExpect(status().isOk())
                .andExpect(content().json(expected, false));
    }

    @Test
    void testCloseActionNoReasonSuccess() throws Exception {
        String fakeResponse = fromFile("fake/actions/close/close-success-no-reason.json");

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/99"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(PUT))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.state_event", is("close")))
                .andRespond(withSuccess(fakeResponse, APPLICATION_JSON));

        String expected = fromFile("responses/actions/close/close-success-no-reason.json");

        close(GITLAB_AUTH_TOKEN, null)
                .andExpect(status().isOk())
                .andExpect(content().json(expected, false));
    }

    @Test
    void testCloseActionCommentFailed() throws Exception {
        String fakeResponse = fromFile("fake/actions/close/comment-failed.json");

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/99/notes"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body", is("test-close-reason")))
                .andRespond(
                        withStatus(NOT_FOUND)
                                .contentType(APPLICATION_JSON)
                                .body(fakeResponse)
                );

        String expected = fromFile("responses/actions/close/comment-failed.json");

        close(GITLAB_AUTH_TOKEN, "test-close-reason")
                .andExpect(status().is5xxServerError())
                .andExpect(header().string("x-backend-status", "404"))
                .andExpect(content().json(expected, false));
    }

    @Test
    void testCloseActionCloseFailed() throws Exception {
        String fakeCommentResponse = fromFile("fake/actions/close/comment-success.json");

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/99/notes"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body", is("test-close-reason")))
                .andRespond(withSuccess(fakeCommentResponse, APPLICATION_JSON));

        String fakeCloseResponse = fromFile("fake/actions/close/close-failed.json");

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/99"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(PUT))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.state_event", is("close")))
                .andRespond(
                        withStatus(SERVICE_UNAVAILABLE)
                                .contentType(APPLICATION_JSON)
                                .body(fakeCloseResponse)
                );

        String expected = fromFile("responses/actions/close/close-failed.json");

        close(GITLAB_AUTH_TOKEN, "test-close-reason")
                .andExpect(status().is5xxServerError())
                .andExpect(header().string("x-backend-status", "503"))
                .andExpect(content().json(expected, false));
    }

    /////////////////////////////
    // Comment Action
    /////////////////////////////

    @Test
    void testCommentActionUnauthorized() throws Exception {
        mockGitlab.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        comment(GITLAB_AUTH_TOKEN, "test-comment")
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Backend-Status", "401"));
    }

    @Test
    void testCommentAuthHeaderMissing() throws Exception {
        comment(null, "test-comment")
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCommentActionSuccess() throws Exception {
        String fakeResponse = fromFile("fake/actions/comment/success.json");

        String expected = fromFile("responses/actions/comment/success.json");

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/99/notes"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body", is("test-comment")))
                .andRespond(withSuccess(fakeResponse, APPLICATION_JSON));

        comment(GITLAB_AUTH_TOKEN, "test-comment")
                .andExpect(status().isOk())
                .andExpect(content().json(expected, false));
    }

    @Test
    void testCommentActionMissingComment() throws Exception {
        comment(GITLAB_AUTH_TOKEN, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCommentActionFailed() throws Exception {
        String fakeResponse = fromFile("fake/actions/comment/failed.json");

        String expected = fromFile("responses/actions/comment/failed.json");

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/99/notes"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body", is("test-comment")))
                .andRespond(
                        withStatus(NOT_FOUND)
                                .contentType(APPLICATION_JSON)
                                .body(fakeResponse)
                );

        comment(GITLAB_AUTH_TOKEN, "test-comment")
                .andExpect(status().is5xxServerError())
                .andExpect(header().string("x-backend-status", "404"))
                .andExpect(content().json(expected, false));
    }

    /////////////////////////////
    // Merge Action
    /////////////////////////////

    @Test
    void testMergeActionUnauthorized() throws Exception {
        mockGitlab.expect(requestTo(any(String.class)))
                .andRespond(withUnauthorizedRequest());

        merge(GITLAB_AUTH_TOKEN, "test-sha")
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Backend-Status", "401"));
    }

    @Test
    void testMergeAuthHeaderMissing() throws Exception {
        merge(null, "test-sha")
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMergeActionMissingSha() throws Exception {
        merge(GITLAB_AUTH_TOKEN, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMergeActionSuccess() throws Exception {
        String fakeResponse = fromFile("fake/actions/merge/success.json");

        String expected = fromFile("responses/actions/merge/success.json");

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/99/merge"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(PUT))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.sha", is("test-sha")))
                .andRespond(withSuccess(fakeResponse, APPLICATION_JSON));

        merge(GITLAB_AUTH_TOKEN, "test-sha")
                .andExpect(status().isOk())
                .andExpect(content().json(expected, false));
    }

    @Test
    void testMergeActionFailed() throws Exception {
        String fakeResponse = fromFile("fake/actions/merge/failed.json");

        String expected = fromFile("responses/actions/merge/failed.json");

        mockGitlab.expect(requestTo("https://gitlab.com/api/v4/projects/vmware%2Ftest-repo/merge_requests/99/merge"))
                .andExpect(header(AUTHORIZATION, "Bearer " + GITLAB_AUTH_TOKEN))
                .andExpect(method(PUT))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.jsonPath("$.sha", is("test-sha")))
                .andRespond(
                        withStatus(METHOD_NOT_ALLOWED)
                                .contentType(APPLICATION_JSON)
                                .body(fakeResponse)
                );

        merge(GITLAB_AUTH_TOKEN, "test-sha")
                .andExpect(status().is5xxServerError())
                .andExpect(header().string("x-backend-status", "405"))
                .andExpect(content().json(expected, false));
    }

}
