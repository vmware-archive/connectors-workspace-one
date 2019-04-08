/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.bitbucket.server;

import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BitbucketServerControllerTest extends ControllerTestsBase {

    private static final String AUTH_HEADER = "X-Connector-Authorization";
    private static final String BASE_URL_HEADER = "X-Connector-Base-Url";
    private static final String ROUTING_PREFIX = "x-routing-prefix";
    private static final String ATLASSIAN_TOKEN = "X-Atlassian-Token";
    private static final String COMMENT_PARAM_KEY = "comment";

    private static final String BITBUCKET_SERVER_AUTH_TOKEN = "bitbucket-token";

    private static final String PULL_REQUEST_ID_1 = "236";

    private static final String PULL_REQUEST_ID_2 = "246";

    @Value("classpath:bitbucket/responses/pr_236.json")
    private Resource pr236;

    @Value("classpath:bitbucket/responses/pr_246.json")
    private Resource pr246;

    @Value("classpath:bitbucket/responses/approved.json")
    private Resource approve;

    @Value("classpath:bitbucket/responses/comments.json")
    private Resource comments;

    @Value("classpath:bitbucket/responses/personal-repo.json")
    private Resource testRepoPr1;

    @Value("classpath:bitbucket/responses/personal-repo-approved.json")
    private Resource testRepoPr1Approved;

    @Value("classpath:bitbucket/responses/personal-repo-comments.json")
    private Resource testRepoPr1Comments;

    @ParameterizedTest
    @ValueSource(strings = {
            "/cards/requests",
            "/api/v1/app-platform-server/249/approve",
            "/api/v1/app-platform-server/249/comments"
    })
    void testProtectedResources(String uri) throws Exception {
        testProtectedResource(POST, uri);
    }

    @Test
    void discovery() throws Exception {
        testConnectorDiscovery();
    }

    @Test
    void regex() throws Exception {
        List<String> expectedProjectsRepos = List.of(
                // Project Name / Repository Slug / Pull Request ID
                "UFO/app-platform-server - Pull request #244: ",
                "UFO/app-platform-server - Pull request #245: ",
                "UFO/app-platform-server - Pull request #241: ",
                "UFO/app-platform-server - Pull request #239: ",
                "JBARD/test-repo - Pull request #1: ", // false positive, but will taken care up by the skip404
                "UFO/card-connectors - Pull request #9: ");

        testRegex("projects_pr_email_subject", fromFile("/regex/pr-email-subject.txt"), expectedProjectsRepos);

        List<String> expectedUsersRepos = List.of(
                // Username / Repository Slug / Pull Request ID
                "JBARD/test-repo - Pull request #1: "
        );

        testRegex("users_pr_email_subject", fromFile("/regex/pr-email-subject.txt"), expectedUsersRepos);
    }

    @Test
    void image()  {
        webClient.get()
                .uri("/images/connector.png")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentLength(11901)
                .expectHeader().contentType(IMAGE_PNG_VALUE)
                .expectBody().consumeWith(body -> assertThat(body.getResponseBody(),
                equalTo(bytesFromFile("/static/images/connector.png"))));
    }

    @Test
    void cardsEmptyIssue() throws Exception {
        testCardRequests("emptyIssue.json", "noResults.json", null);
    }

    @DisplayName("Card request success cases")
    @ParameterizedTest(name = "{index} ==> Language=''{0}''")
    @CsvSource({
            ", success.json",
            "xx, success_xx.json"
    })
    void cardsSuccess(String lang, String resFile) throws Exception {
        buildRequestForCards();

        testCardRequests("request.json", resFile, lang);
    }

    private void buildRequestForCards() {
        String pr236Url = "/rest/api/1.0/projects/UFO/repos/app-platform-server/pull-requests/" + PULL_REQUEST_ID_1;
        String pr246Url = "/rest/api/1.0/projects/UFO/repos/app-platform-server/pull-requests/" + PULL_REQUEST_ID_2;
        String notFoundUrl = "/rest/api/1.0/projects/UFO/repos/NOT-FOUND/pull-requests/999";
        String personalRepoPrUrl = "/rest/api/1.0/users/JBARD/repos/test-repo/pull-requests/1";

        expect(pr236Url).andRespond(withSuccess(pr236, APPLICATION_JSON));
        expect(pr246Url).andRespond(withSuccess(pr246, APPLICATION_JSON));
        expect(notFoundUrl).andRespond(withStatus(HttpStatus.NOT_FOUND));
        expect(personalRepoPrUrl).andRespond(withSuccess(testRepoPr1, APPLICATION_JSON));
    }

    @ParameterizedTest(name = "{index} ==> ''{0}''")
    @DisplayName("Missing parameter cases")
    @CsvSource({
            "emptyRequest.json, noResults.json",
            "emptyToken.json, noResults.json"
    })
    void cardsMissingParameter(String requestFile, String responseFile) throws Exception {
        requestCard(requestFile)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("bitbucket/responses/" + responseFile));
    }

    @Test
    void approve() {
        expect("/rest/api/1.0/projects/UFO/repos/app-platform-server/pull-requests/" + PULL_REQUEST_ID_1)
                .andRespond(withSuccess(pr236, APPLICATION_JSON));

        mockBackend.expect(requestTo("/rest/api/1.0/projects/UFO/repos/app-platform-server/pull-requests/236/approve?version=10"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN))
                .andExpect(MockRestRequestMatchers.header(ATLASSIAN_TOKEN, "no-check"))
                .andRespond(withSuccess(approve, APPLICATION_JSON));

        webClient.post()
                .uri("/api/v1/app-platform-server/236/approve")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(AUTH_HEADER, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN)
                .header(BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, "https://hero/connectors/stash/")
                .body(BodyInserters.fromFormData("project", "UFO"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void approvePersonalRepo() {
        expect("/rest/api/1.0/users/JBARD/repos/test-repo/pull-requests/1")
                .andRespond(withSuccess(testRepoPr1, APPLICATION_JSON));

        mockBackend.expect(requestTo("/rest/api/1.0/users/JBARD/repos/test-repo/pull-requests/1/approve?version=3"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN))
                .andExpect(MockRestRequestMatchers.header(ATLASSIAN_TOKEN, "no-check"))
                .andRespond(withSuccess(testRepoPr1Approved, APPLICATION_JSON));

        webClient.post()
                .uri("/api/v1/test-repo/1/approve")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(AUTH_HEADER, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN)
                .header(BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, "https://hero/connectors/stash/")
                .body(BodyInserters.fromFormData("user", "JBARD"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void approvePersonalRepoUnallowed() {
        expect("/rest/api/1.0/users/JBARD/repos/test-repo/pull-requests/1")
                .andRespond(withSuccess(testRepoPr1, APPLICATION_JSON));

        mockBackend.expect(requestTo("/rest/api/1.0/users/JBARD/repos/test-repo/pull-requests/1/approve?version=3"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN))
                .andExpect(MockRestRequestMatchers.header(ATLASSIAN_TOKEN, "no-check"))
                .andRespond(withBadRequest().contentType(APPLICATION_JSON).body(testRepoPr1Approved));

        webClient.post()
                .uri("/api/v1/test-repo/1/approve")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(AUTH_HEADER, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN)
                .header(BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, "https://hero/connectors/stash/")
                .body(BodyInserters.fromFormData("user", "JBARD"))
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals("X-Backend-Status", "400");
    }

    @Test
    void comment() {
        mockBackend.expect(requestTo("/rest/api/1.0/projects/UFO/repos/app-platform-server/pull-requests/236/comments"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN))
                .andExpect(MockRestRequestMatchers.header(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(comments, APPLICATION_JSON));

        Map<String, List<String>> payload = Map.of(
                "project", List.of("UFO"),
                COMMENT_PARAM_KEY, List.of("Pull request comment")
        );
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>(payload);

        webClient.post()
                .uri("/api/v1/app-platform-server/236/comments")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(AUTH_HEADER, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN)
                .header(BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void commentPersonalRepo() {
        mockBackend.expect(requestTo("/rest/api/1.0/users/JBARD/repos/test-repo/pull-requests/1/comments"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN))
                .andExpect(MockRestRequestMatchers.header(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(testRepoPr1Comments, APPLICATION_JSON));

        Map<String, List<String>> payload = Map.of(
                "user", List.of("JBARD"),
                COMMENT_PARAM_KEY, List.of("Pull request comment")
        );
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>(payload);

        webClient.post()
                .uri("/api/v1/test-repo/1/comments")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(AUTH_HEADER, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN)
                .header(BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testAuthSuccess() {
        mockBackend.expect(requestTo("/rest/api/1.0/dashboard/pull-request-suggestions?limit=1"))
                .andExpect(method(HEAD))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN))
                .andRespond(withSuccess());

        webClient.head()
                .uri("/test-auth")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(AUTH_HEADER, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN)
                .header(BASE_URL_HEADER, mockBackend.url(""))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testAuthFailure() {
        mockBackend.expect(requestTo("/rest/api/1.0/dashboard/pull-request-suggestions?limit=1"))
                .andExpect(method(HEAD))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN))
                .andRespond(withUnauthorizedRequest());

        webClient.head()
                .uri("/test-auth")
                .header(AUTHORIZATION, "Bearer "  + accessToken())
                .header(AUTH_HEADER, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN)
                .header(BASE_URL_HEADER, mockBackend.url(""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("x-backend-status", "401");
    }

    private void testCardRequests(String requestFile, String responseFile, String acceptLanguage) throws Exception {
        WebTestClient.RequestHeadersSpec spec = requestCard(requestFile);
        if (StringUtils.isNotBlank(acceptLanguage)) {
            spec.header(ACCEPT_LANGUAGE, acceptLanguage);
        }
        String body = spec.exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block();
        body = body.replaceAll("[a-z0-9]{40,}", "test-hash");
        System.err.println("\n\n\n" + body + "\n\n\n");
        assertThat(body,  sameJSONAs(fromFile("bitbucket/responses/" + responseFile)).allowingAnyArrayOrdering());
    }

    private WebTestClient.RequestHeadersSpec<?> requestCard(String requestFile) throws IOException {
        return webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header(AUTH_HEADER, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN)
                .header(BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, "https://hero/connectors/stash/")
                .headers(ControllerTestsBase::headers)
                .syncBody(fromFile("/bitbucket/requests/" + requestFile));
    }

    private ResponseActions expect(String url) {
        return mockBackend.expect(requestTo(url))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Basic bitbucket-token"));
    }
}
