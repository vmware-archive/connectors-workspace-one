/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.googleDocs.service

import com.backflipt.commons.*
import com.vmware.connectors.googleDocs.config.DATE_FORMAT_PATTERN
import com.vmware.connectors.googleDocs.config.Endpoints
import com.vmware.connectors.googleDocs.config.IGNORE_DOC_STATUS
import com.vmware.connectors.googleDocs.dto.Comment
import com.vmware.connectors.googleDocs.dto.GoogleDocData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

/**
 * BackendService class for Google Docs contains functions which will fetch the mentioned Comments
 * In the users Google Documents and the following Actions(Invite User, Reply To Comment , Append Message to Document)
 * and every function in the class requires the following parameters
 * baseUrl - tenant url
 * authorization - Google Docs User Bearer Token
 *
 * @property client: WebClient: library to make async http calls
 */
@Component
class BackendService(@Autowired private val client: WebClient) {
    private val logger = getLogger()

    /**
     * fetches the updated Google Documents.
     *
     * @param baseUrl is the endPoint to be called.
     * @param authorization is the token needed for authorizing the call.
     * @param sinceMinutes is the lookUpWindow to search for Updated Documents
     * @returns the list of [GoogleDocData] Object.
     */
    private suspend fun getUpdatedDocuments(
            baseUrl: String,
            authorization: String,
            sinceMinutes: Int
    ): List<GoogleDocData> {
        val docLookUpWindow = sinceMinutes * 60 * 1000
        val query = "mimeType='application/vnd.google-apps.document'"
        val filter = "items(id,modifiedDate,modifiedByMeDate)"
        val url = Endpoints.getGoogleDocumentsUrl(baseUrl, query, filter)
        val request = client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error While Fetching Documents With Url --> $url" }
                }
                .awaitBody<Map<String, Any>>()
        val documents = request
                .getListOrException<Map<String, Any>>("items")
        val lookUpWindow = System.currentTimeMillis().minus(docLookUpWindow)
        val minusTimeStamp = getCurrentUtcTimeUsingEpoch(lookUpWindow, DATE_FORMAT_PATTERN)
        return JsonParser.convertValue<List<GoogleDocData>>(documents)
                .filter {
                    it.modifiedDate > minusTimeStamp
                }
    }

    /**
     * adds the permission to access the Document to the given MailId.
     *
     * @param baseUrl is the endPoint to be called.
     * @param authorization is the token needed for authorizing the call.
     * @param docId is the Id Of the Document to be shared.
     * @param emailId is the Mail Id to be added.
     * @returns the status of Adding User.
     */
    suspend fun inviteUserToDocument(
            baseUrl: String,
            authorization: String,
            docId: String,
            emailId: String
    ): Boolean {
        val url = Endpoints.inviteUserToDocumentUrl(baseUrl, docId)
        val postCommentBody = mapOf(
                "role" to "writer",
                "type" to "user",
                "value" to emailId
        )
        return client
                .post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(postCommentBody)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error While Inviting User With url : $url" }
                }
                .statusCode()
                .is2xxSuccessful
    }

    /**
     * list the userNames Having Permissions(edit,view) to the Document.
     *
     * @param baseUrl is the endPoint to be called.
     * @param authorization is the token needed for authorizing the call.
     * @param docId is the Id Of the Document.
     * @returns the list of userNames.
     */
    private suspend fun listDocumentCollaborators(
            baseUrl: String,
            authorization: String,
            docId: String
    ): List<String> {
        val url = Endpoints.listDocumentUsersUrl(baseUrl, docId)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError()
                .awaitBody<Map<String, Any>>()
                .getListOrException<Map<String, Any>>("items")
                .filter {
                    it.getStringOrException("id") != "anyoneWithLink"
                }
                .map {
                    val name = it.getStringOrDefault("name")
                    if (name.isEmpty()) it.getStringOrException("emailAddress") else name
                }
    }

    /**
     * fetches the current user mail Address.
     *
     * @param baseUrl is the endPoint to be called.
     * @param authorization is the token needed for authorizing the call.
     * @returns the current user's mail Address.
     */
    private suspend fun getCurrentUserMailFromToken(baseUrl: String, authorization: String): String {
        val filter = "name,user(emailAddress)"
        val url = Endpoints.getCurrentUserMailUrl(baseUrl, filter)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError()
                .awaitBody<Map<String, Any>>()
                .getMapOrException<String, Any>("user")
                .getStringOrException("emailAddress")
    }

    /**
     * fetches the Document Comments.
     *
     * @param baseUrl is the endPoint to be called.
     * @param authorization is the token needed for authorizing the call.
     * @param docId is the Id Of the Document to be shared.
     * @param sinceMinutes is the lookUpWindow to search for Updated Comments.
     * @returns the List Of Comments MetaData.
     */
    private suspend fun listCommentsAndRepliesForDocument(
            baseUrl: String,
            authorization: String,
            docId: String,
            sinceMinutes: Int
    ): List<Map<String, Any>> {
        val docLookUpWindow = sinceMinutes * 60 * 1000
        val filter = "items(commentId,modifiedDate,author(displayName),content,status,fileTitle" +
                ",replies(replyId,content,modifiedDate,author(displayName),verb))"
        val url = Endpoints.listCommentsAndRepliesUrl(baseUrl, docId, filter)
        val messagesWithReplies = client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError()
                .awaitBody<Map<String, Any>>()
                .getListOrException<Map<String, Any>>("items")
                .filter {
                    val lookUpWindow = System.currentTimeMillis().minus(docLookUpWindow)
                    val minusTimeStamp = getCurrentUtcTimeUsingEpoch(lookUpWindow, DATE_FORMAT_PATTERN)
                    it.getStringOrException("status") != IGNORE_DOC_STATUS &&
                            it.getStringOrException("modifiedDate") > minusTimeStamp
                }.map {
                    val commentId = it.getStringOrException("commentId")
                    it.plus("uniqueId" to commentId)
                }
        val replies = messagesWithReplies
                .flatMap { item ->
                    val commentId = item.getStringOrException("commentId")
                    val title = item.getStringOrException("fileTitle")
                    item.getListOrException<Map<String, Any>>("replies").map {
                        val replyId = it.getStringOrException("replyId")
                        it.plus(
                                mapOf("commentId" to commentId, "fileTitle" to title, "docId" to docId, "uniqueId" to replyId))
                    }
                }
        val currentUserMessagesAndReplies = filterDuplicateComments(messagesWithReplies.plus(replies), docLookUpWindow)
        return currentUserMessagesAndReplies.filter {
            val currentUser = getCurrentUserMailFromToken(baseUrl, authorization)
            it.getStringOrException("content").contains(currentUser)
        }.map {
            val users = listDocumentCollaborators(baseUrl, authorization, docId)
            it.plus(mapOf("users" to users, "docId" to docId))
        }
    }

    /**
     * fetches the Mentioned Comments Of the Current User.
     *
     * @param baseUrl is the endPoint to be called.
     * @param authorization is the token needed for authorizing the call.
     * @param sinceMinutes is the lookUpWindow to search for Updated Comments
     * @returns the List Of [Comment] Object
     */
    suspend fun getMentionedComments(baseUrl: String, authorization: String, sinceMinutes: Int): List<Comment> {
        val docIds = getUpdatedDocuments(baseUrl, authorization, sinceMinutes).map { it.id }
        val mentionedComments = docIds.fold(emptyList<Map<String, Any>>()) { acc, docId ->
            acc.plus(listCommentsAndRepliesForDocument(baseUrl, authorization, docId, sinceMinutes))
        }
        return JsonParser.convertValue(mentionedComments)
    }

    /**
     * filters the Duplicate Comments While Fetching Comment Replies.
     *
     * @param comments is the user's comments in All Documents
     * @param docLookUpWindow is the lookUpWindow to search for Mentioned Comments
     * @returns the List Of Comments MetaData
     */
    private fun filterDuplicateComments(
            comments: List<Map<String, Any>>,
            docLookUpWindow: Int
    ): List<Map<String, Any>> {
        return if (comments.isEmpty()) emptyList()
        else {
            comments.filter {
                val lookUpWindow = System.currentTimeMillis().minus(docLookUpWindow)
                val minusTimeStamp = getCurrentUtcTimeUsingEpoch(lookUpWindow, DATE_FORMAT_PATTERN)
                val repliesList = it.getListOrDefault<Map<String, Any>>("replies")
                repliesList.lastOrNull()?.getStringOrException("modifiedDate") != it.getStringOrException("modifiedDate") &&
                        it.getStringOrException("modifiedDate") > minusTimeStamp
            }
        }
    }

    /**
     * will reply to the mentioned Comment.
     *
     * @param baseUrl is the endPoint to be called.
     * @param authorization is the token needed for authorizing the call.
     * @param docId is the Id Of the Document to be shared.
     * @param commentId is the Id Of the Mentioned Comment
     * @param message is the content to reply.
     * @returns the status of Replying.
     */
    suspend fun replyToMessage(
            baseUrl: String,
            authorization: String,
            docId: String,
            commentId: String,
            message: String
    ): Boolean {
        val filter = "*"
        val url = Endpoints.replyToMessageUrl(baseUrl, docId, commentId, filter)
        val replyBody = mapOf("content" to message)
        return client
                .post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(replyBody)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error While Replying to the Message With Url: $url" }
                }
                .statusCode()
                .is2xxSuccessful
    }
}
