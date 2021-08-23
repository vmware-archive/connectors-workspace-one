/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connector.hub.salesforce;


import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.vmware.connectors.test.ControllerTestsBase;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.client.ExpectedCount.between;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HubSalesForceControllerTest extends ControllerTestsBase {

    private static final String SOQL_QUERY_PATH = "/services/data/v44.0/query";

    private final static String WORK_ITEMS_QUERY = "SELECT Id,TargetObjectid, Status,(select id,actor.name, actor.id, actor.email, actor.username from Workitems Where actor.email = '%s'),(SELECT Id, StepStatus, Comments,Actor.Name, Actor.Id, actor.email, actor.username FROM Steps) FROM ProcessInstance Where Status = 'Pending'";

    private final static String OPPORTUNITY_QUERY = "SELECT Id, Name, FORMAT(ExpectedRevenue), Account.Owner.Name, Discount_Percentage__c, Reason_for_Discount__c FROM opportunity WHERE Id IN ('%s')";

    @Value("classpath:workflow_step_result.json")
    private Resource workflowStepApproval;

    @Value("classpath:opportunity_result.json")
    private Resource opportunityResult;

    @Value("classpath:connector/responses/empty_records.json")
    private Resource emptyRecords;

    @Value("classpath:connector/responses/empty_work_items.json")
    private Resource emptyWorkItems;

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
    public void testMissingRequestHeaders() throws IOException {
        String uri = "/cards/requests";
        webClient.post()
                .uri(uri)
                .header(AUTHORIZATION, "Bearer " + accessToken(uri))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("X-Connector-Authorization", "Bearer abc")
                .bodyValue(fromFile("request.json"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Missing request header 'X-Connector-Base-Url' for method parameter of type String");
    }

    @ParameterizedTest
    @CsvSource({
            ", success.json",
            "xx, success_xx.json"
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

        String uri = "/api/expense/approve/00541000000osYgAAI";
        webClient.post()
                .uri(uri)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer " + accessToken(uri))
                .header(X_AUTH_HEADER, "Bearer abc")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData("reason", "Approved"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testRejectRequest() {
        setupMock();

        String uri = "/api/expense/reject/00541000000osYgAAI";
        webClient.post()
                .uri(uri)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer " + accessToken(uri))
                .header(X_AUTH_HEADER, "Bearer abc")
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData("reason", "discount approval rejected"))
                .exchange()
                .expectStatus().isOk();
    }

    @ParameterizedTest
    @MethodSource("emptyWorkFlowArgument")
    void testCardRequestWithEmptyWorkflow(final String email, final Resource resource) throws Exception {
        expectSalesforceRequest(String.format(WORK_ITEMS_QUERY, email))
                .andRespond(withSuccess(resource, APPLICATION_JSON));

        expectSalesforceRequest(String.format(OPPORTUNITY_QUERY, "0062E00001Em72DQAR', '0062E00001Em757QAB', '0062E00001Em7iBQAR', '0062E00001Em7kWQAR', '0062E00001Em7lyQAB"))
                .andRespond(withSuccess(opportunityResult, APPLICATION_JSON));

        testRequestCards("empty_card_response.json", "");
    }

    private Stream<Arguments> emptyWorkFlowArgument() {
        return Stream.of(
                Arguments.of("admin@acme.com", emptyRecords),
                Arguments.of("admin@acme.com", emptyWorkItems)
        );
    }

    @ParameterizedTest
    @CsvSource({
            "config_missing.json",
            "discount_percentage_missing.json",
            "discount_reason_missing.json",
            "invalid_discount_percentage_field_name.json",
            "invalid_discount_reason_field_name.json"
    })
    void testInvalidConfigParams(String fileName) throws Exception {
        requestCards("abc", "invalid/request/" + fileName)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(fromFile("invalid/response/" + fileName));
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
        if (StringUtils.isNotBlank(acceptLanguage)) {
            spec = spec.header(ACCEPT_LANGUAGE, acceptLanguage);
        }

        String body = spec.exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(this::normalizeCards)
                .block();
        body = body.replaceAll("[a-z0-9]{40,}", "test-hash");
        assertThat(
                body,
                sameJSONAs(fromFile("connector/responses/" + responseFile).replace("${SF_BASE_URL}", mockBackend.url("")))
                        .allowingAnyArrayOrdering()
        );
    }

    private WebTestClient.RequestHeadersSpec<?> requestCards(String authToken, String filePath) throws Exception {
        String uri = "/cards/requests";
        return webClient.post()
                .uri(uri)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header(X_AUTH_HEADER, "Bearer " + authToken)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/salesforce/")
                .headers(headers -> headers(headers, uri))
                .bodyValue(fromFile(filePath));
    }

    private ResponseActions expectSalesforceRequest(final String sql) {
        final URI uri = UriComponentsBuilder
                .fromPath(SOQL_QUERY_PATH)
                .queryParam("q", sql)
                .build()
                .toUri();

        return mockBackend.expect(between(0, 1), requestTo(uri))
                .andExpect(method(GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc"));
    }

    public String normalizeCards(String body) {
        Configuration configuration = Configuration.builder()
                .options(Option.SUPPRESS_EXCEPTIONS)
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .build();

        DocumentContext context = JsonPath.using(configuration).parse(body);

        context.set("$.objects[*].id", "00000000-0000-0000-0000-000000000000");
        context.set("$.objects[*].creation_date", "1970-01-01T00:00:00Z");
        context.set("$.objects[*].expiration_date", "1970-01-01T00:00:00Z");
        context.set("$.objects[*].actions[*].id", "00000000-0000-0000-0000-000000000000");
        return context.jsonString();
    }
}
