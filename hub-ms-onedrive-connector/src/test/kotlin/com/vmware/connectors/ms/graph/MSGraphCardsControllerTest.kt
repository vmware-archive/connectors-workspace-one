/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.ms.graph

import com.vmware.connectors.ms.graph.config.Endpoints
import com.vmware.connectors.ms.graph.config.ROUTING_PREFIX
import com.vmware.connectors.ms.graph.utils.*
import com.vmware.connectors.test.ControllerTestsBase
import com.vmware.connectors.test.JsonNormalizer
import org.hamcrest.Matchers.startsWith
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.IMAGE_PNG_VALUE
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs
import java.util.stream.Collectors

const val ROUTING_PREFIX_URL = "https://hero/connectors/ms-graph-connector/"
private const val accessRequestId = "1234"


class MSGraphCardsControllerTest : ControllerTestsBase() {

    private fun getAuthorizationToken(uri: String) = "Bearer " + accessToken(uri)
    private val authorization = "Bearer abc"

    @ParameterizedTest
    @ValueSource(strings = [
        "/cards/requests",
        "/access/requests/$accessRequestId/approve",
        "/access/requests/$accessRequestId/decline"
    ])
    @Throws(Exception::class)
    fun testProtectedResource(uri: String) {
        testProtectedResource(HttpMethod.POST, uri)
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
                .expectHeader().contentType(IMAGE_PNG_VALUE)
                .expectBody<ByteArray>().isEqualTo(icon.readAsByteArray())
    }


    @Test
    fun testCardsRequestsForCardsForOthers(
            @Value("classpath:service/api/mailResponses/u1.json")
            mailResponses: Resource,
            @Value("classpath:service/api/userTimeZone/u1.json")
            userTimeZone: Resource,
            @Value("classpath:service/api/siteUrlResponses/u1.json")
            siteUrlResponses: Resource,
            @Value("classpath:service/api/fileUrlResponses/u1.json")
            fileUrlResponses: Resource,
            @Value("classpath:service/api/relevantPeopleResponses/u1.json")
            relevantPeopleResponses: Resource,
            @Value("classpath:connector/responses/u1.json")
            response: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(startsWith(
                Endpoints.getAccessRequestMailsUrl("", "")
                        .replaceAfter("\$filter", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(mailResponses.readAsString(), APPLICATION_JSON))

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(userTimeZone.readAsString(), APPLICATION_JSON))

        val siteHostName = "backflipt-my.sharepoint.com"
        val sitePath = "/personal/pavan_backflipt_com"

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getSiteIdCallUrl("", siteHostName, sitePath)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(siteUrlResponses.readAsString(), APPLICATION_JSON))

        val siteId = "backflipt-my.sharepoint.com,3b4fbdfd-a6c1-4b4e-a5a1-ba26e2e81243,7849a5b3-019f-4bc7-a407-65f41107c481"
        val filePath = "/Backflipt%20Discover%20Canned%20Demo%20Documents/Arista%207000%20series%20-%2072/7368X4-Series-QA.pdf"

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getFileIdFromFilePathUrl("", siteId, filePath)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(fileUrlResponses.readAsString(), APPLICATION_JSON))


