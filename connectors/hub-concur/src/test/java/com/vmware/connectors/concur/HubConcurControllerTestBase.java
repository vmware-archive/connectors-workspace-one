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
import org.junit.Assert;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

@ContextConfiguration(initializers = HubConcurControllerTestBase.CustomInitializer.class)
class HubConcurControllerTestBase extends ControllerTestsBase {

    static final String CALLER_SERVICE_CREDS = "username:password:client-id:client-secret-from-http-request";
    static final String CONFIG_SERVICE_CREDS = "username:password:client-id:client-secret-from-config";

    static final String EXPECTED_AUTH_HEADER = "Bearer test-access-token";

    protected static final String CLIENT_ID = "client_id";
    protected static final String CLIENT_SECRET = "client_secret";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String GRANT_TYPE = "grant_type";

    @Value("classpath:com/vmware/connectors/concur/download.pdf")
    private Resource attachment;

    @Value("classpath:fake/oauth_token.json")
    private Resource oauthToken;

    private static MockWebServerWrapper mockConcurServer;

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

    void testServiceAccountCredential(String serviceCredential) {
        getAttachment(serviceCredential, "1D3BD2E14D144508B05F")
                .exchange().expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401");
    }

    void fetchAttachment(String serviceCredential, String attachmentId) throws IOException {
        byte[] body = getAttachment(serviceCredential, attachmentId)
                .exchange().expectStatus().isOk()
                .expectHeader().contentType(APPLICATION_PDF)
                .expectHeader().contentDisposition(ContentDisposition.parse("Content-Disposition: inline; filename=\"1D3BD2E14D144508B05F.pdf\""))
                .expectBody(byte[].class).returnResult().getResponseBody();

        byte[] expected = this.attachment.getInputStream().readAllBytes();
        Assert.assertArrayEquals(expected, body);
    }

    void fetchAttachmentForInvalidDetails(String serviceCredential, String attachmentId) {
        getAttachment(serviceCredential, attachmentId)
                .exchange().expectStatus().isNotFound();
    }

    void fetchAttachmentForUnauthorizedCredential(String serviceCredential, String attachmentId) throws IOException {
        getAttachment(serviceCredential, attachmentId)
                .exchange().expectStatus().isBadRequest()
                .expectBody().json(fromFile("/connector/responses/invalid_connector_token.json"));
    }

    void fetchAttachmentWithBadStatusCode(String serviceCredential, String attachmentId) throws IOException {
        getAttachment(serviceCredential, attachmentId)
                .exchange().expectStatus().is5xxServerError();
    }

