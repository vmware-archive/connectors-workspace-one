/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.concur

import com.backflipt.commons.readAsString
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.vmware.connectors.concur.config.Endpoints
import com.vmware.connectors.concur.config.ROUTING_PREFIX
import com.vmware.connectors.concur.dto.TravelRequestStatus
import com.vmware.connectors.concur.dto.WorkflowAction
import com.vmware.connectors.test.ControllerTestsBase
import com.vmware.connectors.test.JsonNormalizer
import com.vmware.connectors.utils.IgnoredFieldsReplacer
import org.junit.Assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.*
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import uk.co.datumedge.hamcrest.json.SameJSONAs
import java.text.MessageFormat
import java.time.Duration
import java.util.stream.Collectors

const val ROUTING_PREFIX_URL = "https://hero/connectors/sap_concur/"

class CardsControllerTest : ControllerTestsBase() {
    private fun getAuthorizationToken(uri: String) = "Bearer " + accessToken(uri)
    private val authorization = "Bearer abc"

    @ParameterizedTest
    @ValueSource(strings = [
        "/cards/requests",
        "/travelrequest/workflowaction"
    ])
    @Throws(Exception::class)
    fun testProtectedResource(uri: String) {
        testProtectedResource(HttpMethod.POST, uri)
    }

    @Test
    fun missingAuthorization() {
        webClient.post()
                .uri("/cards/requests")
                .exchange()
                .expectStatus().isUnauthorized
    }

