/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.dynamics.service

import com.backflipt.commons.*
import com.vmware.connectors.dynamics.config.DATE_FORMAT_PATTERN
import com.vmware.connectors.dynamics.config.Endpoints
import com.vmware.connectors.dynamics.dto.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

/**
 * BackendService class for MS Dynamics
 *
 * @property client: WebClient: library to make async http calls
 */
@Component
class BackendService(@Autowired private val client: WebClient) {

    private val logger = getLogger()

    /**
     * getUserProfile gets the basic user profile of the dynamics user
     *
     * @param baseURI is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @return userProfile of the user for this baseURI
     */
    private suspend fun getUserProfile(baseURI: String, authorization: String): Map<String, Any> {
        return client
                .get()
                .uri(Endpoints.getMyProfileUrl(baseURI))
                .header(AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error while fetching profile for user!" }
                }
                .awaitBody()
    }

    /**
     * gets the user id of the user from ms dynamics service
     *
     * @param baseURI is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @return userId of the user for this baseURI
     */
    private suspend fun getUserId(baseURI: String, authorization: String): String {
        return getUserProfile(baseURI, authorization)
                .getStringOrException("UserId")
    }

    /**
     * fetches accounts for user
     *
     * @param baseUrl is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @param selectKeys is the list of keys call expects
     * @param filterQuery Query filter on parameters of the Account records
     * @return list of accounts
     */
    private suspend fun fetchAccounts(baseUrl: String,
                                      authorization: String,
                                      selectKeys: String,
                                      filterQuery: String): List<Account> {
        val url = Endpoints.getMyAccountsUrl(baseUrl) + "?\$select={0}&\$filter={1}"
        return client
                .get()
                .uri(url, selectKeys, filterQuery)
                .header(AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error while fetching new accounts for user with url:$url" }
                }
                .awaitBody<Map<String, Any>>()
                .getListOrDefault<Map<String, Any>>("value")
                .let {
                    JsonParser.convertValue(it)
                }
    }

    /**
     * fetches accounts that are assigned to user since last 24 hours
     *
     * @param baseUrl is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @return list of accounts assigned to user
     */
    suspend fun fetchNewAccountsCreatedSince24Hours(baseUrl: String, authorization: String): List<Account> {
        val userId = getUserId(baseUrl, authorization)
        val dateTime = getYesterdayDateTime(DATE_FORMAT_PATTERN)
        val selectKeys = """accountid,name,createdon,modifiedon,telephone1,description,websiteurl"""
        val filterQuery = """_ownerid_value eq '$userId' and statecode eq 0 and createdon ge '$dateTime'"""
        val accounts = fetchAccounts(baseUrl, authorization, selectKeys, filterQuery)
        val accountIds = accounts.map { it.id }
        val contacts = fetchPrimaryContactForAccounts(accountIds, baseUrl, authorization)
        return accounts
                .map {
                    val primaryContact = contacts?.get(it.id)
                    if (primaryContact != null) it.copy(primaryContact = primaryContact)
                    else it
                }
    }

    /**
     * fetches primary contact assigned to given accounts
     *
     * @param baseURI is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @return Map<String, Contact>: with key: accountId and value: Contact
     */
    private suspend fun fetchPrimaryContactForAccounts(accountIds: List<String>, baseURI: String, authorization: String): Map<String, Contact>? {
        if (accountIds.isEmpty()) return null
        val url = Endpoints.getPrimaryContactsForAccounts(baseURI) + "?\$select={0}&\$filter={1}"
        val selectKeys = """emailaddress1,yomifullname,contactid,mobilephone,_parentcustomerid_value"""
        val filterQuery = accountIds.joinToString(" or ") { """_parentcustomerid_value eq '$it'""" }
        return client
                .get()
                .uri(url, selectKeys, filterQuery)
                .header(AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error While Fetching Primary Contact For AccountIds:${accountIds.serialize()}" }
                }
                .awaitBody<Map<String, Any>>()
                .getListOrDefault<Map<String, Any>>("value")
                .let {
                    JsonParser.convertValue<List<Contact>>(it)
                }
                .associateBy { it.accountId }
    }

