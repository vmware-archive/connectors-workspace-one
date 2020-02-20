package com.vmware.connectors.msPlanner.utils

import com.vmware.connectors.msPlanner.config.Endpoints
import com.vmware.connectors.msPlanner.dto.TaskMetaInfo
import org.jsoup.Jsoup
import org.springframework.http.HttpMethod

/**
 * This function will return the single batch request.
 *
 * @param id is the identity of the request
 * @param method is the http method to be called
 * @param url is the url of the end point
 * @returns the single request for batch
 */
fun getSingleBatchRequest(
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
 * This function will return the PlanRequestBatchBody
 *
 * @param groupIds is the ids of the users groups
 * @returns the batch request body
 */
fun getPlanBatchBody(
        groupIds: List<String>
): List<Map<String, String>> {
    return groupIds.map { groupId ->
        getSingleBatchRequest(
                groupId,
                HttpMethod.GET.name,
                Endpoints.getPlanBatchBodyUrl(groupId)
        )
    }
}

/**
 * This function will return the BucketRequestBatchBody.
 *
 * @param planDetails is the details of plans in which each element
 *  contains planId and groupId separated with space.
 * @returns the batch request body
 */
fun getBucketBatchBody(
        planDetails: List<String>
): List<Map<String, String>> {
    return planDetails.map { planDetail ->
        val list = planDetail.split(" ")
        getSingleBatchRequest(
                planDetail,
                HttpMethod.GET.name,
                Endpoints.getBucketBatchBodyUrl(list[1])
        )
    }
}

/**
 * This function will return the PlanRequestBatchBody.
 *
 * @param taskMetaInfos is the list of serialized TaskMetaInfo object
 * @returns the batch request body
 */
fun getTaskBatchBody(
        taskMetaInfos: List<String>
): List<Map<String, String>> {
    return taskMetaInfos.map { taskMetaInfoString ->
        val taskMetaInfo = JsonParser.deserialize<TaskMetaInfo>(taskMetaInfoString)
        getSingleBatchRequest(
                taskMetaInfo.bucketId,
                HttpMethod.GET.name,
                Endpoints.getTaskBatchBodyUrl(taskMetaInfo.bucketId)
        )
    }
}

/**
 * This function will return the ConversationThread Body.
 *
 * @param taskName is the title of the task
 * @param comment is the message to be added
 * @returns the body of the conversation Thread.
 */
fun getConversationThreadBody(
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
 * This function will return the AddComments Body.
 *
 * @param message is the message to be sent
 * @returns the AddComments body.
 */
fun getAddCommentsToTaskBody(
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
 *  @param string is the HTML Page Content
 *  @returns the required Content
 */
fun getStringExtractedFromHtml(string: String): String {
    val document = Jsoup.parse(string)
    document.getElementById("jSanity_hideInPlanner")?.remove() ?: return document.text()
    document.getElementsByTag("span")?.remove()
    return document.text()
}

/**
 * this function will generate the HTML document
 *
 * @param list List containing name and Comment
 * @returns the HTML document
 */
fun generateHtmlFromComments(list: List<Pair<String, String>>): String {
    val prefix = "<html>\n" +
            "<body>\n" +
            "<table>\n"
    val suffix = "</tr>\n" +
            "</table>\n" +
            "\n" +
            "</body>\n" +
            "</html>"
    return list.joinToString(prefix = prefix, postfix = suffix, separator = "\n") {
        "<tr>\n" +
                " <td > ${it.first}&nbsp:&nbsp${it.second} </td>\n\n" +
                " </tr>"
    }
}