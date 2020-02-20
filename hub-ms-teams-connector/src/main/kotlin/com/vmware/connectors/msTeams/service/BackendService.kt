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
 * ConnectorBackendService class
 *
 * @property client: WebClient: library to make async http calls
 */
@Component
class BackendService(
        @Autowired private val client: WebClient
) {

    private val logger = getLogger()

    /**
     * This function will return the list of team objects
     *
     * @param authorization is the token needed for authorizing the call
     * @return list of Team objects
     */
    private suspend fun getTeams(authorization: String, baseUrl: String): List<Team> {
        val url = Endpoints.getTeamsUrl(baseUrl)
        val response = client
                .get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .awaitExchangeAndThrowError {
                    logger.error(it) { "error while retrieving teams" }
                }
                .awaitBody<Map<String, Any>>()
        return response
                .getListOrException<Map<String, Any>>("value")
                .convertValue()
    }

    /**
     * This function gets the user messages where he is @mentioned
     *
     * @param authorization : backend token for o365
     * @param baseUrl : graph url
     * @return List<Message> values
     */
    suspend fun getMentionedMessages(authorization: String, baseUrl: String): List<Message> {
        val teams = this.getTeams(authorization, baseUrl)
        val teamsMap = teams.map {
            it.id to it
        }.toMap()
        val teamsBody = getChannelsUsingBatchBody(teams)
        val idBody = getSingleBatchRequest(
                "userId",
                HttpMethod.GET.name,
                Endpoints.getIdUsingBatchUrl()
        )
        val timeZoneBody = getSingleBatchRequest(
                "userTimeZone",
                HttpMethod.GET.name,
                Endpoints.getTimeZoneUsingBatchUrl()
        )
        val response = getResponsesFromBatch(
                authorization,
                teamsBody.plus(idBody)
                        .plus(timeZoneBody),
                baseUrl
        )
        val responses = response.getListOrException<Map<String, Any>>("responses")
        val userId = responses
                .find { it["id"] == "userId" }!!
                .getMapOrException<String, Any>("body")
                .getStringOrException("id")
        val userTimeZone = responses
                .find { it["id"] == "userTimeZone" }!!
                .getMapOrException<String, Any>("body")
                .getStringOrException("value")
        val channel = getChannelsFromResponse(responses, teamsMap)
        val idChannelMap = channel.map { it.id to it }
        val ids = idChannelMap.map { it.second.teamId to it.second.id }
        val recentMessages = getRecentMessagesFromBatches(
                authorization,
                idChannelMap.toMap(),
                ids,
                userTimeZone,
                baseUrl
        )
        val messageMap = recentMessages.map {
            it.id to it
        }.toMap()
        val replyMessages = getMessagesReplies(
                authorization,
                messageMap,
                recentMessages,
                userTimeZone,
                baseUrl
        )
        return getMessagesWithUserMentions(getRecentMessages(replyMessages), userId)
    }

    /**
     * This function filters the recent user messages in batch call where he is @mentioned
     *
     * @param authorization : backend token for o365
     * @param baseUrl : graph url
     * @return List<Message> values
     */
    private suspend fun getRecentMessagesFromBatches(
            authorization: String,
            idChannelMap: Map<String, Channel>,
            ids: List<Pair<String, String>>,
            userTimeZone: String,
            baseUrl: String,
            messages: List<Message> = emptyList()
    ): List<Message> {
        return when {
            ids.isEmpty() -> messages
            ids.size <= 20 -> {
                val body = getMessagesUsingBatchesUrlBody(ids, HttpMethod.GET.name)
                messages.plus(
                        getRecentMessagesFromBatch(
                                body,
                                authorization,
                                idChannelMap,
                                userTimeZone,
                                baseUrl
                        )
                )
            }
            else -> {
                val body = getMessagesUsingBatchesUrlBody(ids.take(20), HttpMethod.GET.name)
                getRecentMessagesFromBatches(
                        authorization,
                        idChannelMap,
                        ids.drop(20),
                        userTimeZone,
                        baseUrl,
                        messages.plus(
                                getRecentMessagesFromBatch(
                                        body,
                                        authorization,
                                        idChannelMap,
                                        userTimeZone,
                                        baseUrl
                                )
                        )
                )
            }
        }
    }

    /**
     * This function filters the recent messages for each batch call where he is @mentioned
     *
     * @param body body for performing batch request
     * @param authorization the backend token
     * @param idChannelMap map with channelId as key and channel as value
     * @param baseUrl graph url
     * @param userTimeZone TimeZone of the user
     * @param messages list of messages
     * @param messagesMap map with messageId as key and message as value
     * @param replies boolean value indicating whether it is for replies or not
     * @return List<Message> values
     */
    private suspend fun getRecentMessagesFromBatch(
            body: List<Map<String, String>>,
            authorization: String,
            idChannelMap: Map<String, Channel>,
            userTimeZone: String,
            baseUrl: String,
            messages: List<Message> = emptyList(),
            messagesMap: Map<String, Message> = emptyMap(),
            replies: Boolean = false
    ): List<Message> {
        return if (body.isEmpty())
            messages
        else {
            val response = getResponseFromBatch(authorization, body, baseUrl)
            val responses = response.getListOrException<Map<String, Any>>("responses")
            val responseList = if (replies)
                getModifiedResponsesFromResponse(responses, messagesMap)
            else
                getModifiedResponsesFromResponse(responses, idChannelMap, userTimeZone)
            val (newBody, messageList) = getNewBodyAndRecentMessages(responseList, replies = replies)
            getRecentMessagesFromBatch(
                    newBody,
                    authorization,
                    idChannelMap,
                    userTimeZone,
                    baseUrl,
                    messages.plus(messageList),
                    messagesMap,
                    replies
            )
        }
    }

    /**
     * Replies to a message where the user has been @mentioned
     *
     * @param message the message to be replied tp
     * @param authorization the backend token
     * @param comments the new message to be posted as a reply
     * @param baseUrl the graph url
     * @return Boolean
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
                .body(Mono.just(getBodyReplyMessage(comments)))
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .awaitExchangeAndThrowError {
                    logger.error(it) {
                        "Error while replying to a message for user -> with $authorization"
                    }
                }
                .statusCode()
                .is2xxSuccessful
    }

    /**
     * returns the body for the reply message
     *
     * @param comments comments is the message to be replied to the message
     * @return map body for the reply message
     */
    private fun getBodyReplyMessage(comments: String): Map<String, Any> {
        return mapOf(
                "body" to Body(content = comments, contentType = "text")
        )
    }

    /**
     * gets the emaiId from the Vmware user token
     *
     * @param token it is the Vmware user token
     * @return emailId string
     */
    fun getUserEmailFromToken(token: String): String? {
        val pl = token
                .split(" ")
                .lastOrNull()
                ?.split(".")
                ?.get(1)
                ?.toBase64DecodedString()

        return pl?.deserialize()
                ?.getStringOrNull("eml")
    }

    /**
     * returns response from batch request
     *
     * @param authorization  backend token for o365
     * @param baseUrl graph url
     * @param body body for the batch request
     *
     * @return returns the response
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
                    logger.error(it) { "Error while fetching response from batch for user ->  with $authorization" }
                }
                .awaitBody()
    }

    /**
     * This function gets the user messages replies where he is @metioned
     *
     * @param authorization : backend token for o365
     * @param baseUrl : graph url
     * @return List<Message> values
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
                val body = getMessageRepliesUrlBody(messages, "GET")
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
                val body = getMessageRepliesUrlBody(messages.take(20), "GET")
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
     * returns response from batch requests
     *
     * @param authorization  backend token for o365
     * @param baseUrl graph url
     * @param body body for the batch request
     *
     * @return returns the response
     */
    private suspend fun getResponsesFromBatch(
            authorization: String,
            body: List<Map<String, String>>,
            baseUrl: String
    ): Map<String, Any> {
        val responses = body.chunked(20)
                .flatMap  {
            getResponseFromBatch(
                    authorization,
                    body,
                    baseUrl
            ).getListOrDefault<Map<String, Any>>("responses")
        }
        return mapOf("responses" to responses)
    }
}

/**
 * returns the list of batch requests body that can be used for getting message replies
 *
 * @param messages list of messages
 * @param method a http method type
 * @return list of batch requests body
 */
fun getMessageRepliesUrlBody(messages: List<Message>, method: String): List<Map<String, String>> {
    return messages.map {
        getSingleBatchRequest(
                it.id,
                method,
                Endpoints.getMessageRepliesUsingBatchUrl(
                        it.teamId, it.channelId, it.id
                )
        )
    }
}
