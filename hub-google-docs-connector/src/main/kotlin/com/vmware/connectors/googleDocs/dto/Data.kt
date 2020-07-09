/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.googleDocs.dto

import com.backflipt.commons.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GoogleDocData(
        @JsonProperty("id")
        val id: String,
        @JsonProperty("modifiedDate")
        val modifiedDate: String
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class Comment(
        @JsonProperty("commentId")
        val commentId: String,
        @JsonProperty("modifiedDate")
        val modifiedDate: String,
        @JsonProperty("author")
        val author: Map<String, String>,
        @JsonProperty("content")
        val content: String,
        @JsonProperty("fileTitle")
        val fileTitle: String,
        @JsonProperty("replies")
        val replies: List<Map<String, Any>>?,
        @JsonProperty("users")
        val users: List<String>,
        @JsonProperty("docId")
        val docId: String,
        @JsonProperty("uniqueId")
        val uniqueId: String
) {
    val link = "https://docs.google.com/document/d/$docId/edit?disco=$commentId"
}


data class CommentInfo(
        @JsonProperty("comments")
        val comments: String,
        @JsonProperty("document")
        private val document: String
) {
    private val commentMap = this.document.deserialize()
    val commentObj = Comment(
            commentId = commentMap.getStringOrException("comment-id"),
            content = commentMap.getStringOrException("comment"),
            docId = commentMap.getStringOrException("doc-id"),
            users = commentMap.getStringOrException("collaborators").split(","),
            fileTitle = commentMap.getStringOrException("doc-title"),
            modifiedDate = commentMap.getStringOrException("updated-at"),
            author = commentMap.getMapOrDefault("author"),
            replies = commentMap.getListOrDefault("replies"),
            uniqueId = commentMap.getStringOrDefault("id")
    )

}