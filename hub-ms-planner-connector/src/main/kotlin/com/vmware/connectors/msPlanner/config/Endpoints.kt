package com.vmware.connectors.msPlanner.config

/**
 * Backend Service API endpoints
 */
object Endpoints {
    fun getUserIdUrl(baseUrl: String) = "$baseUrl/me"

    fun getUserNameUrl(baseUrl: String, id: String) = "$baseUrl/users/$id"

    fun getUserTimeZoneUrl(baseUrl: String) = "$baseUrl/me/mailboxsettings/timeZone"

    fun getGroupIdsUrl(baseUrl: String) = "$baseUrl/me/getMemberGroups"

    fun getAllIdsUrl(baseUrl: String) = "$baseUrl/\$batch"

    fun getPlanBatchBodyUrl(groupId: String) = "groups/$groupId/planner/plans"

    fun getBucketBatchBodyUrl(planId: String) = "/planner/plans/$planId/buckets"

    fun getTaskBatchBodyUrl(bucketId: String) = "/planner/buckets/$bucketId/tasks"

    fun updateTaskUrl(baseUrl: String, taskId: String) = "$baseUrl/planner/tasks/$taskId"

    fun replyToTaskUrl(baseUrl: String, groupId: String, threadId: String) =
            "$baseUrl/groups/$groupId/threads/$threadId/reply"

    fun getNewConversationThreadUrl(baseUrl: String, groupId: String) =
            "$baseUrl/groups/$groupId/conversations"

    fun getTaskByIdUrl(baseUrl: String, taskId: String) =
            "$baseUrl/planner/tasks/$taskId"

    fun taskUrl(groupId: String, planId: String, taskId: String) = "https://tasks.office.com/Home/Planner#/plantaskboard?groupId=$groupId&planId=$planId&taskId=$taskId"

    fun getTaskDetailsUrl(baseUrl: String, taskId: String) = "$baseUrl/planner/tasks/$taskId/details"

    fun getLatestCommentOnTaskUrl(baseUrl: String, groupId: String, threadId: String) = "$baseUrl/groups/$groupId/threads/$threadId/posts"

    fun getLabelCategoriesUrl(baseUrl: String, planId: String) = "$baseUrl/planner/plans/$planId/details"
}