    @Test
    fun authFailForWorkFlowActions(
            @Value("classpath:static/templates/concur-request-template.xml")
            concurRequestTemplateResource: Resource,
            @Value("classpath:service/api/travelRequestDetails/travelRequestDetails1.json")
            travelRequestDetails: Resource,
            @Value("classpath:service/api/userProfile/userProfile.json")
            userProfile: Resource
    ) {
        val concurRequestTemplate = concurRequestTemplateResource.readAsString()
        val body = MessageFormat.format(
                concurRequestTemplate,
                WorkflowAction.APPROVE.getActionString(),
                "sample"
        )
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserLoginIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(userProfile.readAsString(), APPLICATION_JSON))
        val modifiedTravelRequestDetails = travelRequestDetails.readAsString().replace("\${concur_host}", mockBackend.url(""))
        mockBackend.expect(MockRestRequestMatchers.requestTo("/api/travelrequest/v1.0/requests/gWigWzz09wveXwkfgEW8G0ckU1lxNpL2aaT8/WorkFlowAction"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(body))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "sample")
        formData.add("actionType", WorkflowAction.APPROVE.name)
        formData.add("travelRequest", modifiedTravelRequestDetails)
        val uri = "/travelrequest/workflowaction"
        webClient.post()
                .uri(uri)
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
    }

    @Test
    fun testSuccessForApprovingRequest(
            @Value("classpath:static/templates/concur-request-template.xml")
            concurRequestTemplateResource: Resource,
            @Value("classpath:service/api/travelRequestDetails/travelRequestDetails1.json")
            travelRequestDetails: Resource,
            @Value("classpath:service/api/userProfile/userProfile.json")
            userProfile: Resource
    ) {
        val concurRequestTemplate = concurRequestTemplateResource.readAsString()
        val body = MessageFormat.format(
                concurRequestTemplate,
                WorkflowAction.APPROVE.getActionString(),
                "sample"
        )
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserLoginIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(userProfile.readAsString(), APPLICATION_JSON))
        val modifiedTravelRequestDetails = travelRequestDetails.readAsString().replace("\${concur_host}", mockBackend.url(""))
        mockBackend.expect(MockRestRequestMatchers.requestTo("/api/travelrequest/v1.0/requests/gWigWzz09wveXwkfgEW8G0ckU1lxNpL2aaT8/WorkFlowAction"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(body))
                .andRespond(MockRestResponseCreators.withSuccess())
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "sample")
        formData.add("actionType", WorkflowAction.APPROVE.name)
        formData.add("travelRequest", modifiedTravelRequestDetails)
        val uri = "/travelrequest/workflowaction"
        webClient.post()
                .uri(uri)
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isOk
    }

    @Test
    fun testSuccessForSendBackRequest(
            @Value("classpath:static/templates/concur-request-template.xml")
            concurRequestTemplateResource: Resource,
            @Value("classpath:service/api/travelRequestDetails/travelRequestDetails1.json")
            travelRequestDetails: Resource,
            @Value("classpath:service/api/userProfile/userProfile.json")
            userProfile: Resource
    ) {
        val concurRequestTemplate = concurRequestTemplateResource.readAsString()
        val body = MessageFormat.format(
                concurRequestTemplate,
                WorkflowAction.SEND_BACK.getActionString(),
                "sample"
        )
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserLoginIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(userProfile.readAsString(), APPLICATION_JSON))
        val modifiedTravelRequestDetails = travelRequestDetails.readAsString().replace("\${concur_host}", mockBackend.url(""))
        mockBackend.expect(MockRestRequestMatchers.requestTo("/api/travelrequest/v1.0/requests/gWigWzz09wveXwkfgEW8G0ckU1lxNpL2aaT8/WorkFlowAction"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(body))
                .andRespond(MockRestResponseCreators.withSuccess())
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "sample")
        formData.add("actionType", WorkflowAction.SEND_BACK.name)
        formData.add("travelRequest", modifiedTravelRequestDetails)
        val uri = "/travelrequest/workflowaction"
        webClient.post()
                .uri(uri)
                .contentType(APPLICATION_FORM_URLENCODED)
                .header(AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isOk
    }

    @Test
    fun testSuccessForCardsRequests(
            @Value("classpath:service/api/userProfile/userProfile.json")
            userLoginId: Resource,
            @Value("classpath:service/api/travelRequests/travelRequests1.json")
            travelRequest: Resource,
            @Value("classpath:service/api/travelRequestDetails/travelRequestDetails1.json")
            travelRequestsDetails: Resource,
            @Value("classpath:connector/responses/cardResponse.json")
            response: Resource
    ) {
        val loginId = "request.approver@concur1.onmicrosoft.com"
        val requestDetailsUrl = "/api/travelrequest/v1.0/requests/gWjcxBTgB6AIr1sadnbPdD4qL84UOooxirg"
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserLoginIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(userLoginId.readAsString(), APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getTravelRequestsUrl("", TravelRequestStatus.TOAPPROVE.name, loginId)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(travelRequest.readAsString().replace("\${concur_host}", mockBackend.url("")), APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(requestDetailsUrl))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(travelRequestsDetails.readAsString(), APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(requestDetailsUrl))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())
        val expectedResponse = response.readAsString()
        val uri = "/cards/requests"
        val data = webClient
                .mutate()
                .responseTimeout(Duration.ofMillis(30000))
                .build()
                .post()
                .uri(uri)
                .header(AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult<String>()
                .responseBody
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block()
                ?.replace(Regex("http://localhost:\\d+/"), "/")
        val configuration = Configuration.builder()
                .jsonProvider(JacksonJsonNodeJsonProvider())
                .build()
        val context: DocumentContext = JsonPath.using(configuration).parse(data)
        val actualData = context
                .set("$.objects[?(@.hash =~ /" + IgnoredFieldsReplacer.UUID_PATTERN + "/)].hash", IgnoredFieldsReplacer.DUMMY_UUID)
                .jsonString()
        Assert.assertThat(actualData, SameJSONAs.sameJSONAs(expectedResponse))
    }

    @Test
    fun testSuccessForEmptyCardsRequests(
            @Value("classpath:service/api/userProfile/userProfile.json")
            userLoginId: Resource,
            @Value("classpath:service/api/travelRequests/travelRequests2.json")
            travelRequest: Resource,
            @Value("classpath:connector/responses/emptyResponse.json")
            response: Resource

    ) {
        val loginId = "request.approver@concur1.onmicrosoft.com"
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserLoginIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(userLoginId.readAsString(), APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getTravelRequestsUrl("", TravelRequestStatus.TOAPPROVE.name, loginId)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(travelRequest.readAsString(), APPLICATION_JSON))
        val expectedResponse = response.readAsString()
        val uri = "/cards/requests"
        val data = webClient
                .mutate()
                .responseTimeout(Duration.ofMillis(30000))
                .build()
                .post()
                .uri(uri)
                .header(AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .returnResult<String>()
                .responseBody
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block()
                ?.replace(Regex("http://localhost:\\d+/"), "/")
        Assert.assertThat<String>(data, SameJSONAs.sameJSONAs(expectedResponse))
    }

    @Test
    fun testAuthFailForCardsRequest(
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserLoginIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withStatus(FORBIDDEN).body("token is expired").contentType(TEXT_PLAIN))
        val expectedResponse = response.readAsString()
        val cardsUrl = "/cards/requests"
        webClient.post()
                .uri(cardsUrl)
                .header(AUTHORIZATION, getAuthorizationToken(cardsUrl))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(expectedResponse)
    }
}