        mockBackend.expect(MockRestRequestMatchers.requestTo(startsWith(
                Endpoints.getRelevantPeopleWithNameUrl("", "")
                        .replaceAfter("\$search", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(relevantPeopleResponses.readAsString(), APPLICATION_JSON))

        val jsonResponse = response.readAsString()

        val uri = "/cards/requests"

        val data = webClient.post()
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

        assertThat<String>(data, sameJSONAs(jsonResponse))
    }

    @Test
    fun testCardsRequestsForCardsForGroupAccessRequest(
            @Value("classpath:service/api/mailResponses/u2.json")
            mailResponses: Resource,
            @Value("classpath:service/api/userTimeZone/u1.json")
            userTimeZone: Resource,
            @Value("classpath:service/api/siteUrlResponses/u1.json")
            siteUrlResponses: Resource,
            @Value("classpath:service/api/fileUrlResponses/u1.json")
            fileUrlResponses: Resource,
            @Value("classpath:service/api/relevantPeopleResponses/u1.json")
            relevantPeopleResponses: Resource,
            @Value("classpath:connector/responses/u2.json")
            response: Resource,
            @Value("classpath:service/api/users/knownUsers.json")
            knownUsers: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(startsWith(
                Endpoints.getAccessRequestMailsUrl("", "")
                        .replaceAfter("\$filter", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(mailResponses.readAsString(), APPLICATION_JSON))

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(userTimeZone.readAsString(), APPLICATION_JSON))

        val siteHostName = "backflipt-my.sharepoint.com"
        val sitePath = "/personal/pavan_backflipt_com"

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getSiteIdCallUrl("", siteHostName, sitePath)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(siteUrlResponses.readAsString(), APPLICATION_JSON))

        val siteId = "backflipt-my.sharepoint.com,3b4fbdfd-a6c1-4b4e-a5a1-ba26e2e81243,7849a5b3-019f-4bc7-a407-65f41107c481"
        val filePath = "/Backflipt%20Discover%20Canned%20Demo%20Documents/Nokia%20-%20120/Nokia_100_1000_-_RH-130_RTTE_DoC.pdf"

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getFileIdFromFilePathUrl("", siteId, filePath)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(fileUrlResponses.readAsString(), APPLICATION_JSON))


        mockBackend.expect(MockRestRequestMatchers.requestTo(startsWith(
                Endpoints.getRelevantPeopleWithNameUrl("", "")
                        .replaceAfter("\$search", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(relevantPeopleResponses.readAsString(), APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllUsers("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(knownUsers.readAsString(), APPLICATION_JSON))


        val jsonResponse = response.readAsString()

        val uri = "/cards/requests"

        val data = webClient.post()
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

        assertThat<String>(data, sameJSONAs(jsonResponse))
    }

    @Test
    fun testCardsRequestsForCardsForDirectAccessRequest(
            @Value("classpath:service/api/mailResponses/u4.json")
            mailResponses: Resource,
            @Value("classpath:service/api/userTimeZone/u1.json")
            userTimeZone: Resource,
            @Value("classpath:service/api/siteUrlResponses/u1.json")
            siteUrlResponses: Resource,
            @Value("classpath:service/api/fileUrlResponses/u1.json")
            fileUrlResponses: Resource,
            @Value("classpath:connector/responses/u3.json")
            response: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(startsWith(
                Endpoints.getAccessRequestMailsUrl("", "")
                        .replaceAfter("\$filter", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(mailResponses.readAsString(), APPLICATION_JSON))

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(userTimeZone.readAsString(), APPLICATION_JSON))

        val siteHostName = "backflipt-my.sharepoint.com"
        val sitePath = "/personal/pavan_backflipt_com"

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getSiteIdCallUrl("", siteHostName, sitePath)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(siteUrlResponses.readAsString(), APPLICATION_JSON))

        val siteId = "backflipt-my.sharepoint.com,3b4fbdfd-a6c1-4b4e-a5a1-ba26e2e81243,7849a5b3-019f-4bc7-a407-65f41107c481"
        val filePath = "/Backflipt%20Discover%20Canned%20Demo%20Documents/Nokia%20-%20120/Nokia_101_1010_%20-_%20RM-769_RTTE_DoC.pdf"

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getFileIdFromFilePathUrl("", siteId, filePath)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(fileUrlResponses.readAsString(), APPLICATION_JSON))

        val jsonResponse = response.readAsString()

        val uri = "/cards/requests"

        val data = webClient.post()
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

        assertThat<String>(data, sameJSONAs(jsonResponse))
    }

    @Test
    fun testCardsRequestsWithEmptyCards(
            @Value("classpath:service/api/mailResponses/u1.json")
            mailResponses: Resource,
            @Value("classpath:service/api/userTimeZone/u2.json")
            userTimeZone: Resource,
            @Value("classpath:service/api/usersResponses/u1.json")
            users: Resource,
            @Value("classpath:service/api/siteUrlResponses/u1.json")
            siteUrlResponses: Resource,
            @Value("classpath:service/api/fileUrlResponses/u2.json")
            fileUrlResponses: Resource,
            @Value("classpath:service/api/relevantPeopleResponses/u2.json")
            relevantPeopleResponses: Resource,
            @Value("classpath:connector/responses/u2.json")
            response: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(startsWith(
                Endpoints.getAccessRequestMailsUrl("", "")
                        .replaceAfter("\$filter", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(mailResponses.readAsString(), APPLICATION_JSON))

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(userTimeZone.readAsString(), APPLICATION_JSON))

        val siteHostName = "backflipt-my.sharepoint.com"
        val sitePath = "/personal/pavan_backflipt_com"

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getSiteIdCallUrl("", siteHostName, sitePath)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(siteUrlResponses.readAsString(), APPLICATION_JSON))

        val siteId = "backflipt-my.sharepoint.com,3b4fbdfd-a6c1-4b4e-a5a1-ba26e2e81243,7849a5b3-019f-4bc7-a407-65f41107c481"
        val filePath = "/Backflipt%20Discover%20Canned%20Demo%20Documents/Arista%207000%20series%20-%2072/7368X4-Series-QA.pdf"

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getFileIdFromFilePathUrl("", siteId, filePath)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(fileUrlResponses.readAsString(), APPLICATION_JSON))


        mockBackend.expect(MockRestRequestMatchers.requestTo(startsWith(
                Endpoints.getRelevantPeopleWithNameUrl("", "")
                        .replaceAfter("\$search", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(relevantPeopleResponses.readAsString(), APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllUsers("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(users, APPLICATION_JSON))

        val jsonResponse = response.readAsString()

        val data = webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, getAuthorizationToken("/cards/requests"))
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

        assertThat<String>(data, sameJSONAs(jsonResponse))
    }

    @Test
    fun testCardsRequestsWithEmptyCardsWithBadMailsFormat(
            @Value("classpath:service/api/mailResponses/u5.json")
            mailResponses: Resource,
            @Value("classpath:service/api/userTimeZone/u2.json")
            userTimeZone: Resource,
            @Value("classpath:connector/responses/u2.json")
            response: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(startsWith(
                Endpoints.getAccessRequestMailsUrl("", "")
                        .replaceAfter("\$filter", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(mailResponses.readAsString(), APPLICATION_JSON))

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(userTimeZone.readAsString(), APPLICATION_JSON))

        val jsonResponse = response.readAsString()

        val data = webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, getAuthorizationToken("/cards/requests"))
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

        assertThat<String>(data, sameJSONAs(jsonResponse))
    }

    @Test
    fun testCardsRequestsWithEmptyCardsWithNoSharePointFiles(
            @Value("classpath:service/api/mailResponses/u3.json")
            mailResponses: Resource,
            @Value("classpath:service/api/userTimeZone/u1.json")
            userTimeZone: Resource,
            @Value("classpath:connector/responses/u2.json")
            response: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(startsWith(
                Endpoints.getAccessRequestMailsUrl("", "")
                        .replaceAfter("\$filter", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(mailResponses.readAsString(), APPLICATION_JSON))

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(userTimeZone.readAsString(), APPLICATION_JSON))

        val jsonResponse = response.readAsString()

        val data = webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, getAuthorizationToken("/cards/requests"))
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

        assertThat<String>(data, sameJSONAs(jsonResponse))
    }

    @Test
    fun testCardsRequestsWithEmptyCardsWithNoEmailBody(
            @Value("classpath:service/api/mailResponses/u6.json")
            mailResponses: Resource,
            @Value("classpath:service/api/userTimeZone/u1.json")
            userTimeZone: Resource,
            @Value("classpath:connector/responses/u2.json")
            response: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(startsWith(
                Endpoints.getAccessRequestMailsUrl("", "")
                        .replaceAfter("\$filter", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(mailResponses.readAsString(), APPLICATION_JSON))

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(userTimeZone.readAsString(), APPLICATION_JSON))

        val jsonResponse = response.readAsString()

        val data = webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, getAuthorizationToken("/cards/requests"))
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

        assertThat<String>(data, sameJSONAs(jsonResponse))
    }

    @Test
    fun missingAuthorization() {
        webClient.post()
                .uri("/cards/requests")
                .exchange()
                .expectStatus().isUnauthorized
    }

    @Test
    fun testAuthFailForCardsRequests(
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withUnauthorizedRequest())


        val jsonResponse = response.readAsString()

        webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, getAuthorizationToken("/cards/requests"))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(jsonResponse)
    }

    @Test
    fun testAuthFailForCardsRequestsWithExpiredTokenForFileUrlRequest(
            @Value("classpath:service/api/mailResponses/u1.json")
            mailResponses: Resource,
            @Value("classpath:service/api/userTimeZone/u2.json")
            userTimeZone: Resource,
            @Value("classpath:service/api/usersResponses/u1.json")
            users: Resource,
            @Value("classpath:service/api/siteUrlResponses/u1.json")
            siteUrlResponses: Resource,
            @Value("classpath:service/api/fileUrlResponses/u2.json")
            fileUrlResponses: Resource,
            @Value("classpath:service/api/relevantPeopleResponses/u2.json")
            relevantPeopleResponses: Resource,
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(startsWith(
                Endpoints.getAccessRequestMailsUrl("", "")
                        .replaceAfter("\$filter", "")
        )))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(mailResponses.readAsString(), APPLICATION_JSON))

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(userTimeZone.readAsString(), APPLICATION_JSON))

        val siteHostName = "backflipt-my.sharepoint.com"
        val sitePath = "/personal/pavan_backflipt_com"

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getSiteIdCallUrl("", siteHostName, sitePath)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(siteUrlResponses.readAsString(), APPLICATION_JSON))

        val siteId = "backflipt-my.sharepoint.com,3b4fbdfd-a6c1-4b4e-a5a1-ba26e2e81243,7849a5b3-019f-4bc7-a407-65f41107c481"
        val filePath = "/Backflipt%20Discover%20Canned%20Demo%20Documents/Arista%207000%20series%20-%2072/7368X4-Series-QA.pdf"

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getFileIdFromFilePathUrl("", siteId, filePath)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withUnauthorizedRequest())

        val jsonResponse = response.readAsString()

        webClient.post()
                .uri("/cards/requests")
                .header(AUTHORIZATION, getAuthorizationToken("/cards/requests"))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
                .expectHeader().contentTypeCompatibleWith(APPLICATION_JSON)
                .expectBody().json(jsonResponse)

    }

    @Test
    fun testAuthFailForApproveAction(
            @Value("classpath:service/api/accessRequests/u1.json")
            accessRequest: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.inviteUrl("", "backflipt-my.sharepoint.com,3b4fbdfd-a6c1-4b4e-a5a1-ba26e2e81243,7849a5b3-019f-4bc7-a407-65f41107c481", "0176WBI5ACLJLNVM7QXJBZ7TVV5DZ4BYIT")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withUnauthorizedRequest().body("No OAuth"))

        val formData = LinkedMultiValueMap<String, String>()
        formData.add("accessRequest", accessRequest.readAsString())
        formData.add("roles", "write")
        val uri = "/access/requests/$accessRequestId/approve"

        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
    }

    @Test
    fun testAuthFailForDeclineAction(
            @Value("classpath:service/api/accessRequests/u1.json")
            accessRequest: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.sendMailUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withUnauthorizedRequest())

        val formData = LinkedMultiValueMap<String, String>()
        formData.add("accessRequest", accessRequest.readAsString())
        val uri = "/access/requests/$accessRequestId/decline"

        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
    }


    @Test
    fun testSuccessForApproveActionWithNoComments(
            @Value("classpath:service/api/accessRequests/u1.json")
            accessRequest: Resource,
            @Value("classpath:service/api/invite/u1.json")
            invite: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.inviteUrl("", "backflipt-my.sharepoint.com,3b4fbdfd-a6c1-4b4e-a5a1-ba26e2e81243,7849a5b3-019f-4bc7-a407-65f41107c481", "0176WBI5ACLJLNVM7QXJBZ7TVV5DZ4BYIT")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(invite.readAsString(), APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.sendMailUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess())

        val formData = LinkedMultiValueMap<String, String>()
        formData.add("accessRequest", accessRequest.readAsString())
        formData.add("roles", "write")
        val uri = "/access/requests/$accessRequestId/approve"

        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isCreated
    }

    @Test
    fun testSuccessForApproveActionForSharedGroup(
            @Value("classpath:service/api/accessRequests/u4.json")
            accessRequest: Resource,
            @Value("classpath:service/api/invite/u1.json")
            invite: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.inviteUrl("", "backflipt-my.sharepoint.com,3b4fbdfd-a6c1-4b4e-a5a1-ba26e2e81243,7849a5b3-019f-4bc7-a407-65f41107c481", "0176WBI5ACLJLNVM7QXJBZ7TVV5DZ4BYIT")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(invite.readAsString(), APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.sendMailUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess())

        val formData = LinkedMultiValueMap<String, String>()
        formData.add("accessRequest", accessRequest.readAsString())
        formData.add("roles", "write")
        val uri = "/access/requests/$accessRequestId/approve"

        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isCreated
    }

    @Test
    fun testSuccessForApproveActionWithNoRequester(
            @Value("classpath:service/api/accessRequests/u3.json")
            accessRequest: Resource,
            @Value("classpath:service/api/invite/u1.json")
            invite: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.inviteUrl("", "backflipt-my.sharepoint.com,3b4fbdfd-a6c1-4b4e-a5a1-ba26e2e81243,7849a5b3-019f-4bc7-a407-65f41107c481", "0176WBI5ACLJLNVM7QXJBZ7TVV5DZ4BYIT")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(invite.readAsString(), APPLICATION_JSON))

        val formData = LinkedMultiValueMap<String, String>()

        formData.add("accessRequest", accessRequest.readAsString())
        formData.add("roles", "write")
        formData.add("comments", "ok")
        val uri = "/access/requests/$accessRequestId/approve"

        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isCreated
    }