    /**
     * create task for given account
     *
     * @param accountId: id of the given account
     * @param task: CreateTaskRequest: task request with subject, description & dueDate
     * @param baseURI is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @return Unit
     */
    suspend fun createTaskForAccount(
            accountId: String,
            task: CreateTaskRequest,
            baseURI: String,
            authorization: String
    ): Boolean {
        val userId = getUserId(baseURI, authorization)
        val dueDate = task.dueDate ?: getCurrentDateRoundedBy30Mins()
        val params = mapOf(
                "subject" to task.comments,
                "description" to task.description,
                "scheduledend" to formatDate(dueDate, DATE_FORMAT_PATTERN),
                "ownerid@odata.bind" to """/systemusers($userId)""",
                "regardingobjectid_account_task@odata.bind" to """/accounts($accountId)"""
        )
                .filterValues { !it.isNullOrEmpty() }
        return client
                .post()
                .uri(Endpoints.getTasksUrl(baseURI))
                .header(AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(params)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error while creating task for account -> $accountId with request-> ${JsonParser.serialize(task)}" }
                }
                .statusCode()
                .is2xxSuccessful
    }

    /**
     * create appointment for given account
     *
     * @param accountId: id of the given account
     * @param task: CreateAppointmentRequest: task request with subject, startTime & endTime
     * @param baseURI is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @return Unit
     */
    suspend fun createAppointmentForAccount(
            accountId: String,
            task: CreateAppointmentRequest,
            baseURI: String,
            authorization: String
    ): Boolean {
        val userId = getUserId(baseURI, authorization)
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
        val startDate = task.startTime ?: getCurrentDateRoundedBy30Mins()
        val endDate = task.endTime ?: startDate.plusMinutes(30)
        val params = mapOf(
                "subject" to task.comments,
                "scheduledstart" to formatDate(startDate, DATE_FORMAT_PATTERN),
                "scheduledend" to formatDate(endDate, DATE_FORMAT_PATTERN),
                "ownerid@odata.bind" to userResource,
                "regardingobjectid_account_appointment@odata.bind" to accountResource,
                "appointment_activity_parties" to parties
        )
        return client
                .post()
                .uri(Endpoints.getAppointmentsUrl(baseURI))
                .header(AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(params)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error while creating appointment for account -> $accountId with request-> ${JsonParser.serialize(task)}" }
                }
                .statusCode()
                .is2xxSuccessful
    }

    /**
     * create phone call for given account
     *
     * @param accountId: id of the given account
     * @param task: CreatePhoneCallRequest: task request with subject, description, phoneNumber & dueDate
     * @param baseURI is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @return Unit
     */
    suspend fun createPhoneCallForAccount(
            accountId: String,
            task: CreatePhoneCallRequest,
            baseURI: String,
            authorization: String
    ): Boolean {
        val userId = getUserId(baseURI, authorization)
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
        val dueDate = task.dueDate ?: getCurrentDateRoundedBy30Mins()
        val params = mapOf(
                "subject" to task.comments,
                "description" to task.description,
                "phonenumber" to task.phoneNo,
                "scheduledend" to formatDate(dueDate, DATE_FORMAT_PATTERN),
                "ownerid@odata.bind" to userResource,
                "regardingobjectid_account_phonecall@odata.bind" to accountResource,
                "phonecall_activity_parties" to parties
        )
                .filterValues { it != null }
        return client
                .post()
                .uri(Endpoints.getPhoneCallsUrl(baseURI))
                .header(AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(params)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error while creating phone call for account -> $accountId with request-> ${JsonParser.serialize(task)}" }
                }
                .statusCode()
                .is2xxSuccessful
    }
}
