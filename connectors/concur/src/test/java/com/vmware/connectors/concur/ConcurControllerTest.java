/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.mock.MockWebServerWrapper;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.vmware.connectors.common.utils.CommonUtils.BACKEND_STATUS;
import static com.vmware.connectors.concur.ConcurConstants.RequestParam.REASON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

@ContextConfiguration(initializers = ConcurControllerTest.CustomInitializer.class)
@ActiveProfiles("test")
class ConcurControllerTest extends ControllerTestsBase {

    private static final String REPORT_ID_1 = "79D89435DAE94F53BF60";
    private static final String REPORT_ID_2 = "F49BD54084CE4C09BD65";
    private static final String INVALID_REPORT_ID = "invalid-report-id";

    @Value("classpath:concur/responses/approved.xml")
    private Resource approved;

    @Value("classpath:concur/responses/rejected.xml")
    private Resource rejected;

    @Value("classpath:concur/responses/oauth_token.json")
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
    void verifyMock() {
        mockConcurServer.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/cards/requests",
            "/api/expense/approve/" + REPORT_ID_1,
            "/api/expense/reject/" + REPORT_ID_2})
    void testProtectedResources(String uri) throws Exception {
        testProtectedResource(POST, uri);
    }

    @Test
    void testDiscovery() throws IOException {
        testConnectorDiscovery();
    }

    @Test
    void testRequestWithEmptyIssue() throws Exception {
        testRequestCards("emptyIssue.json", "noResults.json", null);
    }

    @Test
    void testRequestWithEmptyEmailSubject() throws Exception{
        testRequestCards("emptyEmailSubject.json", "noResults.json", null);
    }

    @DisplayName("Missing parameter cases")
    @ParameterizedTest(name = "{index} ==> ''{0}''")
    @CsvSource({
            "emptyRequest.json, noResults.json",
            "emptyToken.json, noResults.json"})
    void testRequestCardsWithMissingParameter(String requestFile, String responseFile) throws Exception {
        requestCards(requestFile)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("connector/responses/" + responseFile));
    }

    @Test
    void testGetImage() {
        webClient.get()
                .uri("/images/connector.png")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentLength(9339)
                .expectHeader().contentType(IMAGE_PNG_VALUE)
                .expectBody().consumeWith(body -> assertThat(body.getResponseBody(),
                    equalTo(bytesFromFile("/static/images/connector.png"))));
    }

    @DisplayName("Card request success cases")
    @ParameterizedTest(name = "{index} ==> Language=''{0}''")
    @CsvSource({
            ", success.json",
            "xx, success_xx.json"})
    void testRequestCardsSuccess(String lang, String resFile) throws Exception {
        expect(REPORT_ID_1).andRespond(withSuccess(
                fromFile("/concur/responses/report_id_1.json")
                        .replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));
        expect(REPORT_ID_2).andRespond(withSuccess(
                fromFile("/concur/responses/report_id_2.json")
                        .replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));

        mockConcurServer.expect(requestTo("/oauth2/v0/token"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess(oauthToken, APPLICATION_JSON));

        testRequestCards("request.json", resFile, lang);
    }

    @Test
    void testRequestCardsWithInvalidReportId() throws Exception {
        expect(REPORT_ID_1).andRespond(withSuccess(
                fromFile("/concur/responses/report_id_1.json")
                        .replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));

        expect(INVALID_REPORT_ID).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        mockConcurServer.expect(requestTo("/oauth2/v0/token"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess(oauthToken, APPLICATION_JSON));

        testRequestCards("request_with_invalid_report_id.json", "single_card_response.json", "");
    }

    @Test
    void testApproveRequest() throws Exception {
        expect(REPORT_ID_1).andRespond(withSuccess(
                fromFile("/concur/responses/report_id_1.json")
                        .replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));

        mockConcurServer.expect(requestTo("/oauth2/v0/token"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess(oauthToken, APPLICATION_JSON));

        mockExpenseReport("/api/expense/approve/",
                REPORT_ID_1,
                approved,
                "/api/expense/expensereport/v1.1/report/gWujNPAb67r9LjhqgN7BEYYaQOWzavXBtUP1sej$sXfPQ/WorkFlowAction");
    }

    @Test
    void testRejectRequest() throws Exception {
        expect(REPORT_ID_2).andRespond(withSuccess(
                fromFile("/concur/responses/report_id_2.json")
                        .replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));

        mockConcurServer.expect(requestTo("/oauth2/v0/token"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess(oauthToken, APPLICATION_JSON));

        mockExpenseReport("/api/expense/reject/",
                REPORT_ID_2,
                rejected,
                "/api/expense/expensereport/v1.1/report/gWujNPAb67r9IgBSjNrBNbeHbgDcmoJIs2kyBQX8YzEoS/WorkFlowAction");
    }

    private void mockExpenseReport(final String uri,
                                   final String expenseReportId,
                                   final Resource expectedResponse,
                                   final String workflowActionUrl) {
        mockBackend.expect(requestTo(workflowActionUrl))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"))
                .andExpect(MockRestRequestMatchers.header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_XML))
                .andRespond(withSuccess(expectedResponse, APPLICATION_XML));

        webClient.post()
                .uri(uri + expenseReportId)
                .header(AUTHORIZATION, "Bearer " + accessToken(uri + expenseReportId))
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(X_AUTH_HEADER, "Basic dXNlcm5hbWU6cGFzc3dvcmQ=")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(REASON, "Approval Done"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testEscapeHtmlTest() throws IOException {
        expect(REPORT_ID_2).andRespond(withSuccess(
                fromFile("/concur/responses/report_id_2.json")
                        .replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));

        mockBackend.expect(requestTo("/api/expense/expensereport/v1.1/report/gWujNPAb67r9IgBSjNrBNbeHbgDcmoJIs2kyBQX8YzEoS/WorkFlowAction"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"))
                .andExpect(MockRestRequestMatchers.content().contentType(APPLICATION_XML))
                // Html characters are escaped.
                .andExpect(MockRestRequestMatchers.content().xml("<WorkflowAction xmlns=\"http://www.concursolutions.com/api/expense/expensereport/2011/03\">\n" +
                        "    <Action>Send Back to Employee</Action>\n" +
                        "    <Comment>Approval &lt;/comment&gt; &lt;html&gt; &lt;/html&gt; Done</Comment>\n" +
                        "</WorkflowAction>\n"))
                .andRespond(withSuccess(approved, APPLICATION_XML));

        mockConcurServer.expect(requestTo("/oauth2/v0/token"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess(oauthToken, APPLICATION_JSON));

        String uri = "/api/expense/reject/" + REPORT_ID_2;
        webClient.post()
                .uri(uri)
                .header(AUTHORIZATION, "Bearer " + accessToken(uri))
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(X_AUTH_HEADER, "Basic dXNlcm5hbWU6cGFzc3dvcmQ=")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                // Reason with html character embedded in it.
                .body(BodyInserters.fromFormData(REASON, "Approval </comment> <html> </html> Done"))
                .exchange()
                .expectStatus().isOk();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/expense/approve/",
            "/api/expense/reject/"})
    void testUnauthorizedRequest(String uri) throws Exception {
        expect(REPORT_ID_1).andRespond(withSuccess(
                fromFile("/concur/responses/report_id_1.json")
                        .replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));
        mockBackend.expect(requestTo("/api/expense/expensereport/v1.1/report/gWujNPAb67r9LjhqgN7BEYYaQOWzavXBtUP1sej$sXfPQ/WorkFlowAction"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        mockConcurServer.expect(requestTo("/oauth2/v0/token"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess(oauthToken, APPLICATION_JSON));

        webClient.post()
                .uri(uri + REPORT_ID_1)
                .header(AUTHORIZATION, "Bearer " + accessToken(uri + REPORT_ID_1))
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(X_AUTH_HEADER, "Basic dXNlcm5hbWU6cGFzc3dvcmQ=")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(REASON, "Approval Done"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(fromFile("/connector/responses/invalid_connector_token.json"));
    }

    @Test
    void testConcurRegex() throws Exception {
        final List<String> expectedList = Arrays.asList(
                "523EEB33D8E548C1B90C",
                "623EEB33D8E548C1B902",
                "126EEB33D8E548C1B902",
                "356EEB33D8E548C1B902",
                "356EEB33D8E548C1B902",
                "923EFB33D8E548C1B902",
                "D32CBFA3289D4366B1A7");
        testRegex("expense_report_id", fromFile("concur/regex-input.txt"), expectedList);
    }

    @Test
    void testAuthSuccess() {
        mockConcurServer.expect(requestTo("/oauth2/v0/token"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess(oauthToken, APPLICATION_JSON));

        String uri = "/test-auth";
        webClient.head()
                .uri(uri)
                .header(AUTHORIZATION, "Bearer " + accessToken(uri))
                .header(X_AUTH_HEADER, "Basic dXNlcm5hbWU6cGFzc3dvcmQ=")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void testAuthFailure() {
        mockConcurServer.expect(requestTo("/oauth2/v0/token"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        String uri = "/test-auth";
        webClient.head()
                .uri(uri)
                .header(AUTHORIZATION, "Bearer " + accessToken(uri))
                .header(X_AUTH_HEADER, "Basic dXNlcm5hbWU6cGFzc3dvcmQ=")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("x-backend-status", "401");
    }

    @Test
    void testForbiddenException() throws Exception {
        mockConcurServer.expect(requestTo("/oauth2/v0/token"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        requestCards("request.json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals(BACKEND_STATUS, "401");
    }

    private void testRequestCards(final String requestFile,
                                  final String responseFile,
                                  final String acceptLanguage) throws Exception {
        final WebTestClient.RequestHeadersSpec<?> spec = requestCards(requestFile);
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
        assertThat(body, sameJSONAs(fromFile("connector/responses/" + responseFile)
                .replace("${concur_host}", mockBackend.url(""))).allowingAnyArrayOrdering());
    }

    private ResponseActions expect(final String issue) {
        return mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/" + issue))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, "Bearer abc"))
                .andExpect(MockRestRequestMatchers.header(ACCEPT, APPLICATION_JSON_VALUE));
    }

    private WebTestClient.RequestHeadersSpec<?> requestCards(final String requestFile) throws Exception {
        String uri = "/cards/requests";
        return webClient.post()
                .uri(uri)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header(X_AUTH_HEADER, "Basic dXNlcm5hbWU6cGFzc3dvcmQ=")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/concur/")
                .headers(headers -> headers(headers, uri))
                .syncBody(fromFile("/concur/requests/" + requestFile));
    }

    public static class CustomInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            final String concurUrl = ConcurControllerTest.mockConcurServer.url("");
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext, "concur.oauth-instance-url=" + concurUrl);
        }
    }
}
