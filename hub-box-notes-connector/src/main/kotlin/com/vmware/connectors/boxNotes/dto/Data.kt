/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.boxNotes.dto

import com.backflipt.commons.JsonParser
import com.backflipt.commons.getStringOrException
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


@JsonIgnoreProperties(ignoreUnknown = true)
data class CommentMetadata(
        val noteId: String,
        val title: String,
        val lastUpdatedDate: String,
        val lastEditor: String,
        val users: List<String> = emptyList(),
        val commentId: String,
        val comment: String
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class Comment(
        val message: String,
        val id: String,
        val createdBy: String,
        val modifiedAt: String
)

data class DocumentInfo(
        @JsonProperty("comments")
        val comments: String,
        @JsonProperty("note")
        private val document: String
) {
    private val noteResp = JsonParser.deserialize(document)
    val noteObj = CommentMetadata(
            noteId = noteResp.getStringOrException("noteId"),
            title = noteResp.getStringOrException("file"),
            lastUpdatedDate = noteResp.getStringOrException("updated-by"),
            lastEditor = noteResp.getStringOrException("updated-at"),
            commentId = noteResp.getStringOrException("id"),
            users = noteResp.getStringOrException("collaborators").split(","),
            comment = noteResp.getStringOrException("comments")
    )
}
