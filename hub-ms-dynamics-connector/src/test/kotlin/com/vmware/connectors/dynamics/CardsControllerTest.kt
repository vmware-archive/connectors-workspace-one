/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.dynamics

import com.backflipt.commons.*
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.vmware.connectors.dynamics.config.DATE_FORMAT_PATTERN
import com.vmware.connectors.dynamics.config.Endpoints
import com.vmware.connectors.dynamics.config.ROUTING_PREFIX
import com.vmware.connectors.test.ControllerTestsBase
import com.vmware.connectors.test.JsonNormalizer
import com.vmware.connectors.utils.IgnoredFieldsReplacer
import org.hamcrest.Matchers.startsWith
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.IMAGE_PNG_VALUE
import org.springframework.test.web.client.ExpectedCount.never
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs
import java.time.Duration
import java.util.stream.Collectors

private const val accountId = "account-id-1"
const val ROUTING_PREFIX_URL = "https://hero/connectors/ms-dynamics/"
const val userId = "u1"

class MSDynamicsActionsControllerTest : ControllerTestsBase() {
    private fun getAuthorizationToken(uri: String) = "Bearer " + accessToken(uri)
    private val authorization = "Bearer abc"

    @ParameterizedTest
    @ValueSource(strings = [
        "/cards/requests",
        "/accounts/1234/create_task",
        "/accounts/1234/schedule_appointment",
        "/accounts/1234/log_phonecall"
    ])

    @Throws(Exception::class)
    fun testProtectedResource(uri: String) {
        testProtectedResource(HttpMethod.POST, uri)
    }

