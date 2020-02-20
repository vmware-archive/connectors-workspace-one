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
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.awaitExchange
import java.text.MessageFormat
import java.time.LocalDateTime

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
     * this Function will return List of RequestAccessMails from the User's Account
     *
     * @param baseUrl is the endPoint to be called
     * @param dateAfter LocalDateTime Object
     * @param authorization is the token needed for authorizing the call
     */
    private suspend fun  getRequestAccessMails(
            baseUrl: String,
            dateAfter: LocalDateTime,
            authorization: String
    ): List<OutlookMailItem> {
        val dateCursor = formatDate(dateAfter)
        val filterQry = "ReceivedDateTime ge $dateCursor and (from/emailAddress/address) eq 'no-reply@sharepointonline.com'"
        val url = Endpoints.getAccessRequestMailsUrl(baseUrl, filterQry)

        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error while fetching mails for user" }
                }
                .awaitBodyOrNull<Map<String, Any>>()
                ?.getListOrNull<Map<String, Any>>("value")
                ?.convertValue()
                ?: emptyList()
    }

    /**
     * this Function will take filePath and sitePath and returns the fileId
     *
     * @param baseUrl is the endPoint to be called
     * @param siteId siteID of the Resource
     * @param filePath is the Path of the SharedFile
     * @param authorization is the token needed for authorizing the call
     */
    private suspend fun getFileIdFromFilePath(
            baseUrl: String,
            siteId: String,
            filePath: String,
            authorization: String
    ): String? {
        logger.info { "fetching file info for $filePath for site $siteId" }
        val url = Endpoints.getFileIdFromFilePathUrl(baseUrl, siteId, filePath)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchange()
                .also {
                    if (it.statusCode().is4xxClientError || it.statusCode().is5xxServerError) {
                        logger.error { "Error while fetching fileid from file path $siteId $filePath" }
                    }
                }
                .awaitBodyOrNull<Map<String, Any>>()
                ?.getStringOrNull("id")
    }

    /**
     * this Function will take siteHostName and sitePath and
     *  returns the Resource's SiteId
     *
     * @param baseUrl is the endPoint to be called
     * @param siteHostName is HostName of the Site
     * @param sitePath is the Path of the Site
     * @param authorization is the token needed for authorizing the call
     */
    private suspend fun getSiteIdFromSitePath(
            baseUrl: String,
            siteHostName: String,
            sitePath: String,
            authorization: String
    ): String? {
        val url = Endpoints.getSiteIdCallUrl(baseUrl, siteHostName, sitePath)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchange()
                .awaitBodyOrNull<Map<String, Any>>()
                ?.getStringOrNull("id")
    }

    /**
     * this function will decline the Request to Access the SharedFile and
     *  will send the Email to the Requested User
     *
     *  @param baseUrl is the endPoint to be called
     *  @param accessRequest AccessRequest
     *  @param comments comments while declining the Request
     *  @param authorization is the token needed for authorizing the call
     */
    suspend fun declinePermissionToResource(
            baseUrl: String,
            accessRequest: AccessRequest,
            comments: String?,
            authorization: String
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

            sendMail(baseUrl, authorization, to, subject = subject, body = body)
        }
    }

    /**
     * this function will Accept the Request to Access the SharedFile and
     *  will send the Email to the Requested User
     *
     *  @param baseUrl is the endPoint to be called
     *  @param accessRequest AccessRequest
     *  @param roles List of roles that user is allowing on Shared File/Folder
     *  @param comments comments while declining the Request
     *  @param authorization is the token needed for authorizing the call
     *  @returns the Map
     */
    suspend fun addPermissionToFile(
            baseUrl: String,
            accessRequest: AccessRequest,
            roles: List<String>,
            comments: String?,
            authorization: String
    ): Map<String, Any>? {

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
                .syncBody(params)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error while adding permission for accessRequest: $accessRequest" }
                }
                .awaitBodyOrNull<Map<String, Any>>()

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
                    baseUrl,
                    authorization,
                    to,
                    subject = subject,
                    body = body
            )
        }
        return resp
    }

    /**
     * this function will returns the relevant people for the User
     *
     *  @param baseUrl is the endPoint to be called
     *  @param searchName displayName to be Searched
     *  @param authorization is the token needed for authorizing the call
     *  @returns the Map containing RelevantPeople for the User
     */
    private suspend fun getRelevantPeopleForUser(
            baseUrl: String,
            searchName: String,
            authorization: String
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
     * this function will take an url and returns the hostName,sitePath,filePath
     * @param url is the URL Resource
     * @returns the Triple<hostName,sitePath,filePath>
     */
    private fun getSharepointParams(url: String): Triple<String, String, String>? {
        val regex = Regex(pattern = "https://(.+.sharepoint.com)(/[^/.]+/[^/.]+)/[^/.]+(/.+)")
        val matches = regex.matchEntire(url)?.groupValues
        return if (matches != null) Triple(matches[1], matches[2], matches[3]) else null
    }

    /**
     * this function will return the CorrectPerson Among the RelevantPeople
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
     * this function will return the user's TimeZone
     *
     * @param authorization is the token needed for authorizing the call
     * @param baseUrl is the endPoint to be called
     * @returns the tomeZone of the User
     */
    suspend fun getUserTimeZone(
            baseUrl: String,
            authorization: String
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
     * this Function will get AccessRequestData froM THE User's Mails.
     *
     * @param baseUrl is the endPoint to be called
     * @param emails is List of OutLookMailItem Objects
     * @param authorization is the token required for authorizing the call
     * @param timeZone is timeZone of the User
     * @returns the List of AccessRequest Object
     */
    private suspend fun getAccessRequestDataFromMails(
            baseUrl: String,
            emails: List<OutlookMailItem>,
            authorization: String,
            timeZone: String
    ): List<AccessRequest> {
        val requestAccessMails = emails.filter { email ->
            accessRequestMailSubjectRegex.matches(email.subject)
        }
        logger.info { "rejected mails by subject-> ${emails.minus(requestAccessMails).map { it.id }}" }
        return requestAccessMails
                .mapNotNull { email ->
                    val body = email.content

                    logger.info { "email body is null -> ${body.isNullOrEmpty()}" }

                    body?.let {
                        val doc = Jsoup.parse(body)
                        // Get File Info
                        val entireFileUrl = doc.select("a[href]").map { it.attr("href") }.last()
                        val fileUrl = getUrlWithoutParameters(entireFileUrl)
                        val siteParams = getSharepointParams(fileUrl)
                        siteParams?.let { params ->
                            val siteHostName = params.first
                            val sitePath = params.second.urlDecode()
                            val filePath = params.third.urlDecode()
                            logger.info { "entireFileUrl-> $entireFileUrl fileUrl-> $fileUrl filePath-> $filePath" }

                            val siteId = getSiteIdFromSitePath(baseUrl, siteHostName, sitePath, authorization)

                            siteId?.let {
                                val fileId = getFileIdFromFilePath(baseUrl, it, filePath, authorization)
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
                                val requestedForEmail = requestedForName?.let {
                                    if (directlyRequested) {
                                        email.replyTo.firstOrNull()?.address
                                    } else {
                                        val matchedPeople = getRelevantPeopleForUser(baseUrl, requestedForName, authorization)
                                        findCorrectPersonAmongRelevants(matchedPeople, requestedForName)
                                                ?: findCorrectPersonAmongUsers(
                                                        getAllUsers(baseUrl, authorization),
                                                        requestedForName
                                                )
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

                                logger.info { "Creating AR obj with file-> $fileId" }


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
                }
    }

    /**
     * this function will get the PendingAccessRequests from mail since given time
     *
     * @param baseUrl is the endPoint to be called
     * @param authorization is the token needed for authorizing the call
     * @param minutes is number od hours to get the AccessRequests
     * @returns the List of AccessRequest Objects
     */
    suspend fun getPendingAccessRequestsSinceMinutes(
            baseUrl: String,
            authorization: String,
            minutes: Long
    ): List<AccessRequest> {
        val since = getDateTimeMinusMinutes(minutes)
        val mails = getRequestAccessMails(baseUrl, since, authorization)
        val timeZone = getUserTimeZone(baseUrl, authorization) ?: "UTC"
        return getAccessRequestDataFromMails(baseUrl, mails, authorization, timeZone)
    }

    /**
     * Send an email to from the authorized user mail box
     * @param authorization : token needed for authorizing the call
     * all params required for an email
     */
    private suspend fun sendMail(
            baseUrl: String,
            authorization: String,
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
                .syncBody(mailItem)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "error while sending mail" }
                }
                .statusCode()

        logger.info { "Mail Sent resp status-> $status" }
    }

    /**
     * this Function will get All the Users in the user Domain
     *
     * @param baseUrl is the endPoint to be Called
     * @param authorization token needed for authorizing the call
     * @returns the user's Information
     */
    private suspend fun getAllUsers(baseUrl: String, authorization: String): Map<String, Any> {
        val url = Endpoints.getAllUsers(baseUrl)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchange()
                .awaitBody()
    }

    /**
     * gets the emaiId from the Vmware user token
     */
    fun getUserEmailFromToken(token: String?): String? {
        val pl = token
                ?.split(" ")
                ?.lastOrNull()
                ?.split(".")
                ?.get(1)
                ?.toBase64DecodedString()

        return pl?.let { JsonParser.deserialize(it) }
                ?.getStringOrNull("eml")
    }
}
