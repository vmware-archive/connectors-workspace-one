/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

class HubHubConcurControllerTest extends ControllerTestsBase {

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
                .expectBody().consumeWith(body -> assertThat(body.getResponseBody(),
                    equalTo(bytesFromFile("/static/images/connector.png"))));
    }

    @ParameterizedTest
    @CsvSource({
            ", success.json",
            "xx, success_xx.json"
    })
    void testCardsRequests(String lang, String expected) throws Exception {

        mockConcurRequests();

        WebTestClient.RequestHeadersSpec<?> spec = webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header("x-routing-prefix", "https://hero/connectors/concur/")
                .headers(ControllerTestsBase::headers)
                .syncBody(fromFile("/connector/requests/request.json"));

        if (StringUtils.isNotBlank(lang)) {
            spec.header(ACCEPT_LANGUAGE, lang);
        }

        String body = spec.exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult(String.class)
                .getResponseBody()
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block();

        assertThat(body, sameJSONAs(fromFile("connector/responses/" + expected))
                .allowingAnyArrayOrdering()
                .allowingExtraUnexpectedFields());
    }

    private void mockConcurRequests() throws Exception {
        mockBackend.expect(requestTo("/fake-api/requests?user_id=fred@acme"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(fromFile("/fake/all-requests.json"), APPLICATION_JSON));

        mockBackend.expect(requestTo("/fake-api/requests/1"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(fromFile("/fake/request-1.json"), APPLICATION_JSON));

        mockBackend.expect(requestTo("/fake-api/requests/4"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(fromFile("/fake/request-4.json"), APPLICATION_JSON));
    }

    @Test
    void testApproveRequest() {
        webClient.post()
                .uri("/api/expense/{id}/approve", "123")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData("comment", "Approval Done"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testRejectRequest() {
        webClient.post()
                .uri("/api/expense/{id}/decline", "123")
                .header(AUTHORIZATION, "Bearer " + accessToken())
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData("reason", "Decline Done"))
                .exchange()
                .expectStatus().isOk();
    }

}
