/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msPlanner.utils

import com.vmware.connectors.msPlanner.config.Endpoints
import com.vmware.connectors.msPlanner.dto.TaskMetaInfo
import org.jsoup.Jsoup
import org.springframework.http.HttpMethod

/**
 * Builds the individual batch request.
 *
 * @param id is the identity of the request
 * @param method is the http method to be called
 * @param url is the url of the end point
 * @returns the single request for batch
 */
fun buildIndividualBatchRequest(
        id: String,
        method: String,
        url: String
): Map<String, String> {
    return mapOf(
            "id" to id,
            "method" to method,
            "url" to url
    )
}

/**
 * builds the Plan Batch Body
 *
 * @param groupIds is the list of Ids of the user groups
 * @returns the batch request body build from [groupIds]
 */
fun buildPlanBatchBody(
        groupIds: List<String>
): List<Map<String, String>> {
    return groupIds.map { groupId ->
        buildIndividualBatchRequest(
                groupId,
                HttpMethod.GET.name,
                Endpoints.getPlanBatchBodyUrl(groupId)
        )
    }
}

/**
 * builds the BucketRequestBatchBody.
 *
 * @param planDetails is the list of plan Details of the user.
 *  contains planId and groupId separated with space.
 * @returns the batch request body for bucket batch request
 */
fun buildBucketBatchBody(
        planDetails: List<String>
): List<Map<String, String>> {
    return planDetails.map { planDetail ->
        val list = planDetail.split(" ")
        buildIndividualBatchRequest(
                planDetail,
                HttpMethod.GET.name,
                Endpoints.getBucketBatchBodyUrl(list[1])
        )
    }
}

/**
 * builds the TaskRequestBatchBody.
 *
 * @param taskMetaInfos is the list of serialized TaskMetaInfo objects
 * @returns the batch request body for task batch t=request
 */
fun buildTaskBatchBody(
        taskMetaInfos: List<String>
): List<Map<String, String>> {
    return taskMetaInfos.map { taskMetaInfoString ->
        val taskMetaInfo = JsonParser.deserialize<TaskMetaInfo>(taskMetaInfoString)
        buildIndividualBatchRequest(
                taskMetaInfo.bucketId,
                HttpMethod.GET.name,
                Endpoints.getTaskBatchBodyUrl(taskMetaInfo.bucketId)
        )
    }
}

/**
 * builds the ConversationThread request Body.
 *
 * @param taskName is the title of the task
 * @param comment is the message to be added
 * @returns the body of the conversation Thread request.
 */
fun buildConversationThreadBody(
        taskName: String,
        comment: String
): Map<String, Any> {
    return mapOf(
            "topic" to "Comments on task \"$taskName\"",
            "threads" to listOf(mapOf(
                    "posts" to listOf(
                            mapOf(
                                    "body" to mapOf(
                                            "contentType" to "text",
                                            "content" to comment
                                    ),
                                    "newParticipants" to emptyList<Any>()
                            )
                    )
            ))
    )
}

/**
 * builds  the add comment to task request Body.
 *
 * @param message is the message to be sent
 * @returns the add comment to task request body.
 */
fun buildAddCommentsToTaskBody(
        message: String
): Map<String, Any> {
    return mapOf(
            "post" to mapOf(
                    "body" to mapOf(
                            "contentType" to "text",
                            "content" to message
                    )
            )
    )
}

/**
 * this function takes the HTML Page Content and returns the
 *  Required Content
 *
 *  @param html is the HTML Page Content
 *  @returns the required Content
 */
fun getStringExtractedFromHtml(html: String): String {
    val document = Jsoup.parse(html)
    document.select("table").remove()
    document.select("span").remove()
    return document.text()
            .replace("\\r\\n", "")
            .replace("\\n", "")
            .trim()
}