/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.ms.graph.service

import com.vmware.connectors.ms.graph.config.Endpoints
import com.vmware.connectors.ms.graph.dto.AccessRequest
import com.vmware.connectors.ms.graph.dto.EmailAddress
import com.vmware.connectors.ms.graph.dto.OutlookMailItem
import com.vmware.connectors.ms.graph.dto.User
import com.vmware.connectors.ms.graph.utils.*
import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Mono
import java.text.MessageFormat
import java.time.LocalDateTime

/**
 * MSGraphService Class contains the functions necessary to fetch the pendingAccessRequests Data
 * and the following Actions(Approve,Reject) to be performed on the AccessRequest
 * and every function in the class requires the following parameters
 * baseUrl - tenant url
 * authorization - Microsoft Teams User Bearer Token
 *
 * @property client WebClient: library to make async http calls
 */
@Component
class MSGraphService(
        @Autowired private val client: WebClient,
        @Value("classpath:templates/Access-Request-Approval-Email-To-Requester.html")
        private val accessRequestApprovalEmailToRequesterTemplateResource: Resource,
        @Value("classpath:templates/Access-Request-Decline-Email-To-Requester.html")
        private val accessRequestDeclineEmailToRequesterTemplateResource: Resource
) {
    private val logger = getLogger()

    private val accessRequestApprovalEmailToRequesterTemplate = accessRequestApprovalEmailToRequesterTemplateResource.readAsString()
    private val accessRequestDeclineEmailToRequesterTemplate = accessRequestDeclineEmailToRequesterTemplateResource.readAsString()

    private val accessRequestMailSubjectRegex = Regex("(.*) wants to share (.*) with (.*)")
    private val accessRequestMailSubjectForGroupRegex = Regex("(.*) wants to share (.*) with (.*) Members")
    private val directAccessRequestMailSubjectRegex = Regex("(.*) wants to access (.*)")


    /**
     *  fetches the List of RequestAccessMails from the User's Account
     *
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called
     * @param dateAfter LocalDateTime Object
     * @returns List of OutlookItem objects
     */
    private suspend fun getRequestAccessMails(
            authorization: String,
            baseUrl: String,
            dateAfter: LocalDateTime
    ): List<OutlookMailItem> {
        val dateCursor = formatDate(dateAfter)
        val filterQry = "ReceivedDateTime ge $dateCursor and (from/emailAddress/address) eq 'no-reply@sharepointonline.com'"
        val url = Endpoints.getAccessRequestMailsUrl(baseUrl, filterQry)

        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchange()
                .awaitBody<Map<String, Any>>()
                .getListOrNull<Map<String, Any>>("value")
                ?.convertValue()
                ?: emptyList()
    }

    /**
     *  fetches the fileId of the Resource
     *
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called
     * @param siteId siteID of the Resource
     * @param filePath is the Path of the SharedFile
     * @returns the File Id of the Resource
     */
    private suspend fun getFileIdFromFilePath(
            authorization: String,
            baseUrl: String,
            siteId: String,
            filePath: String
    ): String? {
        logger.info { "fetching file info for $filePath for site $siteId" }
        val url = Endpoints.getFileIdFromFilePathUrl(baseUrl, siteId, filePath)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error while fetching fileid from file path $siteId $filePath" }
                }
                .awaitBody<Map<String, Any>>()
                .getStringOrNull("id")
    }

    /**
     * fetches the SiteId of the Resource
     *
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called
     * @param siteHostName is HostName of the Site
     * @param sitePath is the Path of the Site
     * @returns the Site Id of the Resource
     */
    private suspend fun getSiteIdFromSitePath(
            authorization: String,
            baseUrl: String,
            siteHostName: String,
            sitePath: String
    ): String? {
        val url = Endpoints.getSiteIdCallUrl(baseUrl, siteHostName, sitePath)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchange()
                .awaitBody<Map<String, Any>>()
                .getStringOrNull("id")
    }

    /**
     * declines the Access Request to the SharedFile and
     * will respond to the Requested User
     *
     *  @param authorization is the token needed for authorizing the call
     *  @param baseUrl is the endPoint to be called
     *  @param accessRequest AccessRequest
     *  @param comments comments while declining the Request
     */
    suspend fun declinePermissionToResource(
            authorization: String,
            baseUrl: String,
            accessRequest: AccessRequest,
            comments: String?
    ) {

        if (accessRequest.requestedBy != null) {

            val to = listOf(accessRequest.requestedBy.toEmailAddress())

            val content = MessageFormat.format(accessRequestDeclineEmailToRequesterTemplate, accessRequest.resource.name)

            val body = mapOf(
                    "contentType" to "HTML",
                    "content" to content
            )
            logger.info { "comments -> $comments" }
            val subject = """Administrator has responded to your request for '${accessRequest.resource.name}'"""

            sendMail(authorization, baseUrl, to, subject = subject, body = body)
        }
    }

    /**
     * Accepts the Access Request to the SharedFile and
     *  will respond to the Requested User
     *
     *  @param authorization is the token needed for authorizing the call
     *  @param baseUrl is the endPoint to be called
     *  @param accessRequest AccessRequest
     *  @param roles List of roles that user is allowing on Shared File/Folder
     *  @param comments comments while declining the Request
     *  @returns the Map
     */
    suspend fun addPermissionToFile(
            authorization: String,
            baseUrl: String,
            accessRequest: AccessRequest,
            roles: List<String>,
            comments: String?
    ): Map<String, Any> {

        val url = Endpoints.inviteUrl(baseUrl, accessRequest.resource.siteId, accessRequest.resource.id)

        val recipients = accessRequest.requestedFor
                .map {
                    mapOf("email" to it.emailId)
                }
        val params = mapOf(
                "recipients" to recipients,
                "message" to (comments ?: "Here's the file that we're collaborating on"),
                "requireSignIn" to true,
                "sendInvitation" to true,
                "roles" to roles
        )

        val resp = client
                .post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(params))
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error while adding permission for accessRequest: $accessRequest" }
                }
                .awaitBody<Map<String, Any>>()

        if (accessRequest.requestedBy != null) {
            val to = listOf(accessRequest.requestedBy.toEmailAddress())

            val postfix = if (accessRequest.sharedForGroup) " Members" else ""

            val requestedForNames = accessRequest.requestedFor
                    .joinToString(", ", postfix = postfix) {
                        it.name
                    }

            val content = MessageFormat.format(
                    accessRequestApprovalEmailToRequesterTemplate,
                    requestedForNames,
                    accessRequest.resource.name,
                    accessRequest.resource.url
            )

            val body = mapOf(
                    "contentType" to "HTML",
                    "content" to content
            )
            val subject = """Administrator has responded to your request for '${accessRequest.resource.name}'"""

            sendMail(
                    authorization,
                    baseUrl,
                    to,
                    subject = subject,
                    body = body
            )
        }
        return resp
    }

    /**
     * fetches the relevant people for the User
     *
     * @param authorization is the token needed for authorizing the call
     *  @param baseUrl is the endPoint to be called
     *  @param searchName displayName to be Searched
     *  @returns the Map containing RelevantPeople for the User
     */
    private suspend fun getRelevantPeopleForUser(
            authorization: String,
            baseUrl: String,
            searchName: String
    ): Map<String, Any>? {
        val url = Endpoints.getRelevantPeopleWithNameUrl(baseUrl, searchName)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchange()
                .awaitBodyOrNull()
    }

    /**
     * fetches the hostName,sitePath,filePath of the Resource
     * @param url is the URL Resource
     * @returns the Triple<hostName,sitePath,filePath>
     */
    private fun getSharePointParams(url: String): Triple<String, String, String>? {
        val regex = Regex(pattern = "https://(.+.sharepoint.com)(/[^/.]+/[^/.]+)/[^/.]+(/.+)")
        val matches = regex.matchEntire(url)?.groupValues
        return if (matches != null) Triple(matches[1], matches[2], matches[3]) else null
    }

    /**
     * fetches the CorrectPerson Among the RelevantPeople
     *
     * @param matchedPeople are relevant People
     * @param searchName is the displayName of the user
     * @returns the correctPerson among Relevant People
     */
    private fun findCorrectPersonAmongRelevants(
            matchedPeople: Map<String, Any>?,
            searchName: String
    ): String? {
        return matchedPeople
                ?.getListOrNull<Map<String, Any>>("value")
                ?.firstOrNull {
                    it.getStringOrDefault("displayName") == searchName || it.getStringOrDefault("givenName") == searchName
                }
                ?.getListOrNull<Map<String, Any>>("emailAddresses")
                ?.firstOrNull()
                ?.getStringOrNull("address")
    }

    /**
     * fetches the CorrectPerson Among the knownUsers
     *
     * @param users are user's domain people
     * @param searchName is the displayName of the user
     * @returns the correctPerson among Domain's People
     */

    private fun findCorrectPersonAmongUsers(
            users: Map<String, Any>,
            searchName: String
    ): String? {
        return users
                .getListOrNull<Map<String, Any>>("value")
                ?.firstOrNull {
                    it.getStringOrDefault("displayName") == searchName || it.getStringOrDefault("givenName") == searchName
                }
                ?.getStringOrNull("mail")
    }

    /**
     * fetches the user's TimeZone Or
     * Null if the user did not set TimeZone.
     *
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called
     * @returns the tomeZone of the User
     */
    suspend fun getUserTimeZone(
            authorization: String,
            baseUrl: String
    ): String? {
        val url = Endpoints.getUserTimeZoneUrl(baseUrl)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "error while getting user's timeZone -> $url" }
                }
                .awaitBody<Map<String, Any>>()
                .getStringOrNull("value")
    }

    /**
     * fetches the AccessRequestData from The User's Mails.
     *
     * @param authorization is the token required for authorizing the call
     * @param baseUrl is the endPoint to be called
     * @param emails is List of OutLookMailItem Objects
     * @param timeZone is timeZone of the User
     * @returns the List of AccessRequest Object
     */
    private suspend fun getAccessRequestDataFromMails(
            authorization: String,
            baseUrl: String,
            emails: List<OutlookMailItem>,
            timeZone: String
    ): List<AccessRequest> {
        val requestAccessMails = emails.filter { email ->
            logger.info { "Regex matched -> ${accessRequestMailSubjectRegex.matches(email.subject)}" }
            accessRequestMailSubjectRegex.matches(email.subject) ||
                    directAccessRequestMailSubjectRegex.matches(email.subject)
        }
        logger.info { "rejected mails by subject-> ${emails.minus(requestAccessMails).map { it.id + it.subject}}" }
        return requestAccessMails
                .mapNotNull { email ->
                    try {
                        val body = email.content

                        logger.info { "email body is null -> ${body == null}" }

                        body?.let {
                            val doc = Jsoup.parse(body)
                            // Get File Info
                            val entireFileUrl = doc.select("a[href]").map { it.attr("href") }.last()
                            val fileUrl = getUrlWithoutParameters(entireFileUrl)
                            val siteParamsQry =  getSharePointParams(fileUrl)
                            val siteParams = if (siteParamsQry != null) {
                                siteParamsQry
                            } else {
                                val qParams = getUrlQueryParameters(entireFileUrl)
                                val fUrl = qParams?.getStringOrNull("url")
                                fUrl?.let { getSharePointParams(it) }
                            }
                            logger.info { "siteParams-> $siteParams entireFileUrl -> $entireFileUrl fileUrl-> $fileUrl" }

                            siteParams?.let { params ->
                                val siteHostName = params.first
                                val sitePath = params.second.urlDecode()
                                val filePath = params.third.urlDecode()
                                logger.info { "entireFileUrl-> $entireFileUrl fileUrl-> $fileUrl filePath-> $filePath" }

                                val siteId = getSiteIdFromSitePath(authorization, baseUrl, siteHostName, sitePath)
                                logger.info { "siteId-> $siteId" }

                                siteId?.let {
                                    val fileId = getFileIdFromFilePath(authorization, baseUrl, it, filePath)
                                    val fileName = filePath.split("/").last()
                                    //Get requestedFor User Info

                                    val requestedForGroup = accessRequestMailSubjectForGroupRegex.matches(email.subject)
                                    val directlyRequested = directAccessRequestMailSubjectRegex.matches(email.subject)

                                    val requestedForName = when {
                                        directlyRequested -> email.replyTo.firstOrNull()?.name
                                        requestedForGroup -> accessRequestMailSubjectForGroupRegex
                                                .matchEntire(email.subject)
                                                ?.groupValues
                                                ?.lastOrNull()
                                        else -> accessRequestMailSubjectRegex
                                                .matchEntire(email.subject)
                                                ?.groupValues
                                                ?.lastOrNull()
                                    }
                                    logger.info { "fileId-> $fileId requestedForName-> $requestedForName" }

                                    val requestedForEmail = requestedForName?.let {
                                        if (directlyRequested) {
                                            email.replyTo.firstOrNull()?.address
                                        } else {
                                            val emailReg = Regex("$requestedForName \\((.*?)\\)")
                                            val requestedFor = emailReg.find(body)?.groupValues?.get(1)
                                            if (requestedFor == null) {
                                                val matchedPeople = getRelevantPeopleForUser(authorization, baseUrl, requestedForName)
                                                logger.info {"matchedPeople -> $matchedPeople" }
                                                findCorrectPersonAmongRelevants(matchedPeople, requestedForName)
                                                        ?: findCorrectPersonAmongUsers(
                                                                getAllUsers(authorization, baseUrl),
                                                                requestedForName
                                                        )
                                            } else requestedFor
                                        }
                                    }
                                    // Get RequestedBy UserInfo
                                    val requestedByName = email.replyTo.firstOrNull()?.name
                                    val requestedByEmail = email.replyTo.firstOrNull()?.address

                                    logger.info { "fileId-> $fileId requestedBy-> $requestedByEmail requestedFor-> $requestedForEmail" }
                                    // Return AccessRequest Card Data

                                    if (fileId == null || requestedForEmail == null) {

                                        logger.warn { "************ Ignoring mail as insufficient data for mail-> ${email.id} *********" }
                                        return@mapNotNull null
                                    }

                                    AccessRequest(
                                            id = email.id,
                                            requestedBy = if (requestedByEmail != null && requestedByName != null) {
                                                User(emailId = requestedByEmail, name = requestedByName)
                                            } else null,
                                            requestedFor = listOf(User(emailId = requestedForEmail, name = requestedForName)),
                                            resource = com.vmware.connectors.ms.graph.dto.Resource(
                                                    id = fileId,
                                                    path = filePath,
                                                    name = fileName,
                                                    url = entireFileUrl,
                                                    siteId = siteId
                                            ),
                                            sharedForGroup = requestedForGroup,
                                            receivedDate = parseDate(email.receivedDateTime, timeZone)
                                    )
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        ex.message?.let {
                            if (it.contains("401 Unauthorized")) {
                                throw ex
                            }
                        }
                        logger.info { "email rejected due to insufficient data -. ${email.subject}" }
                        null
                    }
                }
    }

    /**
     * fetches the PendingAccessRequests from mail since given time
     *
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called
     * @param minutes is number od hours to get the AccessRequests
     * @returns the List of AccessRequest Objects
     */
    suspend fun getPendingAccessRequestsSinceMinutes(
            authorization: String,
            baseUrl: String,
            minutes: Long
    ): List<AccessRequest> {
        val since = getDateTimeMinusMinutes(minutes)
        val mails = getRequestAccessMails(authorization, baseUrl, since)
        val timeZone = getUserTimeZone(authorization, baseUrl) ?: "UTC"
        return getAccessRequestDataFromMails(authorization, baseUrl, mails, timeZone)
    }

    /**
     * Send an email to from the authorized user mail box
     * @param authorization token needed for authorizing the call
     * @param baseUrl is the endPoint to be called
     * @param to to: recipients for the message
     * @param cc cc: recipients for the message
     * @param replyTo The email addresses to use when replying
     * @param body is body of the message(can be text or html)
     * @param subject is the subject of the message
     * @param saveToSentItems whether to save in sentItems or not
     */
    private suspend fun sendMail(
            authorization: String,
            baseUrl: String,
            to: List<EmailAddress>,
            cc: List<EmailAddress> = emptyList(),
            replyTo: List<EmailAddress> = emptyList(),
            body: Map<String, Any>,
            subject: String,
            saveToSentItems: Boolean = false
    ) {

        val mailItem = mapOf(
                "message" to mapOf(
                        "subject" to subject,
                        "body" to body,
                        "toRecipients" to to.map { mapOf("emailAddress" to it.emailAddress) },
                        "ccRecipients" to cc.map { mapOf("emailAddress" to it.emailAddress) },
                        "replyTo" to replyTo.map { mapOf("emailAddress" to it.emailAddress) }
                ),
                "saveToSentItems" to saveToSentItems
        )

        val status = client
                .post()
                .uri(Endpoints.sendMailUrl(baseUrl))
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(mailItem))
                .awaitExchangeAndThrowError {
                    logger.error(it) { "error while sending mail" }
                }
                .statusCode()
                .is2xxSuccessful

        logger.info { "Mail Sent resp status-> $status" }
    }

    /**
     * fetches All the Users in the user Domain
     *
     * @param authorization token needed for authorizing the call
     * @param baseUrl is the endPoint to be Called
     * @returns the user's Information
     */
    private suspend fun getAllUsers(authorization: String, baseUrl: String): Map<String, Any> {
        val url = Endpoints.getAllUsers(baseUrl)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchange()
                .awaitBody()
    }
}
