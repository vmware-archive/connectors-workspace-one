/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.googleDocs

import com.backflipt.commons.readAsString
import com.backflipt.commons.serialize
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.vmware.connectors.googleDocs.config.Endpoints
import com.vmware.connectors.googleDocs.config.ROUTING_PREFIX
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

const val ROUTING_PREFIX_URL = "https://hero/connectors/google_docs/"
const val commentId = "AAAAJZS-Maw"
const val documentId = "13L7Xr357ISKNocGJKYK2rv3_Y_5aO1PnMszM3Oq4DsM"

class CardsControllerTest : ControllerTestsBase() {
    private fun getAuthorizationToken(uri: String) = "Bearer " + accessToken(uri)
    private val authorization = "Bearer abc"

    @ParameterizedTest
    @ValueSource(strings = [
        "/cards/requests",
        "/doc/$documentId/addUser",
        "/$commentId/reply"
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
            @Value("classpath:service/api/DocumentResponses/Documents.json")
            documents: Resource,
            @Value("classpath:service/api/UserResponses/User.json")
            userResponse: Resource,
            @Value("classpath:service/api/CommentsResponse/comments.json")
            commentsResponse: Resource,
            @Value("classpath:service/api/DocumentUsersResponses/DocumentUsers.json")
            documentUsers: Resource,
            @Value("classpath:ConnectorResponses/CardResponse.json")
            cardResponse: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getGoogleDocumentsUrl("", "mimeType%3D'application/vnd.google-apps.document'", "items(id,modifiedDate,modifiedByMeDate)")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(documents.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.listCommentsAndRepliesUrl("", "13L7Xr357ISKNocGJKYK2rv3_Y_5aO1PnMszM3Oq4DsM", "items(commentId,modifiedDate,author(displayName),content,status,fileTitle,replies(replyId,content,modifiedDate,author(displayName),verb))")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(commentsResponse.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getCurrentUserMailUrl("", "name,user(emailAddress)")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(userResponse.readAsString(), MediaType.APPLICATION_JSON))
        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.listDocumentUsersUrl("", "13L7Xr357ISKNocGJKYK2rv3_Y_5aO1PnMszM3Oq4DsM")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(documentUsers.readAsString(), MediaType.APPLICATION_JSON))
        val expectedResponse = cardResponse.readAsString()
        val uri = "/cards/requests"
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

        Assert.assertThat(data, SameJSONAs.sameJSONAs(expectedResponse))
    }

    @Test
    fun testSuccessForEmptyCardsRequest(
            @Value("classpath:service/api/DocumentResponses/EmptyDocuments.json")
            documents: Resource,
            @Value("classpath:ConnectorResponses/EmptyCardResponse.json")
            cardResponse: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getGoogleDocumentsUrl("", "mimeType%3D'application/vnd.google-apps.document'", "items(id,modifiedDate,modifiedByMeDate)")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(documents.readAsString(), MediaType.APPLICATION_JSON))
        val expectedResponse = cardResponse.readAsString()
        val uri = "/cards/requests"
        val data = webClient.mutate().responseTimeout(Duration.ofMillis(10000L))
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
        Assert.assertThat<String>(data, SameJSONAs.sameJSONAs(expectedResponse))
    }

    @Test
    fun testAuthFailForCardsRequest(
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getGoogleDocumentsUrl("", "mimeType%3D'application/vnd.google-apps.document'", "items(id,modifiedDate,modifiedByMeDate)")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest().body("BAD_OAUTH_TOKEN"))
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
                .expectStatus()
                .isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().json(expectedResponse)
    }

    @Test
    fun testSuccessForAddingUserToDocument(
            @Value("classpath:service/api/DocumentResponses/SingleDocument.json")
            commentResponse: Resource
    ) {
        val addUserBody = mapOf(
                "role" to "writer",
                "type" to "user",
                "value" to "test@gmail.com"
        ).serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.inviteUserToDocumentUrl("", "13L7Xr357ISKNocGJKYK2rv3_Y_5aO1PnMszM3Oq4DsM")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(addUserBody))
                .andRespond(MockRestResponseCreators.withSuccess())
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test@gmail.com")
        formData.add("document", commentResponse.readAsString())
        val uri = "/doc/$documentId/addUser"
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
    fun testAuthFailForAddingUserToDocument(
            @Value("classpath:service/api/DocumentResponses/SingleDocument.json")
            commentResponse: Resource
    ) {
        val addUserBody = mapOf(
                "role" to "writer",
                "type" to "user",
                "value" to "test@gmail.com"
        ).serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.inviteUserToDocumentUrl("", "13L7Xr357ISKNocGJKYK2rv3_Y_5aO1PnMszM3Oq4DsM")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(addUserBody))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test@gmail.com")
        formData.add("document", commentResponse.readAsString())
        val uri = "/doc/$documentId/addUser"
        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
    }

    @Test
    fun testSuccessForAddingUserWithInValidMail(
            @Value("classpath:service/api/DocumentResponses/SingleDocument.json")
            commentResponse: Resource
    ) {
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test.com")
        formData.add("document", commentResponse.readAsString())
        val uri = "/doc/$documentId/addUser"
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
    fun testSuccessForReplyToComment(
            @Value("classpath:service/api/DocumentResponses/SingleDocument.json")
            commentResponse: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.replyToMessageUrl("", "13L7Xr357ISKNocGJKYK2rv3_Y_5aO1PnMszM3Oq4DsM", "AAAAGa2TfUM", "*")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(mapOf("content" to "test").serialize()))
                .andRespond(MockRestResponseCreators.withSuccess())
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test")
        formData.add("document", commentResponse.readAsString())
        val uri = "/comment/$commentId/reply"
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
    fun testAthFailForReplyToComment(
            @Value("classpath:service/api/DocumentResponses/SingleDocument.json")
            commentResponse: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.replyToMessageUrl("", "13L7Xr357ISKNocGJKYK2rv3_Y_5aO1PnMszM3Oq4DsM", "AAAAGa2TfUM", "*")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(mapOf("content" to "test").serialize()))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test")
        formData.add("document", commentResponse.readAsString())
        val uri = "/comment/$commentId/reply"
        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
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
