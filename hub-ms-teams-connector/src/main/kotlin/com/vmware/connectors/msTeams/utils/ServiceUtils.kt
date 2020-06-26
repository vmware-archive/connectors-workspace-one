/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msTeams.utils

import com.vmware.connectors.msTeams.config.*
import com.vmware.connectors.msTeams.dto.*
import org.jsoup.Jsoup
import org.springframework.http.HttpMethod
import java.text.SimpleDateFormat
import java.util.*

/**
 * builds a individual batch request
 *
 * @param id: id of the individual request
 * @param method: http method of the individual request
 * @param url: url of the individual request
 * @returns a individual batch request
 */
fun buildIndividualBatchRequest(
        id: String,
        method: String,
        url: String
): Map<String, String> {
    return mapOf(
            "id" to id,
            "method" to method,
            "url" to url
    )
}

/**
 * builds the body for the batch request for retrieving messages within [MESSAGES_LOOKUP_WINDOW_HOURS] Hours
 *
 * @param teamIdToChannelIdMap: list containing team id and channel id
 * @param method : http method of the batch request
 * @returns the body of the batch request
 */
fun buildBatchBodyForMessages(
        teamIdToChannelIdMap: List<Pair<String, String>>,
        method: String
): List<Map<String, String>> {
    val date = getDateTimeMinusHours(MESSAGES_LOOKUP_WINDOW_HOURS)
    return teamIdToChannelIdMap.map { (teamID, channelID) ->
        buildIndividualBatchRequest(
                "$teamID $channelID",
                method,
                Endpoints.getChannelMessagesDeltaUsingBatchUrl(date, teamID, channelID)
        )
    }
}

/**
 * returns the list of Message Objects in specified window in [MENTIONS_LOOKUP_WINDOW_MINUTES] minutes
 *
 * @param messages : List of Message Objects
 * @returns the list of Message Objects in specified window in [MENTIONS_LOOKUP_WINDOW_MINUTES] minutes
 *
 */
fun getRecentMessages(messages: List<Message>): List<Message> {
    val sortedMessages = messages.sortedByDescending { it.createdDate }
    return sortedMessages.takeWhile {
        it.createdDate > (Date() - Minutes(MENTIONS_LOOKUP_WINDOW_MINUTES, "UTC"))
    }
}

/**
 * returns the list of Message Objects in which user is @mentioned
 *
 * @param messages : List of Message Objects
 * @param userId : id of the user
 * @returns the list of Message Objects in which user is @mentioned
 */
fun getMessagesWithUserMentions(messages: List<Message>, userId: String): List<Message> {
    return messages.filter { message ->
        message.mentions?.find {
            it.mentioned.user.id == userId
        } != null
    }
}

/**
 * returns  Recent Messages and body for performing next batch request
 *
 * @param messageBatchResponses : List of message batch responses
 * @param nextBatchBody : body of the next batch that is returned when recursion completes
 * @param messages : List of recent Message Objects of all responses that is returned when recursion completes
 * @returns Recent Messages and body for performing next batch request
 */
fun getNextBatchBodyAndRecentMessages(
        messageBatchResponses: List<MessageBatchResponse>,
        nextBatchBody: List<Map<String, String>> = emptyList(),
        messages: List<Message> = emptyList(),
        replies: Boolean = false
): Pair<List<Map<String, String>>, List<Message>> {
    return if (messageBatchResponses.isEmpty())
        Pair(nextBatchBody, messages)
    else {
        val messageBatchResponse = messageBatchResponses.first()
        val (messageBatchResponseMessages, isNext) = getMessageAndIsNext(messageBatchResponse, replies)
        if (isNext && messageBatchResponse.body.parsedNextLink != null) {
            getNextBatchBodyAndRecentMessages(
                    messageBatchResponses.drop(1),
                    nextBatchBody.plus(buildIndividualBatchRequest(messageBatchResponse.id, "GET", messageBatchResponse.body.parsedNextLink)),
                    messages.plus(messageBatchResponseMessages),
                    replies
            )
        } else {
            getNextBatchBodyAndRecentMessages(
                    messageBatchResponses.drop(1),
                    nextBatchBody,
                    messages.plus(messageBatchResponseMessages),
                    replies
            )
        }
    }
}

