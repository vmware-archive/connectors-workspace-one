package com.vmware.connectors.msTeams

import com.vmware.connectors.msTeams.config.Endpoints
import com.vmware.connectors.msTeams.controller.MESSAGE_ROUTING_PREFIX
import com.vmware.connectors.msTeams.utils.readAsByteArray
import com.vmware.connectors.msTeams.utils.readAsString
import com.vmware.connectors.test.ControllerTestsBase
import com.vmware.connectors.test.JsonNormalizer
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
    fun `Test Auth Fail For replyToMessage`(
            @Value("classpath:service/api/messages/message1.json")
            message: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getReplyToMessageUrl("28146088-6522-4459-9a52-295772efd51c", "19:edc1a6c1be5c4736b7ead64dae9624e3@thread.skype", "1570792618427", "")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())

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
                .expectStatus().isCreated

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
                .expectStatus().isCreated
    }

    @Test
    @Throws(Exception::class)
    fun testDiscovery() {
        testConnectorDiscovery()
    }

    @Test
    @Throws(Exception::class)
    fun testGetImage(@Value("classpath:static/images/connector.png")
                     icon: Resource) {
        webClient.get()
                .uri("/images/connector.png")
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType(MimeTypeUtils.IMAGE_PNG_VALUE)
                .expectBody<ByteArray>().isEqualTo(icon.readAsByteArray())
    }

    @Test
    fun missingAuthorization() {
        webClient.post()
                .uri("/cards/requests")
                .exchange()
                .expectStatus().isUnauthorized
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
                .header(MESSAGE_ROUTING_PREFIX, ROUTING_PREFIX_URL)
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
            response1: Resource,
            @Value("classpath:connector/responses/u2.json")
            response2: Resource,
            @Value("classpath:connector/responses/u3.json")
            response3: Resource,
            @Value("classpath:connector/responses/u4.json")
            response4: Resource,
            @Value("classpath:service/api/users/u1.json")
            user: Resource,
            @Value("classpath:service/api/messages/message2.json")
            message: Resource,
            @Value("classpath:service/api/teams/teams1.json")
            team: Resource,
            @Value("classpath:service/api/batchResponse/teamsBatchResponse1.json")
            teamsBatchResponse1: Resource,
            @Value("classpath:service/api/batchResponse/teamsBatchResponse2.json")
            teamsBatchResponse2: Resource,
            @Value("classpath:service/api/batchResponse/teamsBatchResponse3.json")
            teamsBatchResponse3: Resource,
            @Value("classpath:service/api/batchResponse/teamsBatchResponse4.json")
            teamsBatchResponse4: Resource,
            @Value("classpath:service/api/batchResponse/channelBatchResponse1.json")
            channelBatchResponse: Resource,
            @Value("classpath:service/api/batchResponse/replyBatchResponse1.json")
            replyBatchResponse: Resource
    ) {
        val teamBatchResponses = listOf(teamsBatchResponse1, teamsBatchResponse2, teamsBatchResponse3, teamsBatchResponse4)
        teamBatchResponses.map { teamsBatchResponse ->
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
        }
        val responses = listOf(response1, response2, response3, response4)
        responses.map { response ->
            val jsonResponse = response.readAsString()

            val uri = "/cards/requests"

            val data = webClient.post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                    .header(X_AUTH_HEADER, authorization)
                    .header(X_BASE_URL_HEADER, mockBackend.url(""))
                    .header(MESSAGE_ROUTING_PREFIX, ROUTING_PREFIX_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                    .returnResult<String>()
                    .responseBody
                    .collect(Collectors.joining())
                    .map(JsonNormalizer::forCards)
                    .block()
                    ?.replace(Regex("http://localhost:\\d+/"), "/")

            Assert.assertThat<String>(data, SameJSONAs.sameJSONAs(jsonResponse))
        }
    }

    @Test
    fun `Test Success For Empty Cards`(
            @Value("classpath:connector/responses/u5.json")
            response: Resource,
            @Value("classpath:service/api/users/u1.json")
            user: Resource,
            @Value("classpath:service/api/messages/message2.json")
            message: Resource,
            @Value("classpath:service/api/teams/teams1.json")
            team: Resource,
            @Value("classpath:service/api/batchResponse/teamsBatchResponse1.json")
            teamsBatchResponse: Resource,
            @Value("classpath:service/api/batchResponse/channelBatchResponse1.json")
            channelBatchResponse: Resource,
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
                .header(MESSAGE_ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .returnResult<String>()
                .responseBody
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block()
                ?.replace(Regex("http://localhost:\\d+/"), "/")

        Assert.assertThat<String>(data, SameJSONAs.sameJSONAs(jsonResponse))
    }
}