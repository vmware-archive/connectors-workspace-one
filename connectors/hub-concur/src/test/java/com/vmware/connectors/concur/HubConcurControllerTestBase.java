/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.test.ControllerTestsBase;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class HubConcurControllerTestBase extends ControllerTestsBase {

    protected static final String CALLER_SERVICE_CREDS = "OAuth service-creds-from-http-request";
    protected static final String CONFIG_SERVICE_CREDS = "OAuth service-creds-from-config";

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

    @Test
    void testGetImage() {
        webClient.get()
                .uri("/images/connector.png")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentLength(9339)
                .expectHeader().contentType(IMAGE_PNG_VALUE)
                .expectBody()
                .consumeWith(body -> assertThat(body.getResponseBody(), equalTo(bytesFromFile("/static/images/connector.png"))));
    }

    protected WebTestClient.ResponseSpec testApproveRequest(final String authHeader) {
        return webClient.post().uri("/api/expense/{id}/approve", "1D3BD2E14D144508B05F")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_AUTH_HEADER, authHeader)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("comment", "Approval Done"))
                .exchange();
    }

    void mockActionRequests(final String serviceCredential) throws Exception {
        mockReportsDigest(serviceCredential);
        mockReport1(serviceCredential);
        mockReport1Action();
    }

    protected WebTestClient.ResponseSpec testRejectRequest(final String authHeader) {
        return webClient.post().uri("/api/expense/{id}/decline", "1D3BD2E14D144508B05F")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_AUTH_HEADER, authHeader)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("reason", "Decline Done"))
                .exchange();
    }

    protected void testUnauthorizedApproveRequest(final String serviceCredential) throws Exception {
        mockReportsDigest(serviceCredential);

        webClient.post().uri("/api/expense/{id}/approve", "1D3BD2E14D144508B0")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_AUTH_HEADER, CALLER_SERVICE_CREDS)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("comment", "Approval Done"))
                .exchange()
                .expectStatus().isNotFound();
    }

    protected WebTestClient.ResponseSpec testCardsRequest(final String lang, final String authHeader) throws Exception {
        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(X_AUTH_HEADER, authHeader)
                .header("x-routing-prefix", "https://hero/connectors/concur/")
                .headers(ControllerTestsBase::headers)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .syncBody(fromFile("/connector/requests/request.json"));

        if (StringUtils.isNotBlank(lang)) {
            spec.header(ACCEPT_LANGUAGE, lang);
        }

        return spec.exchange();
    }

    protected void mockConcurRequests(final String serviceCredential) throws Exception {
        mockReportsDigest(serviceCredential);
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

    protected void mockReportsDigest(final String serviceCredential) throws Exception {
        mockUserDetailReport(serviceCredential);

        mockBackend.expect(requestTo("/api/v3.0/expense/reportdigests?approverLoginID=admin%40acme.com&limit=50&user=all"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, serviceCredential))
                .andRespond(withSuccess(fromFile("/fake/report-digests.json").replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));
    }

    protected void mockReport1(final String serviceCredential) throws Exception {
        mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/1D3BD2E14D144508B05F"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, serviceCredential))
                .andRespond(withSuccess(fromFile("/fake/report-1.json").replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));
    }

    protected void mockUserDetailReport(final String serviceCredential) throws Exception {
        mockBackend.expect(requestTo("/api/v3.0/common/users?primaryEmail=admin%40acme.com"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andExpect(header(AUTHORIZATION, serviceCredential))
                .andRespond(withSuccess(fromFile("/fake/user-details.json").replace("${concur_host}", mockBackend.url("")), APPLICATION_JSON));
    }

    protected void mockReport1Action() {
        mockBackend.expect(requestTo("/api/expense/expensereport/v1.1/report/gWqmsMJ27KYsYDsraMCRfUtd5Y9ha96y0lRUG0nBXhO0/WorkFlowAction"))
                .andExpect(method(POST))
                .andExpect(content().contentType(APPLICATION_XML))
                .andRespond(withSuccess());
    }
}