/**
 * transforms the batch response to Message Batch Response
 *
 * @param responses obtained after batch request
 * @param channelsMap contains the map with channel id as key and channel as value
 * @return MessageBatchResponse object
 */
fun transformBatchResponseToMessageBatchResponse(
        responses: List<Map<String, Any>>,
        channelsMap: Map<String, Channel>,
        userTimeZone: String
): List<MessageBatchResponse> {
    return responses.map { resp ->
        val ids = resp.getStringOrException("id").split(" ")
        val (teamId, channelId) = (ids[0] to ids[1])
        val channel = channelsMap.getValue(channelId)
        val batchBody = resp.getMapOrException<String, Any>("body")
        resp + (
                "body" to
                        transformMessagesOfMessageBatchResponseBody(
                                batchBody,
                                teamId,
                                channelId,
                                channel.teamName,
                                channel.displayName,
                                userTimeZone
                        )
                )
    }.mapNotNull {
        it.convertValueOrNull<MessageBatchResponse>()
    }
}

/**
 * returns the Date with respect to the given time zone in [RETURN_FORMAT] format
 *
 * @param dateString Date Object as String
 * @param timeZone timeZone of the User
 * @returns the Date with respect to the given time zone in [RETURN_FORMAT] format
 */
fun getDateFormatString(dateString: String, timeZone: String): String {
    val formatter = SimpleDateFormat(DATE_FORMAT_PATTERN)
            .apply {
                this.timeZone = TimeZone.getTimeZone("UTC")
            }
    val date = formatter.parse(getDateStringWithRespectToTimeZone(dateString, timeZone))
    val returnFormatter = SimpleDateFormat(RETURN_FORMAT)
            .apply {
                this.timeZone = TimeZone.getTimeZone("UTC")
            }
    return returnFormatter.format(date)
}

/**
 * builds the requestBody for fetching channels from teams
 *
 * @param teams List of Team Objects
 * @returns the requestBody for fetching channels from teams
 */
fun buildBatchBodyForChannels(teams: List<Team>): List<Map<String, String>> {
    return teams.map {
        buildIndividualBatchRequest(
                it.id,
                HttpMethod.GET.name,
                Endpoints.getChannelsUsingBatchUrl(it.id)
        )
    }
}

/**
 * returns the List of Channel Objects from the channels batch response
 *
 * @param channelsBatchResponse is the response of the channels batch call
 * @param teamsMap is the Map with team id as keys and Team Object as Values
 * @returns the List of Channel Objects
 */
fun getChannelsFromChannelsBatchResponse(
        channelsBatchResponse: List<Map<String, Any>>,
        teamsMap: Map<String, Team>
): List<Channel> {
    val filteredResponse = channelsBatchResponse.filter {
        it["id"] !in listOf("userId", "userTimeZone")
    }
    return filteredResponse.flatMap { resp ->
        val id = resp.getValue("id") as String
        val body = resp.getMapOrException<String, Any>("body")
        val value = body.getListOrDefault<Map<String, Any>>("value")
        value.map {
            it.plus("teamId" to id)
                    .plus(
                            "teamName" to teamsMap.getValue(id).displayName
                    )
        }.convertValue<List<Channel>>()
    }
}

/**
 * returns mentions of the given raw message in which any user is mentioned that is removing @all @team mentions etc.
 *
 * @param message is the  raw message
 * @returns list of only mentions of the given raw message in which any user is mentioned
 *           that is removing @all @team mentions etc
 */