    @Test
    fun testCardsRequestsWithCards(
            @Value("classpath:service/api/users/u1.json")
            profile: Resource,
            @Value("classpath:service/api/accounts/u1.json")
            accounts: Resource,
            @Value("classpath:service/api/contacts/u1.json")
            contacts: Resource,
            @Value("classpath:connector/responses/u1.json")
            response: Resource
    ) {
        mockBackend.expect(requestTo(Endpoints.getMyProfileUrl("")))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(profile, APPLICATION_JSON))
        mockBackend.expect(requestTo(startsWith(Endpoints.getPrimaryContactsForAccounts(""))))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(contacts, APPLICATION_JSON))
        val accountsUrl = Endpoints.getMyAccountsUrl("")
        mockBackend.expect(requestTo(startsWith(accountsUrl)))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(accounts, APPLICATION_JSON))
        val expectedResponse = response.readAsString()
        val uri = "/cards/requests"
        val actualData = webClient
                .mutate()
                .responseTimeout(Duration.ofMillis(10000L))
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
        assertThat(actualData, sameJSONAs(expectedResponse))
    }

    @Test
    fun testCardsRequestsWithEmptyCards(
            @Value("classpath:service/api/users/u2.json")
            profile: Resource,
            @Value("classpath:service/api/accounts/u2.json")
            accounts: Resource,
            @Value("classpath:connector/responses/u2.json")
            response: Resource
    ) {
        mockBackend.expect(requestTo(Endpoints.getMyProfileUrl("")))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(profile, APPLICATION_JSON))
        val accountsUrl = Endpoints.getMyAccountsUrl("")
        mockBackend.expect(requestTo(startsWith(accountsUrl)))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(accounts, APPLICATION_JSON))
        val expectedResponse = response.readAsString()
        val url = "/cards/requests"
        val actualData = webClient.post()
                .uri(url)
                .header(AUTHORIZATION, getAuthorizationToken(url))
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
        assertThat<String>(actualData, sameJSONAs(expectedResponse))
    }

    @Test
    fun missingAuthorization() {
        webClient.post()
                .uri("/cards/requests")
                .exchange()
                .expectStatus().isUnauthorized
    }

    @Test
    fun testCardsRequestsWithUnAuthorized(
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource
    ) {
        mockBackend.expect(requestTo(Endpoints.getMyProfileUrl("")))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withUnauthorizedRequest())
        val accountsUrl = Endpoints.getMyAccountsUrl("")
        mockBackend.expect(never(), requestTo(startsWith(accountsUrl)))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
        val expectedResponse = response.readAsString()
        val url = "/cards/requests"
        webClient.post()
                .uri(url)
                .header(AUTHORIZATION, getAuthorizationToken(url))
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

    @Test
    fun testCardsRequestsWithUnAuthorizedForGettingNewAccounts(
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource,
            @Value("classpath:service/api/users/u1.json")
            profile: Resource
    ) {
        mockBackend.expect(requestTo(Endpoints.getMyProfileUrl("")))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(profile, APPLICATION_JSON))
        val accountsUrl = Endpoints.getMyAccountsUrl("")
        mockBackend.expect(requestTo(startsWith(accountsUrl)))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withUnauthorizedRequest())
        val expectedResponse = response.readAsString()
        val url = "/cards/requests"
        webClient.post()
                .uri(url)
                .header(AUTHORIZATION, getAuthorizationToken(url))
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

    @Test
    fun testCardsRequestsWithUnAuthorizedForGettingPrimaryContactForAccounts(
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource,
            @Value("classpath:service/api/users/u1.json")
            profile: Resource,
            @Value("classpath:service/api/accounts/u1.json")
            accounts: Resource
    ) {
        mockBackend.expect(requestTo(Endpoints.getMyProfileUrl("")))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(profile, APPLICATION_JSON))
        val accountsUrl = Endpoints.getMyAccountsUrl("")
        mockBackend.expect(requestTo(startsWith(accountsUrl)))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withSuccess(accounts.readAsString(), APPLICATION_JSON))
        mockBackend.expect(requestTo(startsWith(Endpoints.getPrimaryContactsForAccounts(""))))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, authorization))
                .andRespond(withUnauthorizedRequest())
        val expectedResponse = response.readAsString()
        val url = "/cards/requests"
        webClient.post()
                .uri(url)
                .header(AUTHORIZATION, getAuthorizationToken(url))
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

    @ParameterizedTest
    @ValueSource(strings = [
        "/accounts/$accountId/create_task",
        "/accounts/$accountId/schedule_appointment",
        "/accounts/$accountId/schedule_phonecall"
    ])
    fun `Test Auth Fail For Actions`(uri: String) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getMyProfileUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest().body("bad_token"))
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
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
    fun `Test Invalid Account For Task Creation`(
            @Value("classpath:service/api/users/u1.json")
            profile: Resource
    ) {
        val dueDate = getCurrentDateRoundedBy30Mins()
        val params = mapOf(
                "subject" to "test subject",
                "description" to null,
                "scheduledend" to formatDate(dueDate, DATE_FORMAT_PATTERN),
                "ownerid@odata.bind" to """/systemusers($userId)""",
                "regardingobjectid_account_task@odata.bind" to """/accounts($accountId)"""
        )
                .filterValues { !it.isNullOrEmpty() }.serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getMyProfileUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(profile, MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getTasksUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(params))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND))
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
        val uri = "/accounts/$accountId/create_task"
        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is5xxServerError
    }

    @Test
    fun `Test Success For Task Creation`(
            @Value("classpath:service/api/users/u1.json")
            profile: Resource
    ) {
        val dueDate = getCurrentDateRoundedBy30Mins()
        val params = mapOf(
                "subject" to "test subject",
                "description" to null,
                "scheduledend" to formatDate(dueDate, DATE_FORMAT_PATTERN),
                "ownerid@odata.bind" to """/systemusers($userId)""",
                "regardingobjectid_account_task@odata.bind" to """/accounts($accountId)"""
        )
                .filterValues { !it.isNullOrEmpty() }.serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getMyProfileUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(profile, MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getTasksUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.content().string(params))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CREATED))
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
        val uri = "/accounts/$accountId/create_task"
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
    fun `Test Invalid Account For Appointment Creation`(
            @Value("classpath:service/api/users/u1.json")
            profile: Resource
    ) {
        val accountResource = """/accounts($accountId)"""
        val userResource = """/systemusers($userId)"""
        val parties = listOf(
                mapOf(
                        "partyid_account@odata.bind" to accountResource,
                        "participationtypemask" to 5
                ),
                mapOf(
                        "partyid_systemuser@odata.bind" to userResource,
                        "participationtypemask" to 7
                )
        )
        val startDate = getCurrentDateRoundedBy30Mins()
        val endDate = startDate.plusMinutes(30)
        val params = mapOf(
                "subject" to "test subject",
                "scheduledstart" to formatDate(startDate, DATE_FORMAT_PATTERN),
                "scheduledend" to formatDate(endDate, DATE_FORMAT_PATTERN),
                "ownerid@odata.bind" to userResource,
                "regardingobjectid_account_appointment@odata.bind" to accountResource,
                "appointment_activity_parties" to parties
        ).serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getMyProfileUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(profile, MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAppointmentsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(params))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND))
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
        val uri = "/accounts/$accountId/schedule_appointment"
        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is5xxServerError
    }

    @Test
    fun `Test Success For Appointment Creation`(
            @Value("classpath:service/api/users/u1.json")
            profile: Resource
    ) {
        val accountResource = """/accounts($accountId)"""
        val userResource = """/systemusers($userId)"""
        val parties = listOf(
                mapOf(
                        "partyid_account@odata.bind" to accountResource,
                        "participationtypemask" to 5
                ),
                mapOf(
                        "partyid_systemuser@odata.bind" to userResource,
                        "participationtypemask" to 7
                )
        )
        val startDate = getCurrentDateRoundedBy30Mins()
        val endDate = startDate.plusMinutes(30)
        val params = mapOf(
                "subject" to "test subject",
                "scheduledstart" to formatDate(startDate, DATE_FORMAT_PATTERN),
                "scheduledend" to formatDate(endDate, DATE_FORMAT_PATTERN),
                "ownerid@odata.bind" to userResource,
                "regardingobjectid_account_appointment@odata.bind" to accountResource,
                "appointment_activity_parties" to parties
        ).serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getMyProfileUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(profile, MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAppointmentsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.content().string(params))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CREATED))
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
        val uri = "/accounts/$accountId/schedule_appointment"
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
    fun `Test Invalid Account For Phone Call Creation`(
            @Value("classpath:service/api/users/u1.json")
            profile: Resource
    ) {
        val accountResource = """/accounts($accountId)"""
        val userResource = """/systemusers($userId)"""
        val parties = listOf(
                mapOf(
                        "partyid_account@odata.bind" to accountResource,
                        "participationtypemask" to 2
                ),
                mapOf(
                        "partyid_systemuser@odata.bind" to userResource,
                        "participationtypemask" to 1
                )
        )
        val params = mapOf(
                "subject" to "test subject",
                "description" to null,
                "phonenumber" to null,
                "scheduledend" to formatDate(getCurrentDateRoundedBy30Mins(), DATE_FORMAT_PATTERN),
                "ownerid@odata.bind" to userResource,
                "regardingobjectid_account_phonecall@odata.bind" to accountResource,
                "phonecall_activity_parties" to parties
        )
                .filterValues { it != null }.serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getMyProfileUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(profile, MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getPhoneCallsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.content().string(params))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND))
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
        val uri = "/accounts/$accountId/schedule_phonecall"
        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is5xxServerError
    }

    @Test
    fun `Test Success For Phone Call Creation`(
            @Value("classpath:service/api/users/u1.json")
            profile: Resource
    ) {
        val accountResource = """/accounts($accountId)"""
        val userResource = """/systemusers($userId)"""
        val parties = listOf(
                mapOf(
                        "partyid_account@odata.bind" to accountResource,
                        "participationtypemask" to 2
                ),
                mapOf(
                        "partyid_systemuser@odata.bind" to userResource,
                        "participationtypemask" to 1
                )
        )
        val params = mapOf(
                "subject" to "test subject",
                "description" to null,
                "phonenumber" to null,
                "scheduledend" to formatDate(getCurrentDateRoundedBy30Mins(), DATE_FORMAT_PATTERN),
                "ownerid@odata.bind" to userResource,
                "regardingobjectid_account_phonecall@odata.bind" to accountResource,
                "phonecall_activity_parties" to parties
        )
                .filterValues { it != null }.serialize()
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getMyProfileUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(profile, MediaType.APPLICATION_JSON))
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getPhoneCallsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.content().string(params))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CREATED))
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "test subject")
        val uri = "/accounts/$accountId/schedule_phonecall"
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

}