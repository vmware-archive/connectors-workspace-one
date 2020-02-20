/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.mock.MockWebServerWrapper;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

@ContextConfiguration(initializers = HubConcurControllerTestBase.CustomInitializer.class)
class HubConcurControllerTestBase extends ControllerTestsBase {

    protected static final String EXPECTED_AUTH_HEADER = "Bearer test-access-token";

    protected static final String CLIENT_ID = "client_id";
    protected static final String CLIENT_SECRET = "client_secret";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String GRANT_TYPE = "grant_type";

    @Value("classpath:fake/oauth_token.json")
    private Resource oauthToken;

    static MockWebServerWrapper mockConcurServer;

    @BeforeAll
    static void createMock() {
        mockConcurServer = new MockWebServerWrapper(new MockWebServer());
    }

    @BeforeEach
    void resetConcurServer() {
        mockConcurServer.reset();
    }

    @AfterEach
    void verifyConcurServer() {
        mockConcurServer.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/cards/requests",
            "/api/expense/123/approve",
            "/api/expense/123/decline"
    })
    void testProtectedResources(String uri) throws Exception {
        testProtectedResource(POST, uri);
    }

    @Test
    void testDiscovery() throws Exception {
        testConnectorDiscovery();
    }

    WebTestClient.ResponseSpec approveRequest(String authHeader) {
        String uri = "/api/expense/1D3BD2E14D144508B05F/approve";
        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(uri)
                .header(AUTHORIZATION, "Bearer " + accessToken(uri))
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("comment", "Approval Done"));

        if (StringUtils.isNotBlank(authHeader)) {
            spec.header(X_AUTH_HEADER, authHeader);
        }

        return spec.exchange();
    }

    WebTestClient.ResponseSpec rejectRequest(String authHeader) {
        String uri = "/api/expense/1D3BD2E14D144508B05F/decline";
        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(uri)
                .header(AUTHORIZATION, "Bearer " + accessToken(uri))
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("reason", "Decline Done"));

        if (StringUtils.isNotBlank(authHeader)) {
            spec.header(X_AUTH_HEADER, authHeader);
        }

        return spec.exchange();
    }

    WebTestClient.ResponseSpec cardsRequest(String lang, String authHeader) throws Exception {
        String uri = "/cards/requests";
        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(uri)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/concur/")
                .headers(headers -> headers(headers, uri))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .syncBody(fromFile("/connector/requests/request.json"));

        if (StringUtils.isNotBlank(authHeader)) {
            spec.header(X_AUTH_HEADER, authHeader);
        }

        if (StringUtils.isNotBlank(lang)) {
            spec.header(ACCEPT_LANGUAGE, lang);
        }

        return spec.exchange();
    }

    void cardsRequest(String lang, String expected, String authHeader) throws Exception {
        String body = cardsRequest(lang, authHeader)
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block()
                .replaceAll("[0-9]{4}[-][0-9]{2}[-][0-9]{2}T[0-9]{2}[:][0-9]{2}[:][0-9]{2}Z?", "1970-01-01T00:00:00Z")
                .replaceAll("[a-z0-9]{40,}", "test-hash");

        assertThat(
                body,
                sameJSONAs(fromFile("connector/responses/" + expected).replace("${CONCUR_BASE_URL}", mockBackend.url("")))
                        .allowingAnyArrayOrdering()
                        .allowingExtraUnexpectedFields()
        );
    }

    void mockActionRequests() throws Exception {
        mockUserLookup();
        mockUserReportsDigest();
        mockReport1();
        mockReport1Action();
    }

    void mockConcurRequests() throws Exception {
        mockUserLookup();
        mockUserReportsDigest();
        mockReport1();

        mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/683105624FD74A1B9C13"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, EXPECTED_AUTH_HEADER))
                .andRespond(withSuccess(fromFile("/fake/report-2.json").replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));

        mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/A77D016732974B5F8E23"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, EXPECTED_AUTH_HEADER))
                .andRespond(withSuccess(fromFile("/fake/report-3.json").replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));
    }

    void mockEmptyReportsDigest() throws Exception {
        mockUserLookup();

        mockBackend.expect(requestTo("/api/v3.0/expense/reportdigests?approverLoginID=admin%40acme&limit=50&user=all"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, EXPECTED_AUTH_HEADER))
                .andRespond(withSuccess(fromFile("/fake/report-digests-empty.json"), APPLICATION_JSON));
    }

    void mockUserReportsDigest() throws Exception {
        mockReportsDigest("admin%40acme");
    }

    void mockReportsDigest(String loginID) throws IOException {
        mockBackend.expect(requestTo(String.format("/api/v3.0/expense/reportdigests?approverLoginID=%s&limit=50&user=all", loginID)))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, EXPECTED_AUTH_HEADER))
                .andRespond(withSuccess(fromFile("/fake/report-digests.json").replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));
    }

    void mockReport1() throws Exception {
        mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/1D3BD2E14D144508B05F"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, EXPECTED_AUTH_HEADER))
                .andRespond(withSuccess(
                        fromFile("/fake/report-1.json")
                                .replaceAll("\\$\\{concur_host\\}", mockBackend.url("")
                                )
                        , APPLICATION_JSON));
    }

    void mockUserLookup() throws Exception {
        mockUserLookup("/fake/user-details.json");
    }

    void mockUserLookupWrongUser() throws Exception {
        mockUserLookup("/fake/user-details-wrong-user.json");
    }

    void mockUserLookupNotFound() throws IOException {
        mockUserLookup("/fake/user-details-not-found.json");
    }

    private void mockUserLookup(String responseFile) throws IOException {
        mockBackend.expect(requestTo("/api/v3.0/common/users?primaryEmail=admin%40acme.com"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, EXPECTED_AUTH_HEADER))
                .andRespond(withSuccess(fromFile(responseFile), APPLICATION_JSON));
    }

    private void mockReport1Action() {
        mockBackend.expect(requestTo("/api/expense/expensereport/v1.1/report/gWqmsMJ27KYsYDsraMCRfUtd5Y9ha96y0lRUG0nBXhO0/WorkFlowAction"))
                .andExpect(method(POST))
                .andExpect(content().contentType(APPLICATION_XML))
                .andRespond(withSuccess());
    }

    void mockOAuthToken(String serviceCredential) {
        oauthToken(serviceCredential)
                .andRespond(withSuccess(oauthToken, APPLICATION_JSON));
    }

    void mockOAuthForbiddenException(String serviceCredential) {
        oauthToken(serviceCredential)
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
    }

    ResponseActions oauthToken(String serviceCredential) {
        final MultiValueMap<String, String> body = getFormData(serviceCredential);

        return mockConcurServer.expect(requestTo("/oauth2/v0/token"))
                .andExpect(method(POST))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(body));
    }

    private MultiValueMap<String, String> getFormData(String serviceCredential) {
        final String[] authValues = serviceCredential.split(":");

        final MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.put(USERNAME, List.of(authValues[0]));
        body.put(PASSWORD, List.of(authValues[1]));
        body.put(CLIENT_ID, List.of(authValues[2]));
        body.put(CLIENT_SECRET, List.of(authValues[3]));
        body.put(GRANT_TYPE, List.of(PASSWORD));
        return body;
    }

    public static class CustomInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            final String concurUrl = HubConcurControllerTestBase.mockConcurServer.url("");
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext, "concur.oauth-instance-url=" + concurUrl);
        }
    }
}
