/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.sfLeads

import com.backflipt.commons.getDateTimeMinusHours
import com.backflipt.commons.readAsString
import com.backflipt.commons.serialize
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.vmware.connectors.sfLeads.config.EndpointsAndQueries
import com.vmware.connectors.sfLeads.config.LEADS_DATE_FORMATTER
import com.vmware.connectors.sfLeads.config.LEADS_LOOK_SINCE_HOURS
import com.vmware.connectors.sfLeads.config.ROUTING_PREFIX
import com.vmware.connectors.test.ControllerTestsBase
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import uk.co.datumedge.hamcrest.json.SameJSONAs
import java.time.Duration
import java.util.stream.Collectors

const val ROUTING_PREFIX_URL = "https://hero/connectors/sf_leads/"
const val leadId = "0052v00000ftWUbAAM"
const val leadUrl = "https://bfvmw.my.salesforce.com/00Q2v00001cZUg5EAG"
const val DUMMY_HASH = "f7682c00994841942b5c04217585f5578b1ed21a74006d4ba5fd71e63cc2a3a3"

class CardsControllerTest : ControllerTestsBase() {
    private fun getAuthorizationToken(uri: String) = "Bearer " + accessToken(uri)
    private val authorization = "Bearer abc"

    @ParameterizedTest
    @ValueSource(strings = [
        "/leads/$leadId/addTask",
        "/leads/$leadId/logACall",
        "/cards/requests"
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
    fun testSuccessForCardsRequest(
            @Value("classpath:Leads/Leads.json")
            leadResponse: Resource,
            @Value("classpath:users/user1.json")
            user: Resource,
            @Value("classpath:responses/response.json")
            response: Resource
    ) {
        val ownerId = "0052v00000ftWUbAAM"
        val date = getDateTimeMinusHours(LEADS_LOOK_SINCE_HOURS, LEADS_DATE_FORMATTER)
        val query = EndpointsAndQueries.getRecentLeadsQuery(date, ownerId)
        mockBackend.expect(MockRestRequestMatchers.requestTo(EndpointsAndQueries.getUserDetailsQuery("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Matchers.startsWith(
                EndpointsAndQueries.getQueryRecordsUrl("", query)
                        .replaceAfter("?q", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(leadResponse.readAsString(), MediaType.APPLICATION_JSON))
        val uri = "/cards/requests"
        val expectedResponse = response.readAsString()
        val data = webClient
                .mutate()
                .responseTimeout(Duration.ofMillis(10000L))
                .build()
                .post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .returnResult<String>()
                .responseBody
                .collect(Collectors.joining())
                .map(this::normalizeCards)
                .block()
                ?.replace(Regex("http://localhost:\\d+/"), "/")
        val configuration = Configuration.builder()
                .jsonProvider(JacksonJsonNodeJsonProvider())
                .build()
        val context: DocumentContext = JsonPath.using(configuration).parse(data)
        val actualData = context.set("$.objects[0].hash", DUMMY_HASH)
                .set("$.objects[0].header.links.subtitle[0]", leadUrl)
                .set("$.objects[0].body.fields[6].description", leadUrl)
                .jsonString()
        Assert.assertThat(actualData, SameJSONAs.sameJSONAs(expectedResponse))
    }

    @Test
    fun testSuccessForCardsRequestWithNullInputForPhoneNumber(
            @Value("classpath:Leads/Leads1.json")
            leadResponse: Resource,
            @Value("classpath:users/user1.json")
            user: Resource,
            @Value("classpath:responses/responseWithOutMail.json")
            response: Resource
    ) {
        val ownerId = "0052v00000ftWUbAAM"
        val date = getDateTimeMinusHours(LEADS_LOOK_SINCE_HOURS, LEADS_DATE_FORMATTER)
        val query = EndpointsAndQueries.getRecentLeadsQuery(date, ownerId)
        mockBackend.expect(MockRestRequestMatchers.requestTo(EndpointsAndQueries.getUserDetailsQuery("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Matchers.startsWith(
                EndpointsAndQueries.getQueryRecordsUrl("", query)
                        .replaceAfter("?q", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(leadResponse.readAsString(), MediaType.APPLICATION_JSON))
        val uri = "/cards/requests"
        val expectedResponse = response.readAsString()
        val data = webClient
                .mutate()
                .responseTimeout(Duration.ofMillis(10000L))
                .build()
                .post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .returnResult<String>()
                .responseBody
                .collect(Collectors.joining())
                .map(this::normalizeCards)
                .block()
                ?.replace(Regex("http://localhost:\\d+/"), "/")
        val configuration = Configuration.builder()
                .jsonProvider(JacksonJsonNodeJsonProvider())
                .build()
        val modifiedData = data?.replace("\${lead_host}", mockBackend.url(""))
        val context: DocumentContext = JsonPath.using(configuration).parse(modifiedData)
        val actualData = context.set("$.objects[0].hash", DUMMY_HASH)
                .set("$.objects[0].header.links.subtitle[0]", leadUrl)
                .set("$.objects[0].body.fields[4].description", leadUrl)
                .jsonString()
        Assert.assertThat(actualData, SameJSONAs.sameJSONAs(expectedResponse))
    }

    @Test
    fun testSuccessForAddTaskAction(
            @Value("classpath:Leads/SingleLead.json")
            leadResponse: Resource
    ) {
        val addTaskBody = mapOf(
                "WhoId" to leadId,
                "Subject" to "test subject",
                "Status" to "Not Started",
                "Priority" to "Normal"
        ).serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(EndpointsAndQueries.createTaskForLeadUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(addTaskBody))
                .andRespond(MockRestResponseCreators.withSuccess())
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
        formData.add("lead", leadResponse.readAsString())
        val uri = "/leads/$leadId/addTask"
        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isOk
    }

    @Test
    fun testSuccessForLoggingCall(
            @Value("classpath:Leads/SingleLead.json")
            leadResponse: Resource
    ) {
        val loggingCallBody = mapOf("record" to
                mapOf(
                        "WhoId" to "0052v00000ftWUbAAM",
                        "Subject" to "test subject"
                )
        ).serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(EndpointsAndQueries.logACallUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(loggingCallBody))
                .andRespond(MockRestResponseCreators.withSuccess())
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
        formData.add("lead", leadResponse.readAsString())
        val uri = "/leads/$leadId/logACall"
        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isOk
    }

    @Test
    fun testAuthFailForLoggingCall(
            @Value("classpath:Leads/SingleLead.json")
            leadResponse: Resource
    ) {
        val loggingCallBody = mapOf("record" to
                mapOf(
                        "WhoId" to "0052v00000ftWUbAAM",
                        "Subject" to "test subject"
                )
        ).serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(EndpointsAndQueries.logACallUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(loggingCallBody))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN).body("bad_oauth_token"))
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
        formData.add("actionType", "logACall")
        formData.add("lead", leadResponse.readAsString())
        val uri = "/leads/$leadId/logACall"
        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
    }

    @Test
    fun testAuthFailForAddingTask(
            @Value("classpath:Leads/SingleLead.json")
            leadResponse: Resource
    ) {
        val addTaskBody = mapOf(
                "WhoId" to leadId,
                "Subject" to "test subject",
                "Status" to "Not Started",
                "Priority" to "Normal"
        ).serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(EndpointsAndQueries.createTaskForLeadUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(addTaskBody))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.FORBIDDEN).body("missing_oauth_token"))
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
        formData.add("lead", leadResponse.readAsString())
        val uri = "/leads/$leadId/addTask"
        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
    }

    @Test
    fun testAuthFailForCardsRequest(
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource
    ) {
        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(EndpointsAndQueries.getUserDetailsQuery("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())
        val expectedResponse = response.readAsString()
        val cardsUrl = "/cards/requests"
        webClient.post()
                .uri(cardsUrl)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(cardsUrl))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().json(expectedResponse)
    }

    @Test
    fun testAuthFailForCardsRequestWhileFetchingQueryRecords(
            @Value("classpath:users/user1.json")
            user: Resource,
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource
    ) {
        val ownerId = "0052v00000ftWUbAAM"
        val date = getDateTimeMinusHours(LEADS_LOOK_SINCE_HOURS, LEADS_DATE_FORMATTER)
        val query = EndpointsAndQueries.getRecentLeadsQuery(date, ownerId)
        mockBackend.expect(MockRestRequestMatchers.requestTo(EndpointsAndQueries.getUserDetailsQuery("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Matchers.startsWith(
                EndpointsAndQueries.getQueryRecordsUrl("", query)
                        .replaceAfter("?q", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())
        val expectedResponse = response.readAsString()
        val cardsUrl = "/cards/requests"
        webClient.post()
                .uri(cardsUrl)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(cardsUrl))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().json(expectedResponse)
    }

    @Test
    fun testSuccessForCardsRequestForEmptyFieldValue(
            @Value("classpath:Leads/LeadsWithEmptyName.json")
            leadResponse: Resource,
            @Value("classpath:users/user1.json")
            user: Resource,
            @Value("classpath:responses/ResponseWithEmptyField.json")
            response: Resource
    ) {
        val ownerId = "0052v00000ftWUbAAM"
        val date = getDateTimeMinusHours(LEADS_LOOK_SINCE_HOURS, LEADS_DATE_FORMATTER)
        val query = EndpointsAndQueries.getRecentLeadsQuery(date, ownerId)
        mockBackend.expect(MockRestRequestMatchers.requestTo(EndpointsAndQueries.getUserDetailsQuery("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Matchers.startsWith(
                EndpointsAndQueries.getQueryRecordsUrl("", query)
                        .replaceAfter("?q", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(leadResponse.readAsString(), MediaType.APPLICATION_JSON))
        val uri = "/cards/requests"
        val expectedResponse = response.readAsString()
        val data = webClient
                .mutate()
                .responseTimeout(Duration.ofMillis(10000L))
                .build()
                .post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .returnResult<String>()
                .responseBody
                .collect(Collectors.joining())
                .map(this::normalizeCards)
                .block()
                ?.replace(Regex("http://localhost:\\d+/"), "/")
        val configuration = Configuration.builder()
                .jsonProvider(JacksonJsonNodeJsonProvider())
                .build()
        val context: DocumentContext = JsonPath.using(configuration).parse(data)
        val actualData = context.set("$.objects[0].hash", DUMMY_HASH)
                .set("$.objects[0].header.links.subtitle[0]", leadUrl)
                .set("$.objects[0].body.fields[5].description", leadUrl)
                .jsonString()
        Assert.assertThat(actualData, SameJSONAs.sameJSONAs(expectedResponse))
    }

    @Test
    fun testSuccessForEmptyCardsRequest(
            @Value("classpath:Leads/LeadForEmptyCards.json")
            leadResponse: Resource,
            @Value("classpath:users/user1.json")
            user: Resource,
            @Value("classpath:responses/emptyCardsResponse.json")
            response: Resource
    ) {
        val ownerId = "0052v00000ftWUbAAM"
        val date = getDateTimeMinusHours(LEADS_LOOK_SINCE_HOURS, LEADS_DATE_FORMATTER)
        val query = EndpointsAndQueries.getRecentLeadsQuery(date, ownerId)
        mockBackend.expect(MockRestRequestMatchers.requestTo(EndpointsAndQueries.getUserDetailsQuery("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Matchers.startsWith(
                EndpointsAndQueries.getQueryRecordsUrl("", query)
                        .replaceAfter("?q", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(leadResponse.readAsString(), MediaType.APPLICATION_JSON))
        val uri = "/cards/requests"
        val expectedResponse = response.readAsString()
        val data = webClient
                .mutate()
                .responseTimeout(Duration.ofMillis(100000))
                .build()
                .post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .returnResult<String>()
                .responseBody
                .collect(Collectors.joining())
                .map(this::normalizeCards)
                .block()
                ?.replace(Regex("http://localhost:\\d+/"), "/")
        Assert.assertThat(data, SameJSONAs.sameJSONAs(expectedResponse))
    }

    fun normalizeCards(body: String?): String? {
        val configuration = Configuration.builder()
                .options(Option.SUPPRESS_EXCEPTIONS)
                .jsonProvider(JacksonJsonNodeJsonProvider())
                .build()
        val context = JsonPath.using(configuration).parse(body)
        context.set("$.objects[*].id", "00000000-0000-0000-0000-000000000000")
        context.set("$.objects[*].creation_date", "1970-01-01T00:00:00Z")
        context.set("$.objects[*].expiration_date", "1970-01-01T00:00:00Z")
        context.set("$.objects[*].actions[*].id", "00000000-0000-0000-0000-000000000000")
        return context.jsonString()
    }
}
