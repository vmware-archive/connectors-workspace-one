package com.vmware.connectors.msTeams.utils

import com.vmware.connectors.msTeams.config.DATE_FORMAT_PATTERN
import com.vmware.connectors.msTeams.config.Endpoints
import com.vmware.connectors.msTeams.dto.*
import org.springframework.http.HttpMethod
import java.text.SimpleDateFormat
import java.util.*

/**
 * creates the single batch request
 *
 * @param id: id of the individual request
 * @param method: http method to be used
 * @param url: url of the individual request
 * @param body: body of the individual request (optional)
 * @param headers : headers of the individual request (optional)
 * @returns the body of single batch request
 */
fun getSingleBatchRequest(
        id: String,
        method: String,
        url: String,
        body: String? = null,
        headers: String? = null
): Map<String, String> {
    val map = mapOf(
            "id" to id,
            "method" to method,
            "url" to url
    )
    return when {
        headers == null -> map
        body == null -> map.plus("headers" to headers)
        else -> map.plus(mapOf("headers" to headers, "body" to body))
    }
}

/**
 * creates the body of the batch
 *
 * @param idList: list containing team id and channel id
 * @param method : http method to be used
 * @returns the body of the batch
 */
fun getMessagesUsingBatchesUrlBody(
        idList: List<Pair<String, String>>,
        method: String
): List<Map<String, String>> {
    val date = getDateTimeMinusHours(1)
    return idList.map { (teamID, channelID) ->
        getSingleBatchRequest(
                "$teamID $channelID",
                method,
                Endpoints.getChannelMessagesDeltaUsingBatchUrl(date, teamID, channelID)
        )
    }
}

/**
 * gets the list of Message Objects in last one hour
 *
 * @param messages : List of Message Objects
 * @returns the list of Message Objects in last one hour
 *
 */
fun getRecentMessages(messages: List<Message>): List<Message> {
    val sortedMessages = messages.sortedByDescending { it.createdDate }
    return sortedMessages.takeWhile {
        it.createdDate > (Date() - Minutes(5, "UTC"))
    }
}

/**
 * gets the list of Message Objects that contains given user mentions
 *
 * @param messages : List of Message Objects
 * @param userId : id of the user
 * @returns the list of Message Objects that contains given user mentions
 */
fun getMessagesWithUserMentions(messages: List<Message>, userId: String): List<Message> {
    return messages.filter { message ->
        message.mentions?.find {
            it.mentioned.user.id == userId
        } != null
    }
}

/**
 * gets the pair of Recent Messages and New body for the batch
 *
 * @param responses : List of Response Objects of a batch
 * @param body : body of the batch
 * @param messages : List of Message Objects
 * @returns the pair of Recent Messages and New body for the batch
 */
fun getNewBodyAndRecentMessages(
        responses: List<Response>,
        body: List<Map<String, String>> = emptyList(),
        messages: List<Message> = emptyList(),
        replies: Boolean = false
): Pair<List<Map<String, String>>, List<Message>> {
    return if (responses.isEmpty())
        Pair(body, messages)
    else {
        val response = responses.first()
        val (messages1, booleanValue) = getMessageAndBoolean(response, replies)
        if (booleanValue && response.body.parsedNextLink != null) {
            getNewBodyAndRecentMessages(
                    responses.drop(1),
                    body.plus(getSingleBatchRequest(response.id, "GET", response.body.parsedNextLink)),
                    messages.plus(messages1),
                    replies
            )
        } else {
            getNewBodyAndRecentMessages(
                    responses.drop(1),
                    body,
                    messages.plus(messages1),
                    replies
            )
        }
    }
}

/**
 * modifies the responses
 *
 * @param responses obtained after batch request
 * @param idChannelMap contains the map of channel id and team id to Channel object
 * @return the Modified Response Object
 */
fun getModifiedResponsesFromResponse(
        responses: List<Map<String, Any>>,
        idChannelMap: Map<String, Channel>,
        userTimeZone: String
): List<Response> {
    return responses.map { resp ->
        val ids = resp.getStringOrException("id").split(" ")
        val (teamId, channelId) = (ids[0] to ids[1])
        val channel = idChannelMap.getValue(channelId)
        val batchBody = resp.getMapOrException<String, Any>("body")
        resp + (
                "body" to
                        getModifiedBatchBody(
                                batchBody,
                                teamId,
                                channelId,
                                channel.teamName,
                                channel.displayName,
                                userTimeZone
                        )
                )
    }.convertValue()
}

/**
 * this function will return the Date as String with Format [EEE dd-MMM-yy hh:mm a]
 *
 * @param dateString Date Object as String
 * @param timeZone timeZone of the User
 * @returns the Date as String
 */
