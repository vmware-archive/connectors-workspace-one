/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.googleDocs.config

/**
 * Backend Service API endpoints
 */
object Endpoints {
    fun getGoogleDocumentsUrl(baseUrl: String, query: String, filter: String) = "$baseUrl/drive/v2/files?q=$query&fields=$filter"
    fun inviteUserToDocumentUrl(baseUrl: String, docId: String) = "$baseUrl/drive/v2/files/$docId/permissions"
    fun listDocumentUsersUrl(baseUrl: String, docId: String) = "$baseUrl/drive/v2/files/$docId/permissions"
    fun getCurrentUserMailUrl(baseUrl: String, filter: String) = "$baseUrl/drive/v2/about?fields=$filter"
    fun listCommentsAndRepliesUrl(baseUrl: String, docId: String, filter: String) = "$baseUrl/drive/v2/files/$docId/comments?fields=$filter"
    fun replyToMessageUrl(baseUrl: String, docId: String, commentId: String, filter: String) = "$baseUrl/drive/v3/files/$docId/comments/$commentId/replies?fields=$filter"
}