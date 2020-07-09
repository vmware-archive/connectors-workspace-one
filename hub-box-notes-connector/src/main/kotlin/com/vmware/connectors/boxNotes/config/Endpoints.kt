/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.boxNotes.config

import com.backflipt.commons.serialize
import com.backflipt.commons.urlEncode


object Endpoints {
    fun listNotesUrl(baseUrl: String) = "$baseUrl/search?query=boxnote&fields=extension,name&type=file&file_extensions=boxnote&content_types=${listOf("name", "description").serialize().urlEncode()}"
    fun listCollaboratorsUrl(baseUrl: String, noteId: String) = "$baseUrl/files/$noteId/collaborations"
    fun inviteUserToNoteUrl(baseUrl: String) = "$baseUrl/collaborations"
    fun addComment(baseUrl: String) = "$baseUrl/comments"
    fun listNoteComments(baseUrl: String, noteId: String) = "$baseUrl/files/$noteId/comments?fields=tagged_message,message,created_by,modified_at"
    fun getUserInfo(baseUrl: String) = "$baseUrl/users/me"
}

