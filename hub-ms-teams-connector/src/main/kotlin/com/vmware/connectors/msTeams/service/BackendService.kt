/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msTeams.service

import com.vmware.connectors.msTeams.config.Endpoints
import com.vmware.connectors.msTeams.dto.Body
import com.vmware.connectors.msTeams.dto.Channel
import com.vmware.connectors.msTeams.dto.Message
import com.vmware.connectors.msTeams.dto.Team
import com.vmware.connectors.msTeams.utils.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono

/**
 * ConnectorBackendService class to communicate with Microsoft Teams backend API for fetching necessary information
 * and performing the action (reply) on message
 * and every function in the class requires the following parameters
 * baseUrl - tenant url
 * authorization - Microsoft Teams User Bearer Token
 *
 * @property client WebClient: library to make async http calls
 */
@Component
class BackendService(
        @Autowired private val client: WebClient
) {

    private val logger = getLogger()

    /**
     * fetches the list of team objects for this user
     *
     * @param authorization is the token needed for authorizing the call
     * @return list of Team objects for this user
     */
    private suspend fun getTeams(authorization: String, baseUrl: String): List<Team> {
        val url = Endpoints.getTeamsUrl(baseUrl)
        return client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .awaitExchangeAndThrowError {
                    logger.error(it) { "error while retrieving teams" }
                }
                .awaitBody<Map<String, Any>>()
                .getListOrException<Map<String, Any>>("value")
                .convertValue()
    }

    /**
     * fetches the messages where the user is @mentioned
     *
     * @param authorization : backend token for o365
     * @param baseUrl : graph url
     * @return List of message objects where user is @mentioned
     */
    suspend fun getMentionedMessages(authorization: String, baseUrl: String): List<Message> {
        val teams = this.getTeams(authorization, baseUrl)
        val teamsMap = teams.map {
            it.id to it
        }.toMap()
        val channelsBatchBody = buildBatchBodyForChannels(teams)
        val userIdBatchBody = buildIndividualBatchRequest(
                "userId",
                HttpMethod.GET.name,
                Endpoints.getIdUsingBatchUrl()
        )
        val timeZoneBatchBody = buildIndividualBatchRequest(
                "userTimeZone",
                HttpMethod.GET.name,
                Endpoints.getTimeZoneUsingBatchUrl()
        )
        val response = getBatchResponses(
                authorization,
                channelsBatchBody.plus(userIdBatchBody)
                        .plus(timeZoneBatchBody),
                baseUrl
        )
        val responses = response.getListOrException<Map<String, Any>>("responses")
        val userId = responses
                .find { it["id"] == "userId" }!!
                .getMapOrException<String, Any>("body")
                .getStringOrException("id")
        val userTimeZone = responses
                .find { it["id"] == "userTimeZone" }!!
                .getMapOrDefault<String, Any>("body", mapOf("value" to "UTC"))
                .getStringOrException("value")
        val channels = getChannelsFromChannelsBatchResponse(responses, teamsMap)
        val channelsMap = channels.associateBy { it.id }
        val teamIdToChannelIdMap = channels.map { it.teamId to it.id }
        val recentMessages = getRecentMessagesFromBatches(
                authorization,
                channelsMap,
                teamIdToChannelIdMap,
                userTimeZone,
                baseUrl
        )
        val messagesMap = recentMessages.associateBy { it.id }
        val replyMessages = getMessagesReplies(
                authorization,
                messagesMap,
                recentMessages,
                userTimeZone,
                baseUrl
        )
        return getMessagesWithUserMentions(getRecentMessages(replyMessages), userId)
    }

    /**
     * fetches recent messages of all channels for which user is part of
     *
     * @param authorization the backend token
     * @param channelMap map with channelId as key and channel as value
     * @param teamIdToChannelIdMap map with teamId as key and channel as value
     * @param baseUrl graph url
     * @param userTimeZone TimeZone of the user
     * @param messages list of all channel messages that are returned when recursion completes
     * @return list of recent messages of all channels
     */
    private suspend fun getRecentMessagesFromBatches(
            authorization: String,
            channelMap: Map<String, Channel>,
            teamIdToChannelIdMap: List<Pair<String, String>>,
            userTimeZone: String,
            baseUrl: String,
            messages: List<Message> = emptyList()
    ): List<Message> {
        return when {
            teamIdToChannelIdMap.isEmpty() -> messages
            teamIdToChannelIdMap.size <= 20 -> {
                val body = buildBatchBodyForMessages(teamIdToChannelIdMap, HttpMethod.GET.name)
                messages.plus(
                        getRecentMessagesFromBatch(
                                body,
                                authorization,
                                channelMap,
                                userTimeZone,
                                baseUrl
                        )
                )
            }
            else -> {
                val body = buildBatchBodyForMessages(teamIdToChannelIdMap.take(20), HttpMethod.GET.name)
                getRecentMessagesFromBatches(
                        authorization,
                        channelMap,
                        teamIdToChannelIdMap.drop(20),
                        userTimeZone,
                        baseUrl,
                        messages.plus(
                                getRecentMessagesFromBatch(
                                        body,
                                        authorization,
                                        channelMap,
                                        userTimeZone,
                                        baseUrl
                                )
                        )
                )
            }
        }
    }

    /**
     * fetches the recent messages from given batch body
     *
     * @param batchBody body for performing batch request
     * @param authorization the backend token
     * @param channelMap map with channelId as key and channel as value
     * @param baseUrl graph url
     * @param userTimeZone TimeZone of the user
     * @param messages list of all messages that are returned when recursion completes
     * @param messagesMap map with messageId as key and message as value
     * @param replies boolean value indicating whether it is for replies or not
     * @return recent messages of channels present in batch body if replies is false,
     *          else recent message replies of messages present in batch body
     */
    private suspend fun getRecentMessagesFromBatch(
            batchBody: List<Map<String, String>>,
            authorization: String,
            channelMap: Map<String, Channel>,
            userTimeZone: String,
            baseUrl: String,
            messages: List<Message> = emptyList(),
            messagesMap: Map<String, Message> = emptyMap(),
            replies: Boolean = false
    ): List<Message> {
        return if (batchBody.isEmpty())
            messages
        else {
            val response = getResponseFromBatch(authorization, batchBody, baseUrl)
            val responses = response.getListOrException<Map<String, Any>>("responses")
            val responseList = if (replies)
                transformBatchResponseToMessageBatchResponse(responses, messagesMap)
            else
                transformBatchResponseToMessageBatchResponse(responses, channelMap, userTimeZone)
            val (newBody, messageList) = getNextBatchBodyAndRecentMessages(responseList, replies = replies)
            getRecentMessagesFromBatch(
                    newBody,
                    authorization,
                    channelMap,
                    userTimeZone,
                    baseUrl,
                    messages.plus(messageList),
                    messagesMap,
                    replies
            )
        }
    }

    /**
     * Replies to a message
     *
     * @param message the message to be replied to
     * @param authorization the backend token
     * @param comments the new message to be posted as a reply
     * @param baseUrl the graph url
     * @return Boolean true if replying to message is successful,
     *                 false if fails
     */
    suspend fun replyToTeamsMessage(
            message: Message,
            authorization: String,
            comments: String,
            baseUrl: String
    ): Boolean {
        val url = Endpoints.getReplyToMessageUrl(
                message.teamId,
                message.channelId,
                message.replyId,
                baseUrl
        )
        return client
                .post()
                .uri(url)
                .body(Mono.just(getReplyMessageBody(comments)))
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) {
                        "Error while replying to a message  Url:$url"
                    }
                }
                .statusCode()
                .is2xxSuccessful
    }

    /**
     * returns the body for the reply message
     *
     * @param comments the new message to be posted as a reply
     * @return body for the reply message
     */
    private fun getReplyMessageBody(comments: String): Map<String, Any> {
        return mapOf(
                "body" to Body(content = comments, contentType = "text")
        )
    }

    /**
     * fetches response for a batch request
     *
     * @param authorization  backend token for o365
     * @param baseUrl graph url
     * @param body body for the batch request
     *
     * @return the response of batch request
     */
    private suspend fun getResponseFromBatch(
            authorization: String,
            body: List<Map<String, String>>,
            baseUrl: String
    ): Map<String, Any> {
        return client.post()
                .uri(Endpoints.getBatchUrl(baseUrl))
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .body(
                        Mono.just(
                                mapOf(
                                        "requests" to body
                                )
                        )
                )
                .awaitExchangeAndThrowError {
                    logger.error(it) { "Error while fetching response from batch BatchBody:$body" }
                }
                .awaitBody()
    }

    /**
     * fetches recent messages of all channels for which user is part of
     *
     * @param authorization the backend token
     * @param messagesMap map with messageId as key and message as value
     * @param baseUrl graph url
     * @param userTimeZone TimeZone of the user
     * @param messages list of messages for which replies to be  obtained
     * @param totalMessages list of all messages that are returned when recursion completes
     * @return list of recent messages of all channels
     */
    private suspend fun getMessagesReplies(
            authorization: String,
            messagesMap: Map<String, Message>,
            messages: List<Message>,
            userTimeZone: String,
            baseUrl: String,
            totalMessages: List<Message> = emptyList()
    ): List<Message> {
        return when {
            messages.isEmpty() -> totalMessages
            messages.size <= 20 -> {
                val body = buildBatchBodyForMessageReplies(messages, "GET")
                totalMessages.plus(
                        getRecentMessagesFromBatch(
                                body,
                                authorization,
                                emptyMap(),
                                userTimeZone,
                                baseUrl,
                                messages,
                                messagesMap,
                                true
                        )
                )
            }
            else -> {
                val body = buildBatchBodyForMessageReplies(messages.take(20), "GET")
                getMessagesReplies(
                        authorization,
                        messagesMap,
                        messages.drop(20),
                        userTimeZone,
                        baseUrl,
                        totalMessages.plus(
                                getRecentMessagesFromBatch(
                                        body,
                                        authorization,
                                        emptyMap(),
                                        userTimeZone,
                                        baseUrl,
                                        messages,
                                        messagesMap,
                                        true
                                )
                        )
                )
            }
        }
    }

    /**
     * fetches responses of all batch requests
     *
     * @param authorization  backend token for o365
     * @param baseUrl graph url
     * @param body body for the batch request
     *
     * @return the responses of all batch requests
     */
    private suspend fun getBatchResponses(
            authorization: String,
            body: List<Map<String, String>>,
            baseUrl: String
    ): Map<String, Any> {
        val responses = body.chunked(20)
                .flatMap {
                    getResponseFromBatch(
                            authorization,
                            it,
                            baseUrl
                    ).getListOrDefault<Map<String, Any>>("responses")
                }
        return mapOf("responses" to responses)
    }
}