    @Test
    fun testSuccessForDeclineAction(
            @Value("classpath:service/api/accessRequests/u1.json")
            accessRequest: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.sendMailUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess())

        val formData = LinkedMultiValueMap<String, String>()
        formData.add("accessRequest", accessRequest.readAsString())
        val uri = "/access/requests/$accessRequestId/decline"

        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isCreated
    }

    @Test
    fun testSuccessForDeclineActionWithNoRequester(
            @Value("classpath:service/api/accessRequests/u3.json")
            accessRequest: Resource
    ) {
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("accessRequest", accessRequest.readAsString())
        val uri = "/access/requests/$accessRequestId/decline"

        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isCreated
    }

    @Test
    fun miscellaneous() {
        val map = mapOf("string" to null)
        val resp = map.getStringOrDefault("s", "Unit Test")
        Assert.assertEquals("Unit Test", resp)
        val nullRespForList = map.getListOrNull<Any>("s")
        Assert.assertEquals(null, nullRespForList)
        val nullRespForString = map.getStringOrNull("s")
        Assert.assertEquals(null, nullRespForString)
        val mapResp = map.getStringOrDefault("s")
        Assert.assertEquals("", mapResp)
        try {
            map.getListOrException<String>("f")
        } catch (e: java.lang.Exception) {
            Assert.assertEquals(null, e.localizedMessage)
        }
        val userNameFromToken = VmwareUtils.getUserEmailFromToken("token s.bnVsbA==")
        Assert.assertEquals(null, userNameFromToken)
        val nullableResponse = null.toBase64DecodedString()
        Assert.assertEquals(null, nullableResponse)
        val s = "2020-02-25T12:18:10"
        try {
            parseDate(s, "UTC")
        } catch (e: java.lang.Exception) {
            Assert.assertEquals("Unparseable date: 2020-02-25T12:18:10", e.message)
        }
    }
}