    void unauthorizedServiceAccountCredential() {
        mockConcurServer.expect(requestTo("/oauth2/v0/token"))
                .andRespond(withUnauthorizedRequest());

        getAttachment(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401");
    }

    private WebTestClient.RequestHeadersSpec<?> getAttachment(String serviceCredential, String attachmentId) {
        String uri = String.format("/api/expense/report/%s/attachment", attachmentId);
        return webClient.get()
                .uri(uri)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(X_AUTH_HEADER, serviceCredential)
                .headers(headers -> headers(headers, uri));
    }

    void mockActionRequests(String serviceCredential) throws Exception {
        mockUserReportsDigest(serviceCredential);
        mockReport1(serviceCredential);
        mockReport1Action();
    }

    void mockConcurRequests(String serviceCredential) throws Exception {
        mockUserReportsDigest(serviceCredential);
        mockReport1(serviceCredential);

        mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/683105624FD74A1B9C13"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, serviceCredential))
                .andRespond(withSuccess(fromFile("/fake/report-2.json").replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));

        mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/A77D016732974B5F8E23"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, serviceCredential))
                .andRespond(withSuccess(fromFile("/fake/report-3.json").replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));
    }

    void mockFetchAttachment(String serviceCredential) {
        mockBackend.expect(requestTo("/file/t0030426uvdx/C720AECBB775A1D24B70DAF086760A9C5BA3ECDE4423886FAB4A72C717A584E3DA4B78A36E0F24651A84FC091F6E434DEAD2A464F8CF60EFFAB96F456DFD3188H9AAD83239F0E2B9D554093BEAF888BF4?id=1D3BD2E14D144508B05F&e=t0030426uvdx&t=AN&s=ConcurConnect"))
                .andExpect(method(GET))
                .andExpect(header(AUTHORIZATION, serviceCredential))
                .andRespond(withSuccess(attachment, APPLICATION_PDF));
    }

    void mockFetchAttachmentWithUnauthorized(String serviceCredential) {
        mockBackend.expect(requestTo("/file/t0030426uvdx/C720AECBB775A1D24B70DAF086760A9C5BA3ECDE4423886FAB4A72C717A584E3DA4B78A36E0F24651A84FC091F6E434DEAD2A464F8CF60EFFAB96F456DFD3188H9AAD83239F0E2B9D554093BEAF888BF4?id=1D3BD2E14D144508B05F&e=t0030426uvdx&t=AN&s=ConcurConnect"))
                .andExpect(method(GET))
                .andExpect(header(AUTHORIZATION, serviceCredential))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
    }

    void mockFetchAttachmentWithInternalServerError(String serviceCredential) {
        mockBackend.expect(requestTo("/file/t0030426uvdx/C720AECBB775A1D24B70DAF086760A9C5BA3ECDE4423886FAB4A72C717A584E3DA4B78A36E0F24651A84FC091F6E434DEAD2A464F8CF60EFFAB96F456DFD3188H9AAD83239F0E2B9D554093BEAF888BF4?id=1D3BD2E14D144508B05F&e=t0030426uvdx&t=AN&s=ConcurConnect"))
                .andExpect(method(GET))
                .andExpect(header(AUTHORIZATION, serviceCredential))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    void mockEmptyReportsDigest(String expectedServiceCredential) throws Exception {
        mockUserDetailReport(expectedServiceCredential, "/fake/user-details.json");

        mockBackend.expect(requestTo("/api/v3.0/expense/reportdigests?approverLoginID=admin%40acme.com&limit=50&user=all"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, expectedServiceCredential))
                .andRespond(withSuccess(fromFile("/fake/empty-report-digest.json"), APPLICATION_JSON));
    }

    void mockUserReportsDigest(String serviceCredential) throws Exception {
        mockUserDetailReport(serviceCredential, "/fake/user-details.json");

        mockReportsDigest(serviceCredential, "admin%40acme.com");
    }

    void mockReportsDigest(String serviceCredential, String loginID) throws IOException {
        mockBackend.expect(requestTo(String.format("/api/v3.0/expense/reportdigests?approverLoginID=%s&limit=50&user=all", loginID)))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, serviceCredential))
                .andRespond(withSuccess(fromFile("/fake/report-digests.json").replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));
    }

    void mockReport1(String serviceCredential) throws Exception {
        mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/1D3BD2E14D144508B05F"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, serviceCredential))
                .andRespond(withSuccess(
                                fromFile("/fake/report-1.json")
                                .replaceAll("\\$\\{concur_host\\}", mockBackend.url("")
                        )
                        , APPLICATION_JSON));
    }

    void mockReportWithEmptyAttachmentURL(String serviceCredential) throws IOException {
        mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/1D3BD2E14D144508B05F"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, serviceCredential))
                .andRespond(withSuccess(fromFile("/fake/report-with-empty-attachment-url.json"), APPLICATION_JSON));
    }

    void mockUserDetailReport(String serviceCredential, String userDetails) throws Exception {
        mockBackend.expect(requestTo("/api/v3.0/common/users?primaryEmail=admin%40acme.com"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, serviceCredential))
                .andRespond(withSuccess(fromFile(userDetails).replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));
    }

    void mockOAuthToken(String serviceCredential) {
        final String[] authValues = serviceCredential.split(":");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.put(USERNAME, List.of(authValues[0]));
        body.put(PASSWORD, List.of(authValues[1]));
        body.put(CLIENT_ID, List.of(authValues[2]));
        body.put(CLIENT_SECRET, List.of(authValues[3]));
        body.put(GRANT_TYPE, List.of(PASSWORD));

        mockConcurServer.expect(requestTo("/oauth2/v0/token"))
                .andExpect(method(POST))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(body))
                .andRespond(withSuccess(oauthToken, APPLICATION_JSON));
    }

    void mockReport1Action() {
        mockBackend.expect(requestTo("/api/expense/expensereport/v1.1/report/gWqmsMJ27KYsYDsraMCRfUtd5Y9ha96y0lRUG0nBXhO0/WorkFlowAction"))
                .andExpect(method(POST))
                .andExpect(content().contentType(APPLICATION_XML))
                .andRespond(withSuccess());
    }

    void mockInvalidUserDetails(String userDetails) throws IOException {
        mockBackend.expect(requestTo("/api/v3.0/common/users?primaryEmail=admin%40acme.com"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(fromFile(userDetails), APPLICATION_JSON));
    }

    public static class CustomInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            final String concurUrl = HubConcurControllerTestBase.mockConcurServer.url("");
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext, "concur.oauth-instance-url=" + concurUrl);
        }
    }
}
