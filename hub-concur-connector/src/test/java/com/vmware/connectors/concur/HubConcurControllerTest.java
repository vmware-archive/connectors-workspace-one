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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;


class HubConcurControllerTest extends ControllerTestsBase    {

    private static final String AUTH_HEADER_VAL = "Bearer test-access-token";

    private static final String EXPECTED_AUTH_HEADER = "Bearer test-access-token";

    @Value("classpath:com/vmware/connectors/concur/download.pdf")
    private Resource attachment;

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

    @ParameterizedTest
    @CsvSource({
            ", success.json,",
            "xx, success_xx.json,"
    })
    void testCardsRequests(String lang, String expected) throws Exception {
        mockConcurRequests();
        cardsRequest(lang, expected, AUTH_HEADER_VAL);
    }

    @Test
    void testCardForMultiCurrencyReport() throws Exception {
        mockUserLookup();
        mockReportsDigest("admin%40acme", "/fake/report-digests-b.json");

        mockReport("43B451635CF24B39A670", "/fake/report-b1.json");

        cardsRequest(null, "success_b.json", AUTH_HEADER_VAL);
    }

    @Test
    void testCardReqWhenUserNotFound() throws Exception {
        mockUserLookupNotFound();
        cardsRequest("", AUTH_HEADER_VAL)
                .expectStatus().isOk()
                .expectBody().json(fromFile("connector/responses/empty_response.json"));
    }

    @Test
    public void testMissingRequestHeaders() throws Exception {
        cardsRequestMissingBaseUrl()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Missing request header 'X-Connector-Base-Url' for method parameter of type String");
    }

    @Test
    void testCardReqWithEmptyApprovals() throws Exception {
        mockUserLookup();
        mockEmptyReportsDigest();
        cardsRequest("", AUTH_HEADER_VAL)
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("connector/responses/empty_response.json"));
    }

    @Test
    void testCardsRequestsWithMissingAuthHeader() throws Exception {
        cardsRequest("", "")
                .expectStatus().isBadRequest();
    }

    @Test
    void testApproveRequests() throws Exception {
        mockActionRequests("/fake/workflow-action-success.json");
        approveRequest(AUTH_HEADER_VAL, "")
                .expectStatus()
                .isOk();
    }

    @Test
    void testApproveWhenUserNotFound() throws IOException {
        mockUserLookupNotFound();
        approveRequest(AUTH_HEADER_VAL, "")
                .expectStatus().isNotFound()
                .expectBody().json(fromFile("connector/responses/user_email_not_found.json"));
    }

    @Test
    void testApproveForWrongUser() throws Exception {
        mockUserLookupWrongUser();
        mockReportsDigest("wrong%40acme", "/fake/report-digests.json");

        approveRequest(AUTH_HEADER_VAL, "")
                .expectStatus().isNotFound();
    }

    @Test
    void testApprovedReqWithMissingAuthHeader() {
        approveRequest("", "")
                .expectStatus().isBadRequest();
    }

    @Test
    void testUnauthorizedApproveRequest() throws Exception {
        // User tries to approve an expense report that isn't theirs
        mockUserLookup();
        mockEmptyReportsDigest();
        approveRequest(AUTH_HEADER_VAL, "")
                .expectStatus().isNotFound();
    }

    /*
     * When approver has "prompt" setting turned ON the approve action fails.
     * The settings can be found in user profile Expense preferences.
     */
    @Test
    void testApproveWhenUserHasPromptSetting() throws Exception {
        mockActionRequests("/fake/workflow-action-failure.json");
        approveRequest(AUTH_HEADER_VAL, "")
                .expectStatus().isBadRequest()
                .expectBody()
                .json(fromFile("connector/responses/workflow_action_failure.json")
                        .replace("${concur_host}", mockBackend.url("")));
    }

    @Test
    void testApproveWhenUserHasPromptSetting_XX() throws Exception {
        mockActionRequests("/fake/workflow-action-failure.json");
        approveRequest(AUTH_HEADER_VAL, "xx")
                .expectStatus().isBadRequest()
                .expectBody()
                .json(fromFile("connector/responses/workflow_action_failure_xx.json")
                        .replace("${concur_host}", mockBackend.url("")));
    }

    // Reject works irrespective of prompt settings.
    @Test
    void testRejectRequest() throws Exception {
        mockActionRequests("/fake/workflow-action-success.json");
        rejectRequest(AUTH_HEADER_VAL, "")
                .expectStatus().isOk();
    }

    @Test
    void testRejectWhenUserNotFound() throws IOException {
        mockUserLookupNotFound();
        rejectRequest(AUTH_HEADER_VAL, "")
                .expectStatus().isNotFound()
                .expectBody().json(fromFile("connector/responses/user_email_not_found.json"));
    }

    /*
     * It either means wrong user is trying to access the report or,
     * The report no longer exists in user's pending aprrovals list.
     */
    @Test
    void testRejectForWrongUser() throws Exception {
        mockUserLookupWrongUser();
        mockReportsDigest("wrong%40acme", "/fake/report-digests.json");

        rejectRequest(AUTH_HEADER_VAL, "")
                .expectStatus().isNotFound()
                .expectBody()
                .json(fromFile("connector/responses/report_not_found.json")
                        .replace("${concur_host}", mockBackend.url("")));
    }

    @Test
    void testRejectForWrongUser_XX() throws Exception {
        mockUserLookupWrongUser();
        mockReportsDigest("wrong%40acme", "/fake/report-digests.json");

        rejectRequest(AUTH_HEADER_VAL, "xx")
                .expectStatus().isNotFound()
                .expectBody()
                .json(fromFile("connector/responses/report_not_found_xx.json")
                        .replace("${concur_host}", mockBackend.url("")));
    }

    @Test
    void testRejectRequestWithMissingAuthHeader() {
        rejectRequest("", "")
                .expectStatus().isBadRequest();
    }

    @Test
    void testUnauthorizedRejectRequest() throws Exception {
        // User tries to reject an expense report that isn't theirs
        mockUserLookup();
        mockEmptyReportsDigest();
        rejectRequest(AUTH_HEADER_VAL, "")
                .expectStatus().isNotFound();
    }

    @Test
    void testAttachmentAPIWithHEADForValidUser() throws Exception {
        mockUserLookup();
        mockReport("1D3BD2E14D144508B05F", "/fake/report-1.json");
        mockFetchAttachment()
                .andRespond(withSuccess(attachment, APPLICATION_PDF));
        mockUserReportsDigest();

        testAttachmentWithHEAD("1D3BD2E14D144508B05F")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testAttachmentAPIWithHEADForInvalidUser() throws Exception {
        // Valid user tries to fetch an expense report attachment which does not belong to them.
        mockUserLookup();
        mockUserReportsDigest();

        testAttachmentWithHEAD("invalid-attachment-id")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void fetchAttachmentForValidUser() throws Exception {
        mockUserLookup();
        mockReport("1D3BD2E14D144508B05F", "/fake/report-1.json");
        mockFetchAttachment()
                .andRespond(withSuccess(attachment, APPLICATION_PDF));
        mockUserReportsDigest();

        byte[] body = getAttachment("1D3BD2E14D144508B05F")
                .exchange().expectStatus().isOk()
                .expectHeader().contentType(APPLICATION_PDF)
                .expectHeader().contentDisposition(ContentDisposition.parse("attachment; filename=\"All Receipts.pdf\""))
                .expectBody(byte[].class).returnResult().getResponseBody();

        byte[] expected = this.attachment.getInputStream().readAllBytes();
        Assert.assertArrayEquals(expected, body);
    }

    @Test
    void fetchAttachmentForWrongUserLoginID() throws Exception {
        // Wrong user tries to fetch an expense report attachment.
        mockUserLookupWrongUser();
        mockReportsDigest("wrong%40acme", "/fake/report-digests.json");
        fetchAttachmentForInvalidDetails("1D3BD2E14D144508B05F");
    }

    @Test
    void fetchAttachmentForInvalidAttachmentID() throws Exception {
        // Valid user tries to fetch an expense report attachment which does not belong to them.
        mockUserLookup();
        mockUserReportsDigest();
        fetchAttachmentForInvalidDetails("invalid-attachment-id");
    }

    @Test
    void testAttachmentUrlNotFound() throws Exception {
        mockUserLookup();
        mockUserReportsDigest();
        mockReportWithEmptyAttachmentURL();
        fetchAttachmentForInvalidDetails("1D3BD2E14D144508B05F");
    }

    @Test
    void testUnauthorizedError() throws Exception {
        mockUserLookup();
        mockReport("1D3BD2E14D144508B05F", "/fake/report-1.json");
        mockUserReportsDigest();
        mockFetchAttachment()
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        getAttachment("1D3BD2E14D144508B05F")
                .exchange().expectStatus().isBadRequest()
                .expectBody().json(fromFile("/connector/responses/invalid_connector_token.json"));
    }

    @Test
    void testBadStatusCode() throws Exception {
        mockUserLookup();
        mockReport("1D3BD2E14D144508B05F", "/fake/report-1.json");
        mockUserReportsDigest();
        mockFetchAttachment()
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        getAttachment("1D3BD2E14D144508B05F")
                .exchange().expectStatus().is5xxServerError();
    }

    private void fetchAttachmentForInvalidDetails(String attachmentId) {
        getAttachment(attachmentId)
                .exchange().expectStatus().isNotFound();
    }

    private WebTestClient.RequestHeadersSpec<?> getAttachment(String attachmentId) {
        String uri = String.format("/api/expense/report/%s/attachment", attachmentId);
        return webClient.get()
                .uri(uri)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(X_AUTH_HEADER, AUTH_HEADER_VAL)
                .headers(headers -> headers(headers, uri));
    }

    private WebTestClient.RequestHeadersSpec<?> testAttachmentWithHEAD(String reportId) {
        String uri = String.format("/api/expense/report/%s/attachment", reportId);

        return webClient.head()
                .uri(uri)
                .header(X_AUTH_HEADER, AUTH_HEADER_VAL)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .headers(headers -> headers(headers, uri));
    }

    private ResponseActions mockFetchAttachment() {
        return mockBackend.expect(requestTo("/file/t0030426uvdx/C720AECBB775A1D24B70DAF086760A9C5BA3ECDE4423886FAB4A72C717A584E3DA4B78A36E0F24651A84FC091F6E434DEAD2A464F8CF60EFFAB96F456DFD3188H9AAD83239F0E2B9D554093BEAF888BF4?id=1D3BD2E14D144508B05F&e=t0030426uvdx&t=AN&s=ConcurConnect"))
                .andExpect(method(GET))
                .andExpect(header(AUTHORIZATION, EXPECTED_AUTH_HEADER));
    }

    private void mockReportWithEmptyAttachmentURL() throws IOException {
        mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/1D3BD2E14D144508B05F"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, EXPECTED_AUTH_HEADER))
                .andRespond(withSuccess(fromFile("/fake/report-with-empty-attachment-url.json"), APPLICATION_JSON));
    }

    private void mockConcurRequests() throws Exception {
        mockUserLookup();
        mockUserReportsDigest();
        mockReport("1D3BD2E14D144508B05F", "/fake/report-1.json");

        mockReport("683105624FD74A1B9C13", "/fake/report-2.json");

        mockReport("A77D016732974B5F8E23", "/fake/report-3.json");
    }

    private void cardsRequest(String lang, String expected, String authHeader) throws Exception {
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

    private WebTestClient.ResponseSpec cardsRequest(String lang, String authHeader) throws Exception {
        String uri = "/cards/requests";
        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(uri)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/concur/card/")
                .headers(headers -> headers(headers, uri))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .bodyValue(fromFile("/connector/requests/request.json"));

        if (StringUtils.isNotBlank(authHeader)) {
            spec.header(X_AUTH_HEADER, authHeader);
        }
        if (StringUtils.isNotBlank(lang)) {
            spec.header(ACCEPT_LANGUAGE, lang);
        }
        return spec.exchange();
    }

    private WebTestClient.ResponseSpec cardsRequestMissingBaseUrl() throws Exception {
        String uri = "/cards/requests";
        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri(uri)
                .header("x-routing-prefix", "https://hero/connectors/concur/card/")
                .header(ACCEPT_LANGUAGE, "lang")
                .header(X_AUTH_HEADER, EXPECTED_AUTH_HEADER)
                .headers(headers -> headers(headers, uri))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .bodyValue(fromFile("/connector/requests/request.json"));

        return spec.exchange();
    }

    private void mockUserLookup() throws Exception {
        mockUserLookup("/fake/user-details.json");
    }

    private void mockUserReportsDigest() throws Exception {
        mockReportsDigest("admin%40acme", "/fake/report-digests.json");
    }

    private void mockReportsDigest(String loginID, String resFile) throws IOException {
        mockBackend.expect(requestTo(String.format("/api/v3.0/expense/reportdigests?approverLoginID=%s&limit=50&user=all", loginID)))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, EXPECTED_AUTH_HEADER))
                .andRespond(withSuccess(fromFile(resFile).replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));
    }

    private void mockReport(String reportId, String resFile) throws Exception {
        mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/" + reportId))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, EXPECTED_AUTH_HEADER))
                .andRespond(withSuccess(
                        fromFile(resFile)
                                .replace("${concur_host}", mockBackend.url("")
                                )
                        , APPLICATION_JSON));
    }

    private void mockActionRequests(String actionResFile) throws Exception {
        mockUserLookup();
        mockUserReportsDigest();
        mockReport("1D3BD2E14D144508B05F", "/fake/report-1.json");
        mockReport1Action(actionResFile);
    }

    private void mockReport1Action(String resFile) throws IOException {
        mockBackend.expect(requestTo("/api/expense/expensereport/v1.1/report/gWqmsMJ27KYsYDsraMCRfUtd5Y9ha96y0lRUG0nBXhO0/WorkFlowAction"))
                .andExpect(method(POST))
                .andExpect(content().contentType(APPLICATION_XML))
                .andRespond(withSuccess()
                        .contentType(APPLICATION_JSON)
                        .body(fromFile(resFile))
                );
    }

    private WebTestClient.ResponseSpec approveRequest(String authHeader, String lang) {
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

        if (StringUtils.isNotBlank(lang)) {
            spec.header(ACCEPT_LANGUAGE, lang);
        }

        return spec.exchange();
    }

   private WebTestClient.ResponseSpec rejectRequest(String authHeader, String lang) {
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

       if (StringUtils.isNotBlank(lang)) {
           spec.header(ACCEPT_LANGUAGE, lang);
       }

        return spec.exchange();
    }

    private void mockEmptyReportsDigest() throws Exception {
        mockReportsDigest("admin%40acme", "/fake/report-digests-empty.json");
    }

    private void mockUserLookupWrongUser() throws Exception {
        mockUserLookup("/fake/user-details-wrong-user.json");
    }

    private void mockUserLookup(String responseFile) throws IOException {
        mockBackend.expect(requestTo("/api/v3.0/common/users?primaryEmail=admin%40acme.com"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, EXPECTED_AUTH_HEADER))
                .andRespond(withSuccess(fromFile(responseFile), APPLICATION_JSON));
    }

    private void mockUserLookupNotFound() throws IOException {
        mockUserLookup("/fake/user-details-not-found.json");
    }
}