fun getDateFormatString(dateString: String, timeZone: String): String {
    val formatter = SimpleDateFormat(DATE_FORMAT_PATTERN)
            .apply {
                this.timeZone = TimeZone.getTimeZone("UTC")
            }
    val date = formatter.parse(getDateStringWithRespectToTimeZone(dateString, timeZone))
    val formatter1 = SimpleDateFormat("EEE dd-MMM-yy hh:mm a")
            .apply {
                this.timeZone = TimeZone.getTimeZone("UTC")
            }
    return formatter1.format(date)
}

/**
 * this function will return the requestBody of the getChannels Api
 *
 * @param teams List of Team Objects
 * @returns the request Body of the Channels API
 */
fun getChannelsUsingBatchBody(teams: List<Team>): List<Map<String, String>> {
    return teams.map {
        getSingleBatchRequest(
                it.id,
                HttpMethod.GET.name,
                Endpoints.getChannelsUsingBatchUrl(it.id)
        )
    }
}

/**
 * this function will returns the List of Channel Objects
 *
 * @param responses is the response of the API call
 * @param teamMap is the Map with teamName as keys and Team Object as Values
 * @returns the List of Channel Objects
 */
fun getChannelsFromResponse(responses: List<Map<String, Any>>, teamMap: Map<String, Team>): List<Channel> {
    val filteredResponse = responses.filter {
        it["id"] !in listOf("userId", "userTimeZone")
    }
    return filteredResponse.flatMap { resp ->
        val id = resp.getValue("id") as String
        val body = resp.getMapOrException<String, Any>("body")
        val value = body.getListOrException<Map<String, Any>>("value")
        value.map {
            it.plus("teamId" to id)
                    .plus(
                            "teamName" to teamMap.getValue(id).displayName
                    )
        }.convertValue<List<Channel>>()
    }
}

/**
 * filters the mentions list
 *
 * @param msg:map containing the messages
 * @returns the list of filtered messages
 */
fun filterMentions(msg: Map<String, Any>): List<Map<String, Any>> {
    val mentions = msg.getListOrException<Map<String, Any>>("mentions")
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
 * this function will returns the modified Request Body
 *
 * @param batchBody is the Request Body to be Modified
 * @param teamId is uniqueId of the team
 * @param channelId is uniqueId of the Channel
 * @param teamName is the Name of the Team
 * @param channelName is the Name of the Channel
 * @param userTimeZone is the TimeZone of the User
 * @returns the Modified Request Body
 */
fun getModifiedBatchBody(
        batchBody: Map<String, Any>,
        teamId: String,
        channelId: String,
        teamName: String,
        channelName: String,
        userTimeZone: String
): Map<String, Any> {
    return batchBody +
            (
                    "value" to batchBody
                            .getListOrException<Map<String, Any>>("value")
                            .map { msg ->
                                msg + mapOf(
                                        "teamId" to teamId,
                                        "channelId" to channelId,
                                        "channelName" to channelName,
                                        "userTimeZone" to userTimeZone,
                                        "mentions" to filterMentions(msg),
                                        "teamName" to teamName
                                )
                            }
                    )
}

/**
 * this function will return the new Response from the given Response
 *
 * @param responses Responses to be modified
 * @param messagesMap is the Map with messageId as keys and Message Object as values
 * @returns the List of Response Objects
 */
fun getModifiedResponsesFromResponse(
        responses: List<Map<String, Any>>,
        messagesMap: Map<String, Message>
): List<Response> {
    val newResp = responses.map { resp ->
        val messageId = resp.getStringOrException("id")
        val message = messagesMap.getValue(messageId)
        val batchBody = resp.getMapOrException<String, Any>("body")
        resp + (
                "body" to
                        getModifiedBatchBody(
                                batchBody,
                                message.teamId,
                                message.channelId,
                                message.teamName,
                                message.channelName,
                                message.userTimeZone
                        )
                )
    }
    return newResp.convertValue()
}

/**
 * this function will return the Pair of Message Objects List to Boolean Value
 *  depending on the message whether it is reply or direct Message
 *
 *  @param response Response Object
 *  @param replies Whether the message is reply or not
 *  @returns the Pair of Message Objects List to Boolean Value
 */
fun getMessageAndBoolean(response: Response, replies: Boolean = false): Pair<List<Message>, Boolean> {
    val messages1 = response.body.value
    return if (replies) {
        val recentMessages = getRecentMessages(messages1)
        recentMessages to (messages1.size == recentMessages.size)
    } else
        messages1 to (messages1.size == 50)
}