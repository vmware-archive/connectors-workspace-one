/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msTeams

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.vmware.connectors.msTeams.config.Endpoints
import com.vmware.connectors.msTeams.config.ROUTING_PREFIX
import com.vmware.connectors.msTeams.utils.*
import com.vmware.connectors.test.ControllerTestsBase
import org.junit.Assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MimeTypeUtils
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import uk.co.datumedge.hamcrest.json.SameJSONAs
import java.util.stream.Collectors

const val ROUTING_PREFIX_URL = "https://hero/connectors/ms_teams/"
const val messageId = "1570792618427"

class CardsControllerTest : ControllerTestsBase() {

    private fun getAuthorizationToken(uri: String) = "Bearer " + accessToken(uri)
    private val authorization = "Bearer abc"

    @ParameterizedTest
    @ValueSource(strings = [
        "/messages/$messageId/reply",
        "/messages/$messageId/dismiss",
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
    @Throws(Exception::class)
    fun testGetImage(
            @Value("classpath:static/images/connector.png")
            icon: Resource
    ) {
        webClient.get()
                .uri("/images/connector.png")
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType(MimeTypeUtils.IMAGE_PNG_VALUE)
                .expectBody<ByteArray>().isEqualTo(icon.readAsByteArray())
    }

    @Test
    fun `Test Auth Fail For replyToMessage`(
            @Value("classpath:service/api/messages/message1.json")
            message: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers
                .requestTo(
                        Endpoints.getReplyToMessageUrl("28146088-6522-4459-9a52-295772efd51c",
                                "19:edc1a6c1be5c4736b7ead64dae9624e3@thread.skype",
                                "1570792618427", "")
                )
        )
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest().body("Bad OAuth Token"))

        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
        formData.add("actionType", "replyToMessage")
        formData.add("message", message.readAsString())

        val uri = "/messages/$messageId/reply"

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
    fun `Test Success For replyToMessage`(
            @Value("classpath:service/api/messages/message1.json")
            message: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getReplyToMessageUrl("28146088-6522-4459-9a52-295772efd51c", "19:edc1a6c1be5c4736b7ead64dae9624e3@thread.skype", "1570792618427", "")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess())

        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
        formData.add("actionType", "replyToMessage")
        formData.add("message", message.readAsString())

        val uri = "/messages/$messageId/reply"

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
    fun `Test Success For replyToMessage With NullInput`(
            @Value("classpath:service/api/messages/message1.json")
            message: Resource
    ) {
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", null)
        formData.add("actionType", "replyToMessage")
        formData.add("message", message.readAsString())

        val uri = "/messages/$messageId/reply"

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
    fun `Test Success For Dismiss`(
            @Value("classpath:service/api/messages/message1.json")
            message: Resource
    ) {
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
        formData.add("actionType", "replyToMessage")
        formData.add("message", message.readAsString())

        val uri = "/messages/$messageId/dismiss"

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
    fun `Test Auth Fail For Cards Requests`(
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource
    ) {
        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getTeamsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())

        val jsonResponse = response.readAsString()

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
                .expectBody().json(jsonResponse)

    }

    @Test
    fun `Test Success For Cards Request`(
            @Value("classpath:connector/responses/u1.json")
            response: Resource,
            @Value("classpath:service/api/teams/teams1.json")
            team: Resource,
            @Value("classpath:service/api/batchResponse/teamsBatchResponse1.json")
            teamsBatchResponse: Resource,
            @Value("classpath:service/api/batchResponse/channelBatchResponse1.json")
            channelBatchResponse: Resource,
            @Value("classpath:service/api/batchResponse/replyBatchResponse1.json")
            replyBatchResponse: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getTeamsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(team.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getBatchUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(teamsBatchResponse.readAsString(), MediaType.APPLICATION_JSON))

        (0..4).map {
            mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getBatchUrl("")))
                    .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                    .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                    .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                    .andRespond(MockRestResponseCreators.withSuccess(channelBatchResponse.readAsString(), MediaType.APPLICATION_JSON))
        }
        (0..1).map {
            mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getBatchUrl("")))
                    .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                    .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                    .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                    .andRespond(MockRestResponseCreators.withSuccess(replyBatchResponse.readAsString(), MediaType.APPLICATION_JSON))
        }
        val jsonResponse = response.readAsString()

        val uri = "/cards/requests"

        val data = webClient.post()
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
        Assert.assertThat<String>(data, SameJSONAs.sameJSONAs(jsonResponse))
    }

    @Test
    fun `Test Success For Empty Cards`(
            @Value("classpath:connector/responses/u5.json")
            response: Resource,
            @Value("classpath:service/api/teams/teams1.json")
            team: Resource,
            @Value("classpath:service/api/batchResponse/teamsBatchResponse1.json")
            teamsBatchResponse: Resource,
            @Value("classpath:service/api/batchResponse/channelBatchResponse1.json")
            channelBatchResponse: Resource,
            @Value("classpath:service/api/batchResponse/channelBatchResponse2.json")
            channelBatchResponse1: Resource,
            @Value("classpath:service/api/batchResponse/replyBatchResponse2.json")
            replyBatchResponse: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getTeamsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(team.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getBatchUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(teamsBatchResponse.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getBatchUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(channelBatchResponse1.readAsString(), MediaType.APPLICATION_JSON))

        (0..4).map {
            mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getBatchUrl("")))
                    .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                    .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                    .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                    .andRespond(MockRestResponseCreators.withSuccess(channelBatchResponse.readAsString(), MediaType.APPLICATION_JSON))
        }
        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getBatchUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(replyBatchResponse.readAsString(), MediaType.APPLICATION_JSON))

        val jsonResponse = response.readAsString()

        val uri = "/cards/requests"

        val data = webClient.post()
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

        Assert.assertThat<String>(data, SameJSONAs.sameJSONAs(jsonResponse))
    }

    @Test
    fun `Test Auth Fail While Batch Request`(
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource,
            @Value("classpath:service/api/teams/teams1.json")
            team: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getTeamsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(team.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getBatchUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())

        val jsonResponse = response.readAsString()

        val uri = "/cards/requests"

        webClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().json(jsonResponse)
    }

    @Test
    fun miscellaneous() {
        val map = mapOf("string" to null)
        val resp = map.getListOrDefault("s", listOf(1, 2))
        Assert.assertEquals(listOf(1, 2), resp)
        val mapResp = map.getMapOrDefault("s", mapOf("sample" to 1))
        Assert.assertEquals(mapOf("sample" to 1), mapResp)
        val emptyMapResp = map.getMapOrDefault<String, Any>("s")
        Assert.assertEquals(emptyMap<String, Any>(), emptyMapResp)
        Assert.assertEquals(null, map.getMapOrNull<String, Any>("j"))
        try {
            map.getListOrException<String>("f")
        } catch (e: java.lang.Exception) {
            Assert.assertEquals(null, e.localizedMessage)
        }
        try {
            map.getMapOrException<String, Any>("f")
        } catch (e: java.lang.Exception) {
            Assert.assertEquals(null, e.localizedMessage)
        }
        val userNameFromToken = VmwareUtils.getUserEmailFromToken("token s.bnVsbA==")
        Assert.assertEquals(null, userNameFromToken)
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
