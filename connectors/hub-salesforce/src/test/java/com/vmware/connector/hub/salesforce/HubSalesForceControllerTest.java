/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connector.hub.salesforce;


import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

class HubSalesForceControllerTest extends ControllerTestsBase {

    private static final String SOQL_QUERY_PATH = "/services/data/v44.0/query";

    private final static String WORK_ITEMS_QUERY = "SELECT Id,TargetObjectid, Status,(select id,actor.name, actor.id, actor.email, actor.username from Workitems Where actor.email = '%s'),(SELECT Id, StepStatus, Comments,Actor.Name, Actor.Id, actor.email, actor.username FROM Steps) FROM ProcessInstance Where Status = 'Pending'";

    private final static String OPPORTUNITY_QUERY = "SELECT Id, Name, FORMAT(ExpectedRevenue), Account.Owner.Name FROM opportunity WHERE Id IN ('%s')";

    @Value("classpath:workflow_step_result.json")
    private Resource workflowStepApproval;

    @Value("classpath:opportunity_result.json")
    private Resource opportunityResult;

    @Value("classpath:/connector/responses/empty_workflow.json")
    private Resource emptyWorkflow;

    @ParameterizedTest
    @ValueSource(strings = {
            "/cards/requests",
            "/api/expense/approve/04i41000003hSU2AAM",
            "/api/expense/reject/04i41000003hSU2AAM"
    })
    void testProtectedResources(final String uri) throws Exception {
        testProtectedResource(POST, uri);
    }

    @Test
    void testDiscovery() throws IOException {
        testConnectorDiscovery();
    }

    @Test
    void testConnectorImage() {
        webClient.get()
                .uri("/images/connector.png")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(IMAGE_PNG_VALUE)
                .expectHeader().contentLength(7314)
                .expectBody().consumeWith(body -> assertThat(body.getResponseBody(), equalTo(bytesFromFile("/static/images/connector.png"))));
    }

    @Test
    public void testMissingRequestHeaders() throws IOException {
        webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("X-Connector-Authorization", "Bearer abc")
                .syncBody(fromFile("request.json"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Missing request header 'X-Connector-Base-Url' for method parameter of type String");
    }

    @ParameterizedTest
    @CsvSource({
            StringUtils.EMPTY + ",success.json",
            "xx," + "success_xx.json"
    })
    void testRequestCardSuccess(final String lang, final String responseFile) throws Exception {
        expectSalesforceRequest(String.format(WORK_ITEMS_QUERY, "admin@acme.com"))
                .andRespond(withSuccess(workflowStepApproval, APPLICATION_JSON));

        expectSalesforceRequest(String.format(OPPORTUNITY_QUERY, "0064100000b9dQsAAI', '0064100000b9S8EAAU"))
                .andRespond(withSuccess(opportunityResult, APPLICATION_JSON));

        testRequestCards(responseFile, lang);
    }

    @Test
    void testApproveRequest() {
        setupMock();

        webClient.post()
                .uri("/api/expense/approve/00541000000osYgAAI")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_AUTH_HEADER, "Bearer abc")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData("reason", "Approved"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testRejectRequest() {
        setupMock();

        webClient.post()
                .uri("/api/expense/reject/00541000000osYgAAI")
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .header(X_AUTH_HEADER, "Bearer abc")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData("reason", "discount approval rejected"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testCardRequestWithEmptyWorkflow() throws Exception {
        expectSalesforceRequest(String.format(WORK_ITEMS_QUERY, "admin@acme.com"))
                .andRespond(withSuccess(emptyWorkflow, APPLICATION_JSON));

        testRequestCards("empty_card_response.json", StringUtils.EMPTY);
    }

    private void setupMock() {
        mockBackend.expect(requestTo("/services/data/v44.0/process/approvals/"))
                .andExpect(method(POST))
                .andExpect(header(AUTHORIZATION, "Bearer abc"))
                .andExpect(header(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess());
    }

    private void testRequestCards(String responseFile, String acceptLanguage) throws Exception {
        WebTestClient.RequestHeadersSpec<?> spec = requestCards("abc", "request.json");
        if (acceptLanguage != null) {
            spec = spec.header(ACCEPT_LANGUAGE, acceptLanguage);
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
        assertThat(body, sameJSONAs(fromFile("connector/responses/" + responseFile)).allowingAnyArrayOrdering());
    }

    private WebTestClient.RequestHeadersSpec<?> requestCards(String authToken, String filePath) throws Exception {
        return webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header(X_AUTH_HEADER, "Bearer " + authToken)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/salesforce/")
                .headers(ControllerTestsBase::headers)
                .syncBody(fromFile(filePath));
    }

    private ResponseActions expectSalesforceRequest(final String sql) {
        final URI uri = UriComponentsBuilder
                .fromPath(SOQL_QUERY_PATH)
                .queryParam("q", sql)
                .build()
                .toUri();

        return mockBackend.expect(requestTo(uri))
                .andExpect(method(GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc"));
    }
}
