/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.boxNotes

import com.backflipt.commons.readAsString
import com.backflipt.commons.serialize
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.vmware.connectors.boxNotes.config.Endpoints
import com.vmware.connectors.boxNotes.config.ROUTING_PREFIX
import com.vmware.connectors.test.ControllerTestsBase
import com.vmware.connectors.test.JsonNormalizer
import com.vmware.connectors.utils.IgnoredFieldsReplacer
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
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import uk.co.datumedge.hamcrest.json.SameJSONAs
import java.time.Duration
import java.util.stream.Collectors

const val ROUTING_PREFIX_URL = "https://hero/connectors/box_notes/"
const val CARDS_URL = "/cards/requests"

class CardsControllerTests : ControllerTestsBase() {
    private fun getAuthorizationToken(uri: String) = "Bearer " + accessToken(uri)
    private val authorization = "Bearer abc"

    @ParameterizedTest
    @ValueSource(strings = [
        "/cards/request",
        "/notes/1234/addUser",
        "/notes/1234/message"
    ])
    @Throws(Exception::class)
    fun testProtectedResource(uri: String) {
        testProtectedResource(HttpMethod.POST, uri)
    }

    @Test
    fun testSuccessForCardsRequest(
            @Value("classpath:service/api/userResp.json")
            userResponse: Resource,
            @Value("classpath:service/api/notesResponse.json")
            notesResponse: Resource,
            @Value("classpath:service/api/collaborationsList.json")
            collabarations: Resource,
            @Value("classpath:service/api/commentsResponse.json")
            commentsResponse: Resource,
            @Value("classpath:connector/responses/cardsResponse.json")
            cardsResponse: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserInfo("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(userResponse.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo("/search?query=boxnote&fields=extension,name&type=file&file_extensions=boxnote&content_types=%255B%2522name%2522%252C%2522description%2522%255D"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(notesResponse.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.listCollaboratorsUrl("", "650920505401")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(collabarations.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.listNoteComments("", "650920505401")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(commentsResponse.readAsString(), MediaType.APPLICATION_JSON))
        val uri = CARDS_URL
        val expectedResponse = cardsResponse.readAsString()
        val actualData = webClient.mutate()
                .responseTimeout(Duration.ofMillis(10000))
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
                .map(JsonNormalizer::forCards)
                .block()
                ?.replace(Regex("http://localhost:\\d+/"), "/")
        Assert.assertThat(actualData, SameJSONAs.sameJSONAs(expectedResponse))
    }

    @Test
    fun testSuccessForEmptyCardsRequest(
            @Value("classpath:service/api/userResp.json")
            userResponse: Resource,
            @Value("classpath:service/api/notesResponse.json")
            notesResponse: Resource,
            @Value("classpath:service/api/collaborationsList.json")
            collabarations: Resource,
            @Value("classpath:service/api/emptyCommentResponse.json")
            commentsResponse: Resource,
            @Value("classpath:connector/responses/emptyResponse.json")
            Response: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserInfo("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(userResponse.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo("/search?query=boxnote&fields=extension,name&type=file&file_extensions=boxnote&content_types=%255B%2522name%2522%252C%2522description%2522%255D"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(notesResponse.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.listCollaboratorsUrl("", "650920505401")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(collabarations.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.listNoteComments("", "650920505401")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(commentsResponse.readAsString(), MediaType.APPLICATION_JSON))
        val uri = CARDS_URL
        val expectedResponse = Response.readAsString()
        val data = webClient.mutate()
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
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserInfo("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest().body("BAD_TOKEN"))
        val cardsUrl = CARDS_URL
        val expectedResponse = response.readAsString()
        webClient.post()
                .uri(cardsUrl)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(cardsUrl))
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
                .expectBody().json(expectedResponse)
    }

    @Test
    fun testAuthFailForCardsRequestWhileFetchingUserNotes(
            @Value("classpath:service/api/userResp.json")
            userResponse: Resource,
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserInfo("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(userResponse.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo("/search?query=boxnote&fields=extension,name&type=file&file_extensions=boxnote&content_types=%255B%2522name%2522%252C%2522description%2522%255D"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())
        val cardsUrl = CARDS_URL
        val expectedResponse = response.readAsString()
        webClient.post()
                .uri(cardsUrl)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(cardsUrl))
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
                .expectBody().json(expectedResponse)
    }

    @Test
    fun testAuthFailForCardsRequestWhileFetchingNoteCollaborators(
            @Value("classpath:service/api/userResp.json")
            userResponse: Resource,
            @Value("classpath:service/api/notesResponse.json")
            notesResponse: Resource,
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserInfo("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(userResponse.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo("/search?query=boxnote&fields=extension,name&type=file&file_extensions=boxnote&content_types=%255B%2522name%2522%252C%2522description%2522%255D"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(notesResponse.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.listCollaboratorsUrl("", "650920505401")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())
        val cardsUrl = CARDS_URL
        val expectedResponse = response.readAsString()
        webClient.post()
                .uri(cardsUrl)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(cardsUrl))
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
                .expectBody().json(expectedResponse)
    }

    @Test
    fun testAuthFailForCardsRequestWhileFetchingNoteComments(
            @Value("classpath:service/api/userResp.json")
            userResponse: Resource,
            @Value("classpath:service/api/notesResponse.json")
            notesResponse: Resource,
            @Value("classpath:service/api/collaborationsList.json")
            collabarations: Resource,
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserInfo("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(userResponse.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo("/search?query=boxnote&fields=extension,name&type=file&file_extensions=boxnote&content_types=%255B%2522name%2522%252C%2522description%2522%255D"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(notesResponse.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.listCollaboratorsUrl("", "650920505401")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(collabarations.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.listNoteComments("", "650920505401")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())
        val cardsUrl = CARDS_URL
        val expectedResponse = response.readAsString()
        webClient.post()
                .uri(cardsUrl)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(cardsUrl))
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
                .expectBody().json(expectedResponse)
    }

    @Test
    fun testSuccessForAddUserRequest(
            @Value("classpath:service/api/userResp.json")
            user: Resource,
            @Value("classpath:service/api/userNoteResp.json")
            note: Resource
    ) {
        val body = mapOf(
                "item" to mapOf(
                        "type" to "file",
                        "id" to "647176810613"
                ),
                "accessible_by" to mapOf(
                        "type" to "user",
                        "login" to "abc@gmail.com"
                ),
                "role" to "editor"

        ).serialize()

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserInfo("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.inviteUserToNoteUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(body))
                .andRespond(MockRestResponseCreators.withSuccess())
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "abc@gmail.com")
        formData.add("actionType", "comment")
        formData.add("document", note.readAsString())
        val uri = "/notes/1234/addUser"
        webClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .header(X_AUTH_HEADER, authorization)
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .exchange()
                .expectStatus().isOk
    }


    @Test
    fun testAuthFailForAddUserRequest(
            @Value("classpath:service/api/userResp.json")
            user: Resource,
            @Value("classpath:service/api/userNoteResp.json")
            note: Resource
    ) {
        val body = mapOf(
                "item" to mapOf(
                        "type" to "file",
                        "id" to "647176810613"
                ),
                "accessible_by" to mapOf(
                        "type" to "user",
                        "login" to "abc@gmail.com"
                ),
                "role" to "editor"

        ).serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserInfo("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.inviteUserToNoteUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(body))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest().body("BAD_TOKEN"))
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "abc@gmail.com")
        formData.add("actionType", "comment")
        formData.add("document", note.readAsString())
        val uri = "/notes/1234/addUser"
        webClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .header(X_AUTH_HEADER, authorization)
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .exchange()
                .expectStatus().isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
    }

    @Test
    fun testSuccessForAddingUserWithInValidMail(
            @Value("classpath:service/api/userResp.json")
            user: Resource,
            @Value("classpath:service/api/userNoteResp.json")
            note: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserInfo("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test")
        formData.add("actionType", "comment")
        formData.add("document", note.readAsString())
        val uri = "/notes/1234/addUser"
        try {
            webClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                    .header(X_AUTH_HEADER, authorization)
                    .header(X_BASE_URL_HEADER, mockBackend.url(""))
                    .body(BodyInserters.fromFormData(formData))
                    .exchange()
        } catch (e: Exception) {
        }
    }

    @Test
    fun testSuccessForAddMessageRequest(
            @Value("classpath:service/api/userResp.json")
            user: Resource,
            @Value("classpath:service/api/commentNoteResp.json")
            note: Resource
    ) {
        val addMessageBody = mapOf(
                "item" to mapOf(
                        "type" to "file",
                        "id" to "647176810613"
                ),
                "message" to "hello"

        ).serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserInfo("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.addComment("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(addMessageBody))
                .andRespond(MockRestResponseCreators.withSuccess())
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "hello")
        formData.add("actionType", "comment")
        formData.add("document", note.readAsString())
        val uri = "/notes/1234/message"
        webClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .header(X_AUTH_HEADER, authorization)
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .exchange()
                .expectStatus().isOk
    }

    @Test
    fun testAuthFailForAddMessageRequest(
            @Value("classpath:service/api/userResp.json")
            user: Resource,
            @Value("classpath:service/api/commentNoteResp.json")
            note: Resource
    ) {
        val addMessageBody = mapOf(
                "item" to mapOf(
                        "type" to "file",
                        "id" to "647176810613"
                ),
                "message" to "hello"

        ).serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserInfo("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.addComment("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(addMessageBody))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest().body("BAD_TOKEN"))
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "hello")
        formData.add("actionType", "comment")
        formData.add("document", note.readAsString())
        val uri = "/notes/1234/message"
        webClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .header(X_AUTH_HEADER, authorization)
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .exchange()
                .expectStatus().isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
    }
}