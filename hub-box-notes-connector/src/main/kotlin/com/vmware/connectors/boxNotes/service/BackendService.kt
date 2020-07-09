/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.boxNotes.service

import com.backflipt.commons.*
import com.vmware.connectors.boxNotes.config.DATE_FORMAT_PATTERN
import com.vmware.connectors.boxNotes.config.DATE_FORMAT_PATTERN_RETURN
import com.vmware.connectors.boxNotes.config.Endpoints
import com.vmware.connectors.boxNotes.config.ZONE_ID
import com.vmware.connectors.boxNotes.dto.Comment
import com.vmware.connectors.boxNotes.dto.CommentMetadata
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody


/**
 * BackendService class to communicate with Box Notes backend API for fetching necessary information
 * and every function in the class requires the following parameters
 * baseUrl - tenant url
 * authorization - Box Notes User Bearer Token
 *
 * @property client WebClient: library to make async http calls
 */
@Component
class BackendService(
        @Autowired private val client: WebClient
) {
    private val logger = getLogger()

    /**
     * gets the current user Info
     *
     * @param baseUrl is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @return current userInfo
     */
    suspend fun getUserInfo(baseUrl: String, authorization: String): Map<String, Any> {
        val url = Endpoints.getUserInfo(baseUrl)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error while fetching  user info" }
                }
                .awaitBody()
    }

    /**
     * gets the all notes information of the  current user
     *
     * @param baseUrl is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @return  the list of notes Info
     */
    private suspend fun listUserNotes(baseUrl: String, authorization: String): List<Map<String, Any>> {
        val url = Endpoints.listNotesUrl(baseUrl)
        val result = client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error While Fetching User Notes url:$url" }
                }
                .awaitBody<Map<String, Any>>()
        return result.getListOrDefault("entries")
    }

    /**
     * gets the collaborators info of the  given note
     *
     * @param baseUrl is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @param noteId id of the note
     * @return the list of collaborators info
     */
    private suspend fun listNoteCollaborators(baseUrl: String, authorization: String, noteId: String): List<String> {
        val url = Endpoints.listCollaboratorsUrl(baseUrl, noteId)
        val result = client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error While Fetching Note Collaborators url:$url" }
                }
                .awaitBody<Map<String, Any>>()
        val entries = result.getListOrDefault<Map<String, Any>>("entries")
        return entries.map {
            val collaborators = if (it.getStringOrDefault("status") == "pending")
                it.getStringOrNull("invite_email")
            else it.getMapOrDefault<String, Any>("accessible_by").getStringOrNull("name")
            val currentUser = it.getMapOrDefault<String, Any>("created_by").getStringOrNull("name")
            listOfNotNull(collaborators, currentUser)
        }.flatten().distinct()
    }

    /**
     * gets the latest comments info of the  given note
     *
     * @param baseUrl is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @param noteId id of the note
     * @param userName name of the user
     * @return the list of [Comment] Object
     */
    private suspend fun getMentionedCommentsInfo(baseUrl: String, authorization: String, noteId: String, userName: String): List<Comment> {
        val url = Endpoints.listNoteComments(baseUrl, noteId)
        val result = client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error While Fetching Comments Info For Notes Url:$url UserName:$userName" }
                }
                .awaitBody<Map<String, Any>>()
        val comments = result.getListOrDefault<Map<String, Any>>("entries")
        val noteLookUpWindow = System.currentTimeMillis().minus(60 * 60 * 1000)
        val oneHourBeforeCurrentDateInUtc = getCurrentUtcTimeUsingEpoch(noteLookUpWindow, DATE_FORMAT_PATTERN)
        val mentionedComments = comments.filter {
            val taggedMessage = it.getStringOrDefault("tagged_message")
            taggedMessage.isNotEmpty() && taggedMessage.contains(userName)
        }.filter {
            getDateStringWithRespectToZoneId(it.getStringOrDefault("modified_at"),
                    "UTC", DATE_FORMAT_PATTERN, DATE_FORMAT_PATTERN_RETURN, ZONE_ID) > oneHourBeforeCurrentDateInUtc
        }
        return mentionedComments.map {
            Comment(
                    message = it.getStringOrDefault("message"),
                    createdBy = it.getMapOrDefault<String, Any>("created_by")
                            .getStringOrDefault("name"),
                    modifiedAt = it.getStringOrDefault("modified_at"),
                    id = it.getStringOrDefault("id")
            )
        }
    }

    /**
     * gets the  comments info of the notes
     *
     * @param baseUrl is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @param userName name of the user
     * @return the list of[CommentMetadata] Object
     */
    suspend fun getMentionedComments(baseUrl: String, authorization: String, userName: String): List<CommentMetadata> {
        val notesInfo = listUserNotes(baseUrl, authorization)
        return notesInfo.flatMap { metadata ->
            val title = metadata.getStringOrDefault("name")
            val noteId = metadata.getStringOrException("id")
            val users = listNoteCollaborators(baseUrl, authorization, noteId)
            val comments = getMentionedCommentsInfo(baseUrl, authorization, noteId, userName)
            comments.map { comment ->
                CommentMetadata(
                        users = users,
                        title = title,
                        lastEditor = comment.createdBy,
                        lastUpdatedDate = getDateStringWithRespectToZoneId(
                                comment.modifiedAt,
                                "UTC",
                                DATE_FORMAT_PATTERN,
                                DATE_FORMAT_PATTERN_RETURN,
                                ZONE_ID),
                        noteId = noteId,
                        commentId = comment.id,
                        comment = comment.message
                )
            }
        }
    }

    /**
     * share the given note to the given email
     *
     * @param baseUrl is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @param noteId id of the note
     * @param emailId email of the user you need to add
     * @return the status of adding user
     */
    suspend fun inviteUserToNote(baseUrl: String, authorization: String, noteId: String, emailId: String): Boolean {
        val url = Endpoints.inviteUserToNoteUrl(baseUrl)
        val body = mapOf(
                "item" to mapOf(
                        "type" to "file",
                        "id" to noteId
                ),
                "accessible_by" to mapOf(
                        "type" to "user",
                        "login" to emailId
                ),
                "role" to "editor"

        )
        val statusCode = client
                .post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error While adding user to Note Url:$url" }
                }
                .statusCode()
        return statusCode.value() == 201
    }

    /**
     * Adds a comment by the user to a specific note
     *
     * @param baseUrl is the root url of the api calls
     * @param authorization is the token needed for authorizing the call
     * @param noteId id of the note
     * @param message comment add to given note
     * @return  the status of adding comment
     */
    suspend fun addComment(baseUrl: String, authorization: String, noteId: String, message: String): Boolean {
        val url = Endpoints.addComment(baseUrl)
        val data = mapOf(
                "item" to mapOf(
                        "type" to "file",
                        "id" to noteId
                ),
                "message" to message

        )
        val statusCode = client
                .post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(data)
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error While adding comment to Note Url:$url" }
                }
                .statusCode()
        return statusCode.value() == 201
    }
}