fun filterMentions(message: Map<String, Any>): List<Map<String, Any>> {
    val mentions = message.getListOrException<Map<String, Any>>("mentions")
    return if (mentions.isEmpty())
        mentions
    else {
        mentions.filter {
            val mentioned = it.getMapOrException<String, Any>("mentioned")
            mentioned["user"] != null
        }
    }
}

/**
 * transforms the messages of the message batch response body
 *
 * @param messageBatchResponseBody is the Response Body to be transformed
 * @param teamId is uniqueId of the team
 * @param channelId is uniqueId of the Channel
 * @param teamName is the Name of the Team
 * @param channelName is the Name of the Channel
 * @param userTimeZone is the TimeZone of the User
 * @returns transformed message batch response body
 */
fun transformMessagesOfMessageBatchResponseBody(
        messageBatchResponseBody: Map<String, Any>,
        teamId: String,
        channelId: String,
        teamName: String,
        channelName: String,
        userTimeZone: String
): Map<String, Any> {
    return messageBatchResponseBody +
            (
                    "value" to messageBatchResponseBody
                            .getListOrDefault<Map<String, Any>>("value")
                            .map { message ->
                                message + mapOf(
                                        "teamId" to teamId,
                                        "channelId" to channelId,
                                        "channelName" to channelName,
                                        "userTimeZone" to userTimeZone,
                                        "mentions" to filterMentions(message),
                                        "teamName" to teamName
                                )
                            }
                    )
}

/**
 * Transforms tht messages of the message batch response
 *
 * @param responses Responses to be modified
 * @param messagesMap is the Map with messageId as keys and Message Object as values
 * @returns the List of transformed MessageBatchResponse Objects
 */
fun transformBatchResponseToMessageBatchResponse(
        responses: List<Map<String, Any>>,
        messagesMap: Map<String, Message>
): List<MessageBatchResponse> {
    val newResp = responses.map { resp ->
        val messageId = resp.getStringOrException("id")
        val message = messagesMap.getValue(messageId)
        val batchBody = resp.getMapOrException<String, Any>("body")
        resp + (
                "body" to
                        transformMessagesOfMessageBatchResponseBody(
                                batchBody,
                                message.teamId,
                                message.channelId,
                                message.teamName,
                                message.channelName,
                                message.userTimeZone
                        )
                )
    }
    return newResp.mapNotNull {
        it.convertValueOrNull<MessageBatchResponse>()
    }
}

/**
 * returns the recent messages and boolean value indicating whether to perform next batch request or not
 *  depending on the message whether it is reply or direct Message
 *
 *  @param messageBatchResponse Response Object
 *  @param replies Whether the message is reply or not
 *  @returns recent messages and boolean value indicating whether to perform next batch request or not
 */
fun getMessageAndIsNext(messageBatchResponse: MessageBatchResponse, replies: Boolean = false): Pair<List<Message>, Boolean> {
    val messages1 = messageBatchResponse.body.value
    return if (replies) {
        val recentMessages = getRecentMessages(messages1)
        recentMessages to (messages1.size == recentMessages.size)
    } else
        messages1 to (messages1.size == 50)
}

/**
 * prepends @ for every mention for the message body
 *
 * @param messageBody body of the message
 * @return transformed message body with @ prepended to every mention
 */
fun prependMentionsAndExtractBody(messageBody: String): String {
    val doc = Jsoup.parse(messageBody)
    doc.getElementsByTag("at")
            .forEach { it.prepend("@") }
    return doc.text()
}

/**
 * builds batch body using messages for fetching message replies
 *
 * @param messages list of messages
 * @param method a http method type
 * @return batch body for message replies
 */
fun buildBatchBodyForMessageReplies(messages: List<Message>, method: String): List<Map<String, String>> {
    return messages.map {
        buildIndividualBatchRequest(
                it.id,
                method,
                Endpoints.getMessageRepliesUsingBatchUrl(
                        it.teamId, it.channelId, it.id
                )
        )
    }
}