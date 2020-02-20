/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

import static com.vmware.connectors.common.utils.CommonUtils.BACKEND_STATUS;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

/**
 * Test cases with empty concur service account auth header from configuration.
 */
@TestPropertySource(locations = "classpath:empty-concur-service-credential.properties")
class HubConcurOotbTest extends HubConcurControllerTestBase {

    private static final String CALLER_SERVICE_CREDS = "username:password:client-id:client-secret-from-http-request";

    @Value("classpath:com/vmware/connectors/concur/download.pdf")
    private Resource attachment;

    @ParameterizedTest
    @CsvSource({
            ", success.json",
            "xx, success_xx.json"
    })
    void testCardsRequests(String lang, String expected) throws Exception {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockConcurRequests();
        cardsRequest(lang, expected, CALLER_SERVICE_CREDS);
    }

    @Test
    void testCardsRequestsWithMissingAuthHeader() throws Exception {
        cardsRequest("", "")
                .expectStatus().isBadRequest();
    }

    @Test
    void testApproveRequest() throws Exception {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockActionRequests();

        approveRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isOk();
    }

    @Test
    void testApprovedReqWithMissingAuthHeader() {
        approveRequest("")
                .expectStatus().isBadRequest();
    }

    @Test
    void testRejectRequest() throws Exception {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockActionRequests();

        rejectRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isOk();
    }

    @Test
    void testAttachmentAPIWithHEADForValidUser() throws Exception {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockUserLookup();
        mockReport1();
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
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockUserLookup();
        mockUserReportsDigest();

        testAttachmentWithHEAD("invalid-attachment-id")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testRejectRequestWithMissingAuthHeader() {
        rejectRequest("")
                .expectStatus().isBadRequest();
    }

    @Test
    void testUnauthorizedRejectRequest() throws Exception {
        // User tries to reject an expense report that isn't theirs
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockEmptyReportsDigest();

        rejectRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isNotFound();
    }

    @Test
    void fetchAttachmentForValidUser() throws Exception {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockUserLookup();
        mockReport1();
        mockFetchAttachment()
                .andRespond(withSuccess(attachment, APPLICATION_PDF));
        mockUserReportsDigest();

        byte[] body = getAttachment(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F")
                .exchange().expectStatus().isOk()
                .expectHeader().contentType(APPLICATION_PDF)
                .expectHeader().contentDisposition(ContentDisposition.parse("attachment; filename=\"1D3BD2E14D144508B05F.pdf\""))
                .expectBody(byte[].class).returnResult().getResponseBody();

        byte[] expected = this.attachment.getInputStream().readAllBytes();
        Assert.assertArrayEquals(expected, body);
    }

    @Test
    void fetchAttachmentForWrongUserLoginID() throws Exception {
        // Wrong user tries to fetch an expense report attachment.
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockUserLookupWrongUser();
        mockReportsDigest("wrong%40acme");

        fetchAttachmentForInvalidDetails("1D3BD2E14D144508B05F");
    }

    @Test
    void fetchAttachmentForInvalidAttachmentID() throws Exception {
        // Valid user tries to fetch an expense report attachment which does not belong to them.
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockUserLookup();
        mockUserReportsDigest();

        fetchAttachmentForInvalidDetails("invalid-attachment-id");
    }

    @Test
    void testAttachmentUrlNotFound() throws Exception {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockUserLookup();
        mockUserReportsDigest();
        mockReportWithEmptyAttachmentURL();

        fetchAttachmentForInvalidDetails("1D3BD2E14D144508B05F");
    }

    @Test
    void testUnauthorizedError() throws Exception {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockUserLookup();
        mockReport1();
        mockUserReportsDigest();
        mockFetchAttachment()
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        getAttachment(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F")
                .exchange().expectStatus().isBadRequest()
                .expectBody().json(fromFile("/connector/responses/invalid_connector_token.json"));
    }

    @Test
    void testBadStatusCode() throws Exception {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockUserLookup();
        mockReport1();
        mockUserReportsDigest();
        mockFetchAttachment()
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        getAttachment(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F")
                .exchange().expectStatus().is5xxServerError();
    }

    @ParameterizedTest
    @CsvSource({
            // Service account credential length < 4
            "username:client-id:client-secret",
            // Service account credential length > 4
            "username:password:client-id:client-secret:junk"
    })
    void testInvalidServiceAccountCredential(String serviceCredential) {
        getAttachment(serviceCredential, "1D3BD2E14D144508B05F")
                .exchange().expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401");
    }

    @Test
    void testUnauthorizedServiceAccountCredential() {
        mockConcurServer.expect(requestTo("/oauth2/v0/token"))
                .andRespond(withUnauthorizedRequest());

        getAttachment(CALLER_SERVICE_CREDS, "1D3BD2E14D144508B05F")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals("X-Backend-Status", "401");
    }

    @Test
    void testCardReqWhenUserNotFound() throws Exception {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockUserLookupNotFound();

        cardsRequest("", CALLER_SERVICE_CREDS)
                .expectStatus().isOk()
                .expectBody().json(fromFile("connector/responses/empty_response.json"));
    }

    @Test
    void testCardReqWithEmptyApprovals() throws Exception {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockEmptyReportsDigest();

        cardsRequest("", CALLER_SERVICE_CREDS)
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(fromFile("connector/responses/empty_response.json"));
    }

    @Test
    void testApproveWhenUserNotFound() throws IOException {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockUserLookupNotFound();

        approveRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isNotFound()
                .expectBody().json(fromFile("connector/responses/user_email_not_found.json"));
    }

    @Test
    void testApproveForWrongUser() throws Exception {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockUserLookupWrongUser();
        mockReportsDigest("wrong%40acme");

        approveRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isNotFound();
    }

    @Test
    void testRejectWhenUserNotFound() throws IOException {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockUserLookupNotFound();

        rejectRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isNotFound()
                .expectBody().json(fromFile("connector/responses/user_email_not_found.json"));
    }

    @Test
    void testRejectForWrongUser() throws Exception {
        mockOAuthToken(CALLER_SERVICE_CREDS);
        mockUserLookupWrongUser();
        mockReportsDigest("wrong%40acme");

        rejectRequest(CALLER_SERVICE_CREDS)
                .expectStatus().isNotFound()
                .expectBody().json(fromFile("connector/responses/report_not_found.json"));
    }

    @Test
    void testForbiddenException() throws Exception {
        mockOAuthForbiddenException(CALLER_SERVICE_CREDS);

        cardsRequest("", CALLER_SERVICE_CREDS)
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals(BACKEND_STATUS, "401");
    }

    private void fetchAttachmentForInvalidDetails(String attachmentId) {
        getAttachment(CALLER_SERVICE_CREDS, attachmentId)
                .exchange().expectStatus().isNotFound();
    }

    private WebTestClient.RequestHeadersSpec<?> getAttachment(String serviceCredential, String attachmentId) {
        String uri = String.format("/api/expense/report/%s/attachment", attachmentId);
        return webClient.get()
                .uri(uri)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(X_AUTH_HEADER, serviceCredential)
                .headers(headers -> headers(headers, uri));
    }

    private WebTestClient.RequestHeadersSpec testAttachmentWithHEAD(String reportId) {
        String uri = String.format("/api/expense/report/%s/attachment", reportId);

        return webClient.head()
                .uri(uri)
                .header(X_AUTH_HEADER, CALLER_SERVICE_CREDS)
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

}
