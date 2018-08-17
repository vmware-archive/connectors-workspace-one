/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.bitbucket.server;

import com.google.common.collect.ImmutableList;
import com.vmware.connectors.bitbucket.server.utils.BitbucketServerAction;
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
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.vmware.connectors.bitbucket.server.utils.BitbucketServerConstants.*;
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

    @ParameterizedTest
    @ValueSource(strings = {
            "/cards/requests",
            "/api/v1/UFO/app-platform-server/249/approve",
            "/api/v1/UFO/app-platform-server/249/comments"})
    void testProtectedResources(String uri) throws Exception {
        testProtectedResource(POST, uri);
    }

    @Test
    void testDiscovery() throws Exception {
        testConnectorDiscovery();
    }

    @Test
    void testRegex() throws Exception {
        final List<String> expectedList = ImmutableList.of(
                // Project Name/ Repository Plug/ Pull request id.
                "UFO/app-platform-server - Pull request #244: ",
                "UFO/app-platform-server - Pull request #245: ",
                "UFO/app-platform-server - Pull request #241: ",
                "UFO/app-platform-server - Pull request #239: ",
                "UFO/card-connectors - Pull request #9: ");

        testRegex("pr_email_subject", fromFile("/regex/pr-email-subject.txt"), expectedList);
    }

    @Test
    void getImage()  {
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
    void testCardRequestWithEmptyIssue() throws Exception {
        testCardRequests("emptyIssue.json", "emptyIssue.json", null);
    }

    @DisplayName("Card request success cases")
    @ParameterizedTest(name = "{index} ==> Language=''{0}''")
    @CsvSource({
            StringUtils.EMPTY + ", success.json",
            "xx, success_xx.json"})
    void testRequestCardsSuccess(String lang, String resFile) throws Exception {
        buildRequestForCards();

        testCardRequests("request.json", resFile, lang);
    }

    private void buildRequestForCards() {
        final String pr236Url = "/rest/api/1.0/projects/UFO/repos/app-platform-server/pull-requests/" + PULL_REQUEST_ID_1;
        final String pr246Url = "/rest/api/1.0/projects/UFO/repos/app-platform-server/pull-requests/" + PULL_REQUEST_ID_2;
        final String notFoundUrl = "/rest/api/1.0/projects/UFO/repos/NOT-FOUND/pull-requests/999";

        expect(pr236Url).andRespond(withSuccess(pr236, APPLICATION_JSON));
        expect(pr246Url).andRespond(withSuccess(pr246, APPLICATION_JSON));
        expect(notFoundUrl).andRespond(withStatus(HttpStatus.NOT_FOUND));
    }

    @ParameterizedTest(name = "{index} ==> ''{0}''")
    @DisplayName("Missing parameter cases")
    @CsvSource({
            "emptyRequest.json, emptyRequest.json",
            "emptyToken.json, emptyToken.json"})
    void testRequestCardsWithMissingParameter(String requestFile, String responseFile) throws Exception {
        requestCard(BITBUCKET_SERVER_AUTH_TOKEN, requestFile)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("bitbucket/responses/" + responseFile));
    }

    @Test
    void testBitbucketServerPRApproveAction() {
        expect("/rest/api/1.0/projects/UFO/repos/app-platform-server/pull-requests/" + PULL_REQUEST_ID_1).andRespond(withSuccess(pr236, APPLICATION_JSON));

        mockBackend.expect(requestTo("/rest/api/1.0/projects/UFO/repos/app-platform-server/pull-requests/236/" + BitbucketServerAction.APPROVE.getAction() + "?version=10"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN))
                .andExpect(MockRestRequestMatchers.header(ATLASSIAN_TOKEN, "no-check"))
                .andRespond(withSuccess(approve, APPLICATION_JSON));

        webClient.post()
                .uri("/api/v1/UFO/app-platform-server/236/approve")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(AUTH_HEADER, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN)
                .header(BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, "https://hero/connectors/stash/")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void comment() {
        mockBackend.expect(requestTo("/rest/api/1.0/projects/UFO/repos/app-platform-server/pull-requests/236/comments"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN))
                .andExpect(MockRestRequestMatchers.header(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(comments, APPLICATION_JSON));

        webClient.post()
                .uri("/api/v1/UFO/app-platform-server/236/comments")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(AUTH_HEADER, "Basic " + BITBUCKET_SERVER_AUTH_TOKEN)
                .header(BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(COMMENT_PARAM_KEY, "Pull request comment"))
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
    void testAuthFail() {
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

    private void testCardRequests(final String requestFile,
                                  final String responseFile,
                                  final String acceptLanguage) throws Exception {
        final WebTestClient.RequestHeadersSpec spec = requestCard(BITBUCKET_SERVER_AUTH_TOKEN, requestFile);
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
        assertThat(body,  sameJSONAs(fromFile("bitbucket/responses/" + responseFile)).allowingAnyArrayOrdering());
    }

    private WebTestClient.RequestHeadersSpec<?> requestCard(final String authToken, final String requestFile) throws IOException {
            return webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header(AUTH_HEADER, "Basic " + authToken)
                .header(BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, "https://hero/connectors/stash/")
                .headers(ControllerTestsBase::headers)
                .syncBody(fromFile("/bitbucket/requests/" + requestFile));
    }

    private ResponseActions expect(final String url) {
        return mockBackend.expect(requestTo(url))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Basic bitbucket-token"));
    }
}
