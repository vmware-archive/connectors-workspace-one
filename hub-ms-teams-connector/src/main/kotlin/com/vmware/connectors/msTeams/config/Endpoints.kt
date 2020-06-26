/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msTeams.config

/**
 * Backend Service API endpoints
 */
object Endpoints {

    const val teamBetaUrl = "https://graph.microsoft.com/beta"

    fun getTeamsUrl(baseUrl: String) = "$baseUrl/me/joinedTeams"

    fun getBatchUrl(baseUrl: String) = "$baseUrl/\$batch"

    fun getReplyToMessageUrl(teamId: String, channelId: String, messageId: String, baseUrl: String) = "$baseUrl/teams/$teamId/channels/$channelId/messages/$messageId/replies"

    fun getChannelsUsingBatchUrl(teamId: String) = "/teams/$teamId/channels"

    fun getTimeZoneUsingBatchUrl() = "/me/mailboxsettings/timeZone"

    fun getIdUsingBatchUrl() = "/me"

    fun getChannelMessagesDeltaUsingBatchUrl(dateTime: String, teamId: String, channelId: String) =
            "/teams/$teamId/channels/$channelId/messages/delta?\$filter=lastModifiedDateTime gt $dateTime&\$top=50"

    fun getMessageRepliesUsingBatchUrl(teamId: String, channelId: String, messageId: String) =
            "/teams/$teamId/channels/$channelId/messages/$messageId/replies"
}