package com.vmware.connectors.ms.graph.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.vmware.connectors.ms.graph.utils.JsonParser
import com.vmware.connectors.ms.graph.utils.getStringOrNull
import java.util.*

/**
 * OutlookMailItem Object
 *
 * @property contentType contentType of the MailBody
 * @property contentIsHtml whether the content is html or not
 * @property content Body Content
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OutlookMailItem(
        /**
         * Unique identifier for the message.By default, this value changes
         * when the item is moved from one container (such as a folder or calendar) to another.
         * To change this behavior, use the Prefer: IdType="ImmutableId" header
         */
        @JsonProperty("id") val id: String,
        /**
         * The subject of the message.
         */
        @JsonProperty("subject") val subject: String,
        /**
         * 	The date and time the message was received.
         */
        @JsonProperty("receivedDateTime") val receivedDateTime: String,
        /**
         * 	The body of the message.
         * 	It can be in HTML or text format. Find out about safe HTML in a message body.
         */
        @JsonProperty("body") val body: Map<String, Any>,
        /**
         * 	The email addresses to use when replying.
         */
        @JsonProperty("replyTo") val replyTo: List<EmailAddress>,
        /**
         * Indicates whether the message has been read.
         */
        @JsonProperty("isRead") val isRead: Boolean
) {
    private val contentType = body.getStringOrNull("contentType")
    private val contentIsHtml: Boolean = (contentType == "html")
    val content = if (contentIsHtml) body.getStringOrNull("content") else null
}

/**
 * EmailAddress Object
 *
 * @property name Name of the User
 * @property address Address of the User
 */
data class EmailAddress(
        /**
         * emailAddress of the User
         */
        @JsonProperty("emailAddress") val emailAddress: Map<String, Any>
) {
    val name = emailAddress.getStringOrNull("name")
    val address = emailAddress.getStringOrNull("address")
}


/**
 *  AccessRequest Object
 */
data class AccessRequest(
        /**
         * Id of the AccessRequest
         */
        val id: String,
        /**
         * Name of the User Who Requested the Resource
         */
        val requestedBy: User?,
        /**
         * List of Users requestedFor the Resource
         */
        val requestedFor: List<User>,
        /**
         * Resource Object
         */
        val resource: Resource,
        /**
         * whether Resource is shared for Group or Not
         */
        val sharedForGroup: Boolean = false,
        /**
         * The date and time the message was received.
         */
        val receivedDate: Date
)

/**
 * Resource Object
 */
data class Resource(
        /**
         * Id of the Resource
         */
        val id: String,
        /**
         * Path of the Resource
         */
        val path: String,
        /**
         * Name of the Resource
         */
        val name: String,
        /**
         * url to access the Resource
         */
        val url: String,
        /**
         * siteId of the Resource
         */
        val siteId: String
)

/**
 * User Object
 */
data class User(
        /**
         * emailId of the User
         */
        val emailId: String,
        /**
         * Name of the User
         */
        val name: String,
        /**
         * personType of the User
         */
        val personType: String = ""
) {
    fun toEmailAddress() = EmailAddress(mapOf("name" to name, "address" to emailId))
}

/**
 * ApproveActionRequest Object
 *
 * @property accessRequestObj AccessRequest Object
 * @property rolesArray List of Roles
 */
data class ApproveActionRequest(
        /**
         * comments entered by the Administrator while Approving the Request
         */
        val comments: String?,
        /**
         * serialized AccessRequest Object
         */
        private val accessRequest: String,
        /**
         * Roles given while Approving
         */
        private val roles: String?
) {
    val accessRequestObj = JsonParser.deserialize<AccessRequest>(this.accessRequest)
    val rolesArray = roles?.split(",")?.map { it.trim() } ?: listOf("read")
}