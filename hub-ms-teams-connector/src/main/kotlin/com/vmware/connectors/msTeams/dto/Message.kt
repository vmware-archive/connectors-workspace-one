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
 * @property createdDate message created date
 * @property createdDateTime message created date pattern
 * @property replyId the id to be used for replying the message
 * @property formatter the simple date formatter object
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
        val url:String?,
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
        @JsonProperty("channelName")
        /**
         * shows the description of the channel
         */
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
        val attachments: List<ChatMessageAttachment>,
        /**
         * shows the description of the team
         */
        @JsonProperty("teamName")
        val teamName: String
) {
        private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
        val createdDate: Date = formatter.parse(createdDateTime)
        val dateTime = getDateFormatString(createdDateTime, userTimeZone)
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
         * Optional. The user associated with this action.
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
 * Response object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Response(
        /**
         * id of the response
         */
        @JsonProperty("id")
        val id: String,
        /**
         * status of the response
         */
        @JsonProperty("status")
        val status: Int,
        /**
         * body of the response
         */
        @JsonProperty("body")
        val body: BatchBody

)

/**
 * Batch Body Object
 *
 * @property parsedNextLink it is the parsed next link
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BatchBody(
        /**
         * value of the batch body
         */
        @JsonProperty("value")
        val value: List<Message>,
        /**
         * no of items in batch body
         */
        @JsonProperty("@odata.count")
        val count: Int?,
        /**
         * next link for retrieving next set of items
         */
        @JsonProperty("@odata.nextLink")
        val nextLink: String? = null
) {
        val parsedNextLink = nextLink?.removePrefix(Endpoints.teamBetaUrl)
}

/**
 * ChatMessageAttachment Object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ChatMessageAttachment(
        /**
         * Read-only. Unique id of the attachment.
         */
        @JsonProperty("id")
        val id: String,
        /**
         * The media type of the content attachment. It can have the following values:
        reference: Attachment is a link to another file. Populate the contentURL with the link to the object.
        file: Raw file attachment. Populate the contenturl field with the base64 encoding of the file in data: format.
        image/: Image type with the type of the image specified ex: image/png, image/jpeg, image/gif. Populate the contentUrl field with the base64 encoding of the file in data: format.
        video/: Video type with the format specified. Ex: video/mp4. Populate the contentUrl field with the base64 encoding of the file in data: format.
        audio/: Audio type with the format specified. Ex: audio/wmw. Populate the contentUrl field with the base64 encoding of the file in data: format.
        application/card type: Rich card attachment type with the card type specifying the exact card format to use. Set content with the json format of the card. Supported values for card type include:
        application/vnd.microsoft.card.adaptive: A rich card that can contain any combination of text, speech, images,,buttons, and input fields. Set the content property to,an AdaptiveCard object.
        application/vnd.microsoft.card.animation: A rich card that plays animation. Set the content property,to an AnimationCardobject.
        application/vnd.microsoft.card.audio: A rich card that plays audio files. Set the content property,to an AudioCard object.
        application/vnd.microsoft.card.video: A rich card that plays videos. Set the content property,to a VideoCard object.
        application/vnd.microsoft.card.hero: A Hero card. Set the content property to a HeroCard object.
        application/vnd.microsoft.card.thumbnail: A Thumbnail card. Set the content property to a ThumbnailCard object.
        application/vnd.microsoft.com.card.receipt: A Receipt card. Set the content property to a ReceiptCard object.
        application/vnd.microsoft.com.card.signin: A user Sign In card. Set the content property to a SignInCard object.
         */
        @JsonProperty("contentType")
        val contentType: String,
        /**
         * URL for the content of the attachment. Supported protocols: http, https, file and data.
         */
        @JsonProperty("contentUrl")
        val contentUrl: String?,
        /**
         * The content of the attachment.
         * If the attachment is a rich card, set the property to the rich card object.
         * This property and contentUrl are mutually exclusive.
         */
        @JsonProperty("content")
        val content: String?,
        /**
         * Name of the attachment.
         */
        @JsonProperty("name")
        val name: String?,
        /**
         * URL to a thumbnail image that the channel can use if it supports using an alternative, smaller form of content or contentUrl.
         * For example, if you set contentType to application/word and set contentUrl to the location of the Word document, you might include a thumbnail image that represents the document.
         * The channel could display the thumbnail image instead of the document. When the user clicks the image, the channel would open the document.
         */
        @JsonProperty("thumbnailUrl")
        val thumbnailUrl: String?
)

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