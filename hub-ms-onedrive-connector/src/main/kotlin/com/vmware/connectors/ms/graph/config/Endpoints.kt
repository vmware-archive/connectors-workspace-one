package com.vmware.connectors.ms.graph.config


private const val ODATA_API_PREFIX = "api/data/v9.0"

/**
 * Backend Service API endpoints
 */
object Endpoints {

    fun getUserTimeZoneUrl(baseUrl: String) = "$baseUrl/me/mailboxsettings/timeZone"

    fun getAccessRequestMailsUrl(baseUrl: String, filterQry: String) = """$baseUrl/me/messages?${"$"}filter=${filterQry}&${"$"}top=100&${"$"}orderby=receivedDateTime desc&select=id,subject,body,replyTo,isRead,receivedDateTime"""

    fun getFileIdFromFilePathUrl(baseUrl: String, siteId: String, filePath: String) = "$baseUrl/sites/$siteId/drive/root:$filePath?\$select=id"

    fun getRelevantPeopleWithNameUrl(baseUrl: String, searchName: String) = "$baseUrl/me/people?\$search=\"$searchName\"&\$select=displayName,givenName,emailAddresses"

    fun inviteUrl(baseUrl: String, siteId: String, itemId: String) = "$baseUrl/sites/$siteId/drive/items/$itemId/invite"

    fun getSiteIdCallUrl(baseUrl: String, siteHostName: String, sitePath: String) = "$baseUrl/sites/$siteHostName:$sitePath"

    fun sendMailUrl(baseUrl: String) = "$baseUrl/me/sendMail"

    fun getAllUsers(baseUrl: String) = "$baseUrl/users?\$select=displayName,givenName,mail"
}