/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

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
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonNormalizer;


class HubConcurControllerTest extends ControllerTestsBase {

	@ParameterizedTest
	@ValueSource(strings = { "/cards/requests", "/api/expense/123/approve", "/api/expense/123/decline" })
	void testProtectedResources(String uri) throws Exception {
		testProtectedResource(POST, uri);
	}

	@Test
	void testDiscovery() throws Exception {
		testConnectorDiscovery();
	}

	@Test
	void testGetImage() {
		webClient.get().uri("/images/connector.png").exchange().expectStatus().isOk().expectHeader().contentLength(9339)
				.expectHeader().contentType(IMAGE_PNG_VALUE).expectBody()
				.consumeWith(body -> assertThat(body.getResponseBody(),
						equalTo(bytesFromFile("/static/images/connector.png"))));
	}

	@ParameterizedTest
	@CsvSource({ StringUtils.EMPTY + ", success.json", "xx, success_xx.json" })
	//TODO : Now that we are not basing the currency on locale,both the above test cases looks redundant - Remove one?
	void testCardsRequests(String lang, String expected) throws Exception {

		mockConcurRequests();

		WebTestClient.RequestHeadersSpec<?> spec = webClient.post().uri("/cards/requests")
				.header(AUTHORIZATION, "Bearer " + accessToken()).header(X_AUTH_HEADER, "Bearer vidm-token")
				.header(X_BASE_URL_HEADER, mockBackend.url(""))
				.header("x-routing-prefix", "https://hero/connectors/concur/").headers(ControllerTestsBase::headers)
				.contentType(APPLICATION_JSON).accept(APPLICATION_JSON)
				.syncBody(fromFile("/connector/requests/request.json"));

		if (StringUtils.isNotBlank(lang)) {
			spec.header(ACCEPT_LANGUAGE, lang);
		}

		String body = spec.exchange().expectStatus().isOk().expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
				.returnResult(String.class).getResponseBody().collect(Collectors.joining())
				.map(JsonNormalizer::forCards).block()
				.replaceAll("[0-9]{4}[-][0-9]{2}[-][0-9]{2}T[0-9]{2}[:][0-9]{2}[:][0-9]{2}Z?", "1970-01-01T00:00:00Z")
				.replaceAll("[a-z0-9]{40,}", "test-hash");

		assertThat(body, sameJSONAs(fromFile("connector/responses/" + expected)).allowingAnyArrayOrdering()
				.allowingExtraUnexpectedFields());
	}

	private void mockConcurRequests() throws Exception {
		mockReportsDigest();

		mockReport1();

		mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/683105624FD74A1B9C13"))
				.andExpect(method(GET)).andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
				.andRespond(withSuccess(fromFile("/fake/report-2.json").replace("${concur_host}", mockBackend.url("")),
						APPLICATION_JSON));

		mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/A77D016732974B5F8E23"))
				.andExpect(method(GET)).andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
				.andRespond(withSuccess(fromFile("/fake/report-3.json").replace("${concur_host}", mockBackend.url("")),
						APPLICATION_JSON));
	}

	private void mockReportsDigest() throws Exception {
		mockBackend
				.expect(requestTo("/api/v3.0/expense/reportdigests?approverLoginID=admin@acme.com&limit=50&user=all"))
				.andExpect(method(GET)).andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
				.andRespond(withSuccess(
						fromFile("/fake/report-digests.json").replace("${concur_host}", mockBackend.url("")),
						APPLICATION_JSON));
	}

	private void mockReport1() throws Exception {

		mockBackend.expect(requestTo("/api/expense/expensereport/v2.0/report/1D3BD2E14D144508B05F"))
				.andExpect(method(GET)).andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
				.andRespond(withSuccess(fromFile("/fake/report-1.json").replace("${concur_host}", mockBackend.url("")),
						APPLICATION_JSON));
	}

	private void mockReport1Action() {
		mockBackend.expect(requestTo(
				"/api/expense/expensereport/v1.1/report/gWqmsMJ27KYsYDsraMCRfUtd5Y9ha96y0lRUG0nBXhO0/WorkFlowAction"))
				.andExpect(method(POST)).andExpect(content().contentType(APPLICATION_XML)).andRespond(withSuccess());
	}

	@Test
	void testApproveRequest() throws Exception {
		mockReportsDigest();
		mockReport1();
		mockReport1Action();

		webClient.post().uri("/api/expense/{id}/approve", "1D3BD2E14D144508B05F")
				.header(AUTHORIZATION, "Bearer " + accessToken()).header(X_AUTH_HEADER, "Bearer vidm-token")
				.header(X_BASE_URL_HEADER, mockBackend.url("")).contentType(APPLICATION_FORM_URLENCODED)
				.body(BodyInserters.fromFormData("comment", "Approval Done")).exchange().expectStatus().isOk();
	}

	@Test
	void testRejectRequest() throws Exception {
		mockReportsDigest();
		mockReport1();
		mockReport1Action();

		webClient.post().uri("/api/expense/{id}/decline", "1D3BD2E14D144508B05F")
				.header(AUTHORIZATION, "Bearer " + accessToken()).header(X_AUTH_HEADER, "Bearer vidm-token")
				.header(X_BASE_URL_HEADER, mockBackend.url("")).contentType(APPLICATION_FORM_URLENCODED)
				.body(BodyInserters.fromFormData("reason", "Decline Done")).exchange().expectStatus().isOk();
	}

	@Test
	void testUnAuthorizedApproveRequest() throws Exception {
		mockReportsDigest();

		webClient.post().uri("/api/expense/{id}/approve", "1D3BD2E14D144508B0")
				.header(AUTHORIZATION, "Bearer " + accessToken()).header(X_AUTH_HEADER, "Bearer vidm-token")
				.header(X_BASE_URL_HEADER, mockBackend.url("")).contentType(APPLICATION_FORM_URLENCODED)
				.body(BodyInserters.fromFormData("comment", "Approval Done")).exchange().expectStatus()
				.isNotFound();
	}

}
