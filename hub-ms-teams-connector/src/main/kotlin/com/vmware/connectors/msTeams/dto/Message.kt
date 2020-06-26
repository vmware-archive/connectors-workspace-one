/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msTeams.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.vmware.connectors.msTeams.config.Endpoints
import com.vmware.connectors.msTeams.utils.deserialize
import com.vmware.connectors.msTeams.utils.getDateFormatString
import java.text.SimpleDateFormat
import java.util.*

/**
 * Team object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Team(
        /**
         * unique id that represents the team
         */
        @JsonProperty("id")
        val id: String,
        /**
         * shows the description of the team
         */
        @JsonProperty("displayName")
        val displayName: String
)

/**
 * Channel object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Channel(
        /**
         * unique id that represents the channel
         */
        @JsonProperty("id")
        val id: String,
        /**
         * shows the description of the channel
         */
        @JsonProperty("displayName")
        val displayName: String,
        /**
         * unique id that represents the team
         */
        @JsonProperty("teamId")
        val teamId: String = "",
        /**
         * shows the description of the team
         */
        @JsonProperty("teamName")
        val teamName: String
)

/**
 * Message object
 *
 * @property createdDate created date of the message
 * @property createdDateTime created date of the message in user time zone
 * @property replyId the id to be used for replying the message
 * @property formatter the simple date formatter object
 * @property createdDateInUserTimeZone is the message created date in user time zone
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Message(
        /**
         * Unique Id of the message.
         */
        @JsonProperty("id")
        val id: String,
        /**
         * Url of the message in teams
         */
        @JsonProperty("webUrl")
        val url: String?,
        /**
         * Id of the parent chat message or root chat message of the thread.
         */
        @JsonProperty("replyToId")
        val replyToId: String?,
        /**
         * The subject of the chat message, in plaintext.
         */
        @JsonProperty("subject")
        val subject: String?,
        /**
         * Timestamp of when the chat message was created.
         */
        @JsonProperty("createdDateTime")
        val createdDateTime: String,
        /**
         *  Timestamp of when the chat message is created or edited,
         *  including when a reply is made (if it's a root chat message in a channel)
         *  or a reaction is added or removed.
         */
        @JsonProperty("lastModifiedDateTime")
        val lastModifiedDateTime: String?,
        /**
         *  Details of the sender of the chat message.
         */
        @JsonProperty("from")
        val from: IdentitySet,
        /**
         * Summary text of the chat message that could be used for push notifications
         * and summary views or fall back views.
         * Only applies to channel chat messages, not chat messages in a chat.
         */
        @JsonProperty("summary")
        val summary: String?,
        /**
         * List of entities mentioned in the chat message.
         * Currently supports user, bot, team, channel.
         */
        @JsonProperty("mentions")
        val mentions: List<ChatMessageMention>?,
        /**
         * Plaintext/HTML representation of the content of the chat message.
         * Representation is specified by the contentType inside the body.
         * The content is always in HTML if the chat message contains a chatMessageMention.
         */
        @JsonProperty("body")
        val body: Body,
        /**
         * unique id that represents the channel
         */
        @JsonProperty("channelId")
        val channelId: String = "",
        /**
         * unique id that represents the team
         */
        @JsonProperty("teamId")
        val teamId: String = "",
        /**
         * shows the description of the channel
         */
        @JsonProperty("channelName")
        val channelName: String = "",
        /**
         * Timezone of the user
         */
        @JsonProperty("userTimeZone")
        val userTimeZone: String = "",
        /**
         *  Attached files. Attachments are currently read-only – sending attachments is not supported.
         */
        @JsonProperty("attachments")
        val attachments: List<Map<String, Any>>,
        /**
         * shows the description of the team
         */
        @JsonProperty("teamName")
        val teamName: String
) {
    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
    val createdDate: Date = formatter.parse(createdDateTime)
    val createdDateInUserTimeZone = getDateFormatString(createdDateTime, userTimeZone)
    val replyId = replyToId ?: id
}

/**
 *  body object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Body(
        /**
         * content type of the body
         */
        @JsonProperty("contentType")
        val contentType: String,
        /**
         * content of the body.Possible values are text and HTML.
         */
        @JsonProperty("content")
        val content: String
)

/**
 * IdentitySet Object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class IdentitySet(
        /**
         * The user associated with this action.
         */
        @JsonProperty("user")
        val user: Identity
)

/**
 * Identity Object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Identity(
        /**
         * The identity's display name.
         * Note that this may not always be available or up to date.
         * For example, if a user changes their display name, the API may show the new value in a future response,
         * but the items associated with the user won't show up as having changed when using delta.
         */
        @JsonProperty("displayName")
        val displayName: String,
        /**
         * Unique identifier for the identity.

         */
        @JsonProperty("id")
        val id: String
)

/**
 * ChatMessageMention Object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ChatMessageMention(
        /**
         * Index of an entity being mentioned in the specified chatMessage.
         * Matches the {index} value in the corresponding <at id="{index}"> tag in the message body.
         */
        @JsonProperty("id")
        val id: String,
        /**
         * String used to represent the mention. For example, a user's display name, a team name.
         */
        @JsonProperty("mentionText")
        val mentionText: String,
        /**
         * The entity (user, application, team, or channel) that was mentioned.
         * If it was a channel or team that was @mentioned,
         * the identitySet contains a conversation property giving the ID of the team/channel,
         * and a conversationIdentityType property that represents either the team or channel.
         */
        @JsonProperty("mentioned")
        val mentioned: IdentitySet
)

/**
 * Message Batch Response object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MessageBatchResponse(
        /**
         * id of the response
         */
        @JsonProperty("id")
        val id: String,
        /**
         * body of the response
         */
        @JsonProperty("body")
        val body: MessageBatchBody

)

/**
 * Message Batch Body Object
 *
 * @property parsedNextLink next link for retrieving next set of messages parsed,can be used as single batch request Url
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MessageBatchBody(
        /**
         * value of the batch body
         */
        @JsonProperty("value")
        val value: List<Message>,
        /**
         * next link for retrieving next set of items
         */
        @JsonProperty("@odata.nextLink")
        val nextLink: String? = null
) {
    val parsedNextLink = nextLink?.removePrefix(Endpoints.teamBetaUrl)
}

/**
 * MessageInfo object
 *
 * @property messageObj message object
 */
data class MessageInfo(
        /**
         * comments of the card
         */
        @JsonProperty("comments")
        val comments: String?,
        /**
         * action type of the card
         */
        @JsonProperty("actionType")
        val actionType: String,
        /**
         * message associated with the card
         */
        @JsonProperty("message")
        private val message: String
) {
    val messageObj = this.message.deserialize<Message>()
}