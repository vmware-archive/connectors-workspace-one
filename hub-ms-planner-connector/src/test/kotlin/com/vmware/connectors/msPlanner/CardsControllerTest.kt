/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msPlanner

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.vmware.connectors.msPlanner.config.Endpoints
import com.vmware.connectors.msPlanner.config.MESSAGE_ROUTING_PREFIX
import com.vmware.connectors.msPlanner.dto.Task
import com.vmware.connectors.msPlanner.utils.*
import com.vmware.connectors.test.ControllerTestsBase
import com.vmware.connectors.test.JsonNormalizer
import com.vmware.connectors.utils.IgnoredFieldsReplacer
import org.junit.Assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MimeTypeUtils
import org.springframework.web.reactive.function.BodyInserters
import uk.co.datumedge.hamcrest.json.SameJSONAs
import java.util.stream.Collectors

const val ROUTING_PREFIX_URL = "https://hero/connectors/ms_planner/"
const val taskId = "1234"

class CardsControllerTest : ControllerTestsBase() {

    private fun getAuthorizationToken(uri: String) = "Bearer " + accessToken(uri)
    private val authorization = "Bearer abc"

    @ParameterizedTest
    @ValueSource(strings = [
        "/planner/type/dismiss",
        "/cards/requests"
    ])
    @Throws(Exception::class)
    fun testProtectedResource(uri: String) {
        testProtectedResource(HttpMethod.POST, uri)
    }

    @Test
    @Throws(Exception::class)
    fun testGetImage(@Value("classpath:static/images/connector.png")
                     icon: Resource) {
        webClient.get()
                .uri("/images/connector.png")
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType(MimeTypeUtils.IMAGE_PNG_VALUE)
                .expectBody<ByteArray>().isEqualTo(icon.readAsByteArray())
    }

    @Test
    fun missingAuthorization() {
        webClient.post()
                .uri("/cards/requests")
                .exchange()
                .expectStatus().isUnauthorized
    }

    @Test
    fun `Test Success For Dismiss Task`(
            @Value("classpath:service/tasks/task1.json")
            task: Resource,
            @Value("classpath:service/user/user.json")
            user: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, mockBackend.url("")))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))

        val formData = LinkedMultiValueMap<String, String>()
        formData.add("task", task.readAsString())

        webClient.post()
                .uri("/planner/user/dismiss")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken("/planner/user/dismiss"))
                .header(X_AUTH_HEADER, mockBackend.url(""))
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is2xxSuccessful
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "/planner/user/dismiss",
        "/planner/tasks/taskId/comment",
        "/planner/tasks/taskId/mark/completed"
    ])
    fun `Test Auth Fail For Actions`(
            uri: String,
            @Value("classpath:service/tasks/task1.json")
            task: Resource
    ) {

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, mockBackend.url("")))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest().body("invalid session id"))

        val formData = LinkedMultiValueMap<String, String>()
        formData.add("task", task.readAsString())

        webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, mockBackend.url(""))
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
    }

    @Test
    fun `Test Success For Add Comment To Task Action`(
            @Value("classpath:service/tasks/task1.json")
            taskResource: Resource,
            @Value("classpath:service/user/user.json")
            user: Resource,
            @Value("classpath:service/conversationThread/conversationThread.json")
            conversationThread: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))

        val taskString = taskResource.readAsString()
        val task = JsonParser.deserialize<Task>(taskString)

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getTaskByIdUrl("", task.id)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(taskString, MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getNewConversationThreadUrl("", task.groupId)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(conversationThread.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.updateTaskUrl("", task.id)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.IF_MATCH, task.eTag))
                .andRespond(MockRestResponseCreators.withSuccess())

        listOf(null, "testing").map { comment ->
            val formData = LinkedMultiValueMap<String, String>()
            formData.add("task", taskString)
            formData.add("comments", comment)

            webClient.post()
                    .uri("/planner/tasks/$taskId/comment")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken("/planner/tasks/$taskId/comment"))
                    .header(X_AUTH_HEADER, authorization)
                    .header(X_BASE_URL_HEADER, mockBackend.url(""))
                    .body(BodyInserters.fromFormData(formData))
                    .exchange()
                    .expectStatus().is2xxSuccessful
        }
    }

    @Test
    fun `Test Success For Complete Task Action`(
            @Value("classpath:service/tasks/taskwithconversationId.json")
            task: Resource,
            @Value("classpath:service/user/user.json")
            user: Resource
    ) {
        val taskId = "4AOsPl68BU259Ie_5yODWmQAPXo9"
        val groupId = "c7a5329b-040b-41d2-9126-d5084a4b5148"
        val threadId = "AAQkADVlZTcwOTY4LTZjMTMtNGViYS05OTQ5LWY5Y2U2NmM1NjQzNAMkABAAYhuKlGdTKU_s0capHWZELRAAYhuKlGdTKU_s0capHWZELQ=="
        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getUserIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, mockBackend.url("")))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.replyToTaskUrl("", groupId, threadId)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, mockBackend.url("")))
                .andRespond(MockRestResponseCreators.withSuccess())

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.updateTaskUrl("", taskId)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.PATCH))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, mockBackend.url("")))
                .andRespond(MockRestResponseCreators.withSuccess())

        val formData = LinkedMultiValueMap<String, String>()
        formData.add("comments", "testing")
        formData.add("task", task.readAsString())

        val url = "/planner/tasks/1234/mark/completed"
        webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(url))
                .header(X_AUTH_HEADER, mockBackend.url(""))
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is2xxSuccessful
    }

    private fun transformCardsResponse(
            response: Resource,
            dueDate: String,
            task: String
    ): List<Map<String, Any>> {
        return JsonParser.deserialize(response.readAsString())
                .getListOrDefault<Map<String, Any>>("objects")
                .map { object1 ->
                    val body = object1.getMapOrDefault<String, Any>("body")
                    val actions = object1.getListOrDefault<Map<String, Any>>("actions")
                    val fields = body.getListOrDefault<Map<String, Any>>("fields")
                    object1.plus("body" to body.plus("fields" to fields.map {
                        val title = it.getStringOrDefault("title")
                        if (title == "Due Date")
                            it.plus("description" to dueDate)
                        else
                            it
                    })).plus(
                            "actions" to actions.map {
                                val request = it.getMapOrException<String, Any>("request")
                                if (request["task"] != null)
                                    it.plus("request" to request.plus("task" to task))
                                else
                                    it
                            }
                    )
                }

    }

    private fun getCardsResponse(): String {
        val uri = "/cards/requests"

        val data = webClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(MESSAGE_ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)

                .exchange()
                .expectStatus().isOk
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .returnResult<String>()
                .responseBody
                .collect(Collectors.joining())
                .map(JsonNormalizer::forCards)
                .block()
                ?.replace(Regex("http://localhost:\\d+/"), "/")

        val configuration = Configuration.builder()
                .jsonProvider(JacksonJsonNodeJsonProvider())
                .build()

        val context: DocumentContext = JsonPath.using(configuration).parse(data)

        return context.set("$.objects[?(@.hash =~ /" + IgnoredFieldsReplacer.UUID_PATTERN + "/)].hash", IgnoredFieldsReplacer.DUMMY_UUID)
                .set("$.objects[?(@.backend_id =~ /" + IgnoredFieldsReplacer.UUID_PATTERN + "/)].backend_id", IgnoredFieldsReplacer.DUMMY_UUID)
                .jsonString()
    }

    @Test
    fun `Test Success For Empty Cards Request And No TimeZone Configured`(
            @Value("classpath:service/user/user.json")
            user: Resource,
            @Value("classpath:service/groupResponses/groups1.json")
            groups: Resource,
            @Value("classpath:service/planResponses/plans1.json")
            plans: Resource,
            @Value("classpath:service/bucketResponses/buckets1.json")
            buckets: Resource,
            @Value("classpath:service/tasks/emptyTaskResponses.json")
            tasks: Resource,
            @Value("classpath:service/user/timezone.json")
            timezone: Resource,
            @Value("classpath:service/user/userdetails.json")
            userdetails: Resource,
            @Value("classpath:service/response.json")
            response: Resource,
            @Value("classpath:service/planResponses/planDetails.json")
            planDetails: Resource
    ) {
        val planId = "BODd529VLk6cwGeF35O4L2QAHS4b"

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getUserIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withNoContent())

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getGroupIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(groups.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(plans.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(plans.readAsString(), MediaType.APPLICATION_JSON))


        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getLabelCategoriesUrl("", planId)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(planDetails.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(buckets.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(buckets.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(tasks.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(tasks.readAsString(), MediaType.APPLICATION_JSON))


        val jsonResponse = response.readAsString()

        val data = getCardsResponse()

        Assert.assertThat<String>(data, SameJSONAs.sameJSONAs(jsonResponse))
    }

    private fun testSuccessForCardsRequestWithDifferentCases(
            taskDetails: Resource = ClassPathResource("service/tasks/taskDetails.json"),
            response: Resource = ClassPathResource("service/response1.json"),
            latestComment: Resource = ClassPathResource("service/LatestComment/LatestComment1.json"),
            plannerTask: Resource = ClassPathResource("service/tasks/task3.json"),
            tasks: Resource = ClassPathResource("service/tasks/task2.json")
    ) {
        val user = ClassPathResource("service/user/user.json")
        val groups = ClassPathResource("service/groupResponses/groups.json")
        val plans = ClassPathResource("service/planResponses/plans.json")
        val planDetails = ClassPathResource("service/planResponses/planDetails.json")
        val buckets = ClassPathResource("service/bucketResponses/buckets.json")
        val timezone = ClassPathResource("service/user/timezone.json")
        val emptyTasksResponse = ClassPathResource("service/tasks/taskResponses.json")
        val date = getCurrentUtcTime()
        val userId = "3fca3578-eee3-4f5d-b62d-47d17f3cce3a"
        val taskId = "evCy5UXMrkCrlVu3g4wx2mQAFC9-"
        val planId = "BODd529VLk6cwGeF35O4L2QAHS4b"
        val groupId = "1"
        val threadId = "thread123"
        val newResp = JsonParser.deserialize(tasks.readAsString()).getListOrDefault<Map<String, Any>>("responses").map { resp ->
            val batchBody = resp.getMapOrDefault<String, Any>("body")
            val value = batchBody.getListOrDefault<Map<String, Any>>("value")
            resp + (
                    "body" to mapOf("value" to value.map {
                        it.plus("dueDateTime" to date)
                    })
                    )
        }

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getUserIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getTaskDetailsUrl("", taskId)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(taskDetails.readAsString(), MediaType.APPLICATION_JSON))

        val taskResponse = JsonParser.serialize(mapOf("responses" to newResp))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(timezone.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getGroupIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(groups.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(plans.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getLabelCategoriesUrl("", planId)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(planDetails.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(buckets.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(emptyTasksResponse.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(taskResponse, MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getLatestCommentOnTaskUrl("", groupId, threadId)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(latestComment.readAsString(), MediaType.APPLICATION_JSON))


        val task = JsonParser.serialize(JsonParser.deserialize<Task>(plannerTask.readAsString()).copy(dueDateTime = date, userId = userId))

        val dueDate = getUserDueDateInUserTimeZone(date, "India Standard Time")

        val transformedObj = transformCardsResponse(response, dueDate, task)

        val jsonResponse = JsonParser.serialize(mapOf("objects" to transformedObj))

        val data = getCardsResponse()

        Assert.assertThat<String>(data, SameJSONAs.sameJSONAs(jsonResponse))
    }

    @Test
    fun `Test Success For Cards Request With Comments`(
            @Value("classpath:service/LatestComment/LatestComment.json")
            latestComment: Resource
    ) {
        testSuccessForCardsRequestWithDifferentCases(latestComment = latestComment)
    }

    @Test
    fun `Test Success For Cards Request With Many Comments`(
            @Value("classpath:service/LatestComment/LatestComment2.json")
            latestComment: Resource,
            @Value("classpath:service/response7.json")
            response: Resource
    ) {
        testSuccessForCardsRequestWithDifferentCases(latestComment = latestComment, response = response)
    }

    @Test
    fun `Test Success For Cards Request For Task With Priority With Urgent`(
            @Value("classpath:service/tasks/tasks1.json")
            tasks: Resource,
            @Value("classpath:service/tasks/task4.json")
            plannerTask: Resource,
            @Value("classpath:service/LatestComment/LatestComment.json")
            latestComment: Resource,
            @Value("classpath:service/response2.json")
            response: Resource
    ) {
        testSuccessForCardsRequestWithDifferentCases(tasks = tasks, plannerTask = plannerTask, latestComment = latestComment, response = response)
    }

    @Test
    fun `Test Success For Cards Request For Task With Priority With Low`(
            @Value("classpath:service/tasks/tasks2.json")
            tasks: Resource,
            @Value("classpath:service/tasks/task5.json")
            plannerTask: Resource,
            @Value("classpath:service/LatestComment/LatestComment.json")
            latestComment: Resource,
            @Value("classpath:service/response3.json")
            response: Resource
    ) {
        testSuccessForCardsRequestWithDifferentCases(tasks = tasks, plannerTask = plannerTask, latestComment = latestComment, response = response)
    }

    @Test
    fun `Test Success For Cards Request For Task With Priority With Important`(
            @Value("classpath:service/tasks/tasks3.json")
            tasks: Resource,
            @Value("classpath:service/tasks/task6.json")
            plannerTask: Resource,
            @Value("classpath:service/LatestComment/LatestComment.json")
            latestComment: Resource,
            @Value("classpath:service/response4.json")
            response: Resource
    ) {
        testSuccessForCardsRequestWithDifferentCases(tasks = tasks, plannerTask = plannerTask, latestComment = latestComment, response = response)
    }

    @Test
    fun `Test Success For Cards Request With Empty Task Details`(
            @Value("classpath:service/tasks/taskDetails1.json")
            taskDetails: Resource,
            @Value("classpath:service/response5.json")
            response: Resource

    ) {
        testSuccessForCardsRequestWithDifferentCases(taskDetails, response)
    }

    @Test
    fun `Test Success For Cards Request With Bad Task Details Response`(
            @Value("classpath:service/tasks/taskDetails2.json")
            taskDetails: Resource,
            @Value("classpath:service/response6.json")
            response: Resource

    ) {
        testSuccessForCardsRequestWithDifferentCases(taskDetails, response)
    }

    @Test
    fun `Test Success For Cards Request For Task Without Conversation Thread`(
            @Value("classpath:service/user/user.json")
            user: Resource,
            @Value("classpath:service/tasks/tasks4.json")
            tasks: Resource,
            @Value("classpath:service/tasks/task7.json")
            task1: Resource,
            @Value("classpath:service/tasks/taskDetails.json")
            taskDetails: Resource,
            @Value("classpath:service/groupResponses/groups.json")
            groups: Resource,
            @Value("classpath:service/planResponses/plans.json")
            plans: Resource,
            @Value("classpath:service/planResponses/planDetails.json")
            planDetails: Resource,
            @Value("classpath:service/bucketResponses/buckets.json")
            buckets: Resource,
            @Value("classpath:service/user/timezone.json")
            timezone: Resource,
            @Value("classpath:service/user/userdetails.json")
            userdetails: Resource,
            @Value("classpath:service/tasks/taskResponses.json")
            emptytasksResponse: Resource,
            @Value("classpath:service/response8.json")
            response: Resource

    ) {
        val date = getCurrentUtcTime()
        val userId = "3fca3578-eee3-4f5d-b62d-47d17f3cce3a"
        val taskId = "evCy5UXMrkCrlVu3g4wx2mQAFC9-"
        val planId = "BODd529VLk6cwGeF35O4L2QAHS4b"
        val newResp = JsonParser.deserialize(tasks.readAsString()).getListOrDefault<Map<String, Any>>("responses").map { resp ->
            val batchBody = resp.getMapOrDefault<String, Any>("body")
            val value = batchBody.getListOrDefault<Map<String, Any>>("value")
            resp + (
                    "body" to mapOf("value" to value.map {
                        it.plus("dueDateTime" to date)
                    })
                    )
        }

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getUserIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getTaskDetailsUrl("", taskId)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(taskDetails.readAsString(), MediaType.APPLICATION_JSON))

        val taskResponse = JsonParser.serialize(mapOf("responses" to newResp))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(timezone.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getGroupIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(groups.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(plans.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getLabelCategoriesUrl("", planId)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(planDetails.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(buckets.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(emptytasksResponse.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getAllIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(MockRestResponseCreators.withSuccess(taskResponse, MediaType.APPLICATION_JSON))

        val task = JsonParser.serialize(JsonParser.deserialize<Task>(task1.readAsString()).copy(dueDateTime = date, userId = userId))

        val dueDate = getUserDueDateInUserTimeZone(date, "India Standard Time")

        val transformedObj = transformCardsResponse(response, dueDate, task)

        val jsonResponse = JsonParser.serialize(mapOf("objects" to transformedObj))

        val data = getCardsResponse()

        Assert.assertThat<String>(data, SameJSONAs.sameJSONAs(jsonResponse))
    }

    @Test
    fun `Test Auth Fail For Getting UserTimeZone`(
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource,
            @Value("classpath:service/user/user.json")
            user: Resource
    ) {
        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())

        val jsonResponse = response.readAsString()

        val uri = "/cards/requests"

        webClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(MESSAGE_ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().json(jsonResponse)
    }

    @Test
    fun `Test Auth Fail For Getting UserGroupIds`(
            @Value("classpath:connector/responses/invalid_connector_token.json")
            response: Resource,
            @Value("classpath:service/user/user.json")
            user: Resource,
            @Value("classpath:service/user/timezone.json")
            timezone: Resource
    ) {
        mockBackend.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(Endpoints.getUserIdUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(user.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getUserTimeZoneUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withSuccess(timezone.readAsString(), MediaType.APPLICATION_JSON))

        mockBackend.expect(MockRestRequestMatchers.requestTo(Endpoints.getGroupIdsUrl("")))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, authorization))
                .andRespond(MockRestResponseCreators.withUnauthorizedRequest())

        val jsonResponse = response.readAsString()

        val uri = "/cards/requests"

        webClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, getAuthorizationToken(uri))
                .header(X_AUTH_HEADER, authorization)
                .header(X_BASE_URL_HEADER, mockBackend.url(""))
                .header(MESSAGE_ROUTING_PREFIX, ROUTING_PREFIX_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectHeader().valueEquals("x-backend-status", "401")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody().json(jsonResponse)
    }

    @Test
    fun miscellaneous() {
        val map = mapOf("string" to null)
        val resp = map.getListOrDefault("s", listOf(1, 2))
        Assert.assertEquals(listOf(1, 2), resp)
        val mapResp = map.getMapOrDefault("s", mapOf("sample" to 1))
        Assert.assertEquals(mapOf("sample" to 1), mapResp)
        Assert.assertEquals(null, map.getMapOrNull<String, Any>("j"))
        Assert.assertEquals("default", map.getStringOrDefault("try", "default"))
        try {
            map.getListOrException<String>("f")
        } catch (e: java.lang.Exception) {
            Assert.assertEquals(null, e.localizedMessage)
        }
        try {
            map.getMapOrException<String, Any>("f")
        } catch (e: java.lang.Exception) {
            Assert.assertEquals(null, e.localizedMessage)
        }
        val userNameFromToken = VmwareUtils.getUserEmailFromToken("token s.bnVsbA==")
        Assert.assertEquals(null, userNameFromToken)
        val d = null.toBase64DecodedString()
        Assert.assertEquals(null, d)
    }
}

