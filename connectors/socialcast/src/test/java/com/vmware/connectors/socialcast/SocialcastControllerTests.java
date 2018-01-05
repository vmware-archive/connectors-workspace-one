/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.socialcast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.connectors.common.model.MessageThread;
import com.vmware.connectors.common.model.UserRecord;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.connectors.test.JsonSchemaValidator;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.AsyncRestTemplate;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class SocialcastControllerTests extends ControllerTestsBase {

    private static final String AUTHORIZATION_HEADER = "Basic abc";

    private static final String URL_BASE = "https://roswell.socialcast.com";

    // JSON for a "normal" request to the Socialcast connector
    private static final String NORMAL_REQUEST_BODY_FILE = "requests/normal-request.json";

    // The expected JSON response from the connector when it correctly handles NORMAL_REQUEST_BODY_FILE
    private static final String NORMAL_CONNECTOR_RESPONSE_FILE = "responses/normal-connector-response.json";

    // JSON for an acceptable but slightly different request to the Socialcast connector
    private static final String MISSING_SENDER_NAME_REQUEST_BODY_FILE = "requests/missing-sender-name.json";

    // The expected JSON response from the connector when it correctly handles MISSING_SENDER_NAME_REQUEST_BODY_FILE
    private static final String MISSING_SENDER_NAME_CONNECTOR_RESPONSE_FILE = "responses/missing-sender-name-connector-response.json";

    // The expected response from the Socialcast service for a user query
    private static final String USER_QUERY_RESPONSE_FILE = "responses/user-query.json";

    // The expected response from the Socialcast service for a group creation request
    private static final String GROUP_CREATION_RESPONSE_FILE = "responses/group-creation.json";

    // The expected response from the Socialcast service for a user addition request
    private static final String USER_ADDITION_RESPONSE_FILE = "responses/user-addition.json";

    // The expected response from the Socialcast service for a message posting request
    private static final String MESSAGE_CREATION_RESPONSE_FILE = "responses/message-creation.json";

    @Autowired
    private AsyncRestTemplate rest;

    private MockRestServiceServer mockSocialcastService;

    private final ObjectMapper mapper = new ObjectMapper();


    @Before
    public void setup() throws Exception {
        super.setup();
        // Create mock service
        mockSocialcastService = MockRestServiceServer.bindTo(rest).build();
    }

    @Test
    public void testProtectedResource() throws Exception {
        testProtectedResource(POST, "/conversations");
    }

    @Test
    public void testDiscovery() throws Exception {
        testConnectorDiscovery();
    }

    @Test
    public void testMissingRequestHeaders() throws Exception {
        perform(post("/conversations").with(token(getAccessToken()))
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .header("x-routing-prefix", "https://hero/connectors/socialcast/")
                .content(fromFile("/requests/normal-request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("Missing request header 'x-socialcast-authorization'")));
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void testNormalMessageThreadPosting() throws Exception {

        // Set up mock service
        mockSocialcastService.reset();
        expectSuccessFromUserQuery();
        expectSuccessFromGroupCreation();
        expectSuccessFromUserAddition();
        expectSuccessFromMessagePosting(3);

        String expectedResponseJSON = fromFile(NORMAL_CONNECTOR_RESPONSE_FILE);

        performTestWithRequestBody(NORMAL_REQUEST_BODY_FILE,
                status().isCreated(), content().json(expectedResponseJSON));
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void testMessageThreadPostingWithMissingSenderName() throws Exception {

        // Set up mock service
        mockSocialcastService.reset();
        expectSuccessFromUserQuery();
        expectSuccessFromGroupCreation();
        expectSuccessFromUserAddition();
        expectSuccessFromMessagePosting(3);

        String missingSenderNameJSON = fromFile(MISSING_SENDER_NAME_CONNECTOR_RESPONSE_FILE);

        performTestWithRequestBody(MISSING_SENDER_NAME_REQUEST_BODY_FILE,
                status().isCreated(), content().json(missingSenderNameJSON));
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void testErrorInUserQuery() throws Exception {

        // Set up mock service
        mockSocialcastService.reset();
        expectErrorFromUserQuery();
        expectSuccessFromGroupCreation();

        performTestWithRequestBody(NORMAL_REQUEST_BODY_FILE,
                status().is5xxServerError(), header().string("X-Backend-Status", "500"));
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void testErrorInGroupCreation() throws Exception {

        // Set up mock service
        mockSocialcastService.reset();
        expectSuccessFromUserQuery();
        expectErrorFromGroupCreation();

        performTestWithRequestBody(NORMAL_REQUEST_BODY_FILE, status().is5xxServerError());
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void testErrorInUserAddition() throws Exception {

        // Set up mock service
        mockSocialcastService.reset();
        expectSuccessFromUserQuery();
        expectSuccessFromGroupCreation();
        expectErrorFromUserAddition();

        performTestWithRequestBody(NORMAL_REQUEST_BODY_FILE, status().is5xxServerError());
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void testErrorInFirstMessagePosting() throws Exception {

        // Set up mock service
        mockSocialcastService.reset();
        expectSuccessFromUserQuery();
        expectSuccessFromGroupCreation();
        expectSuccessFromUserAddition();
        expectErrorFromMessagePosting(1);

        performTestWithRequestBody(NORMAL_REQUEST_BODY_FILE, status().is5xxServerError());
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void testErrorInSubsequentMessagePosting() throws Exception {

        // Set up mock service
        mockSocialcastService.reset();
        expectSuccessFromUserQuery();
        expectSuccessFromGroupCreation();
        expectSuccessFromUserAddition();
        expectSuccessFromMessagePosting(1);
        expectErrorFromMessagePosting(1);

        performTestWithRequestBody(NORMAL_REQUEST_BODY_FILE, status().is5xxServerError());
    }

    @Test
    public void testJsonParsing() throws Exception {
        MessageThread mt = MessageThread.parse(fromFile(NORMAL_REQUEST_BODY_FILE));
        assertThat(mt, is(not(nullValue())));
        assertThat(mt.getFirstSubject(), is("Thread Subject"));
        assertThat(mt.getMessages(), hasSize(3));
        assertThat(mt.getMessages().get(2).getSender().getFirstName(), is("Anja"));
    }

    @Test
    public void testFirstLastName() {
        UserRecord rec = new UserRecord();
        assertThat(rec.getFirstName(), is(nullValue()));
        assertThat(rec.getLastName(), is(nullValue()));

        rec.setName("John Smith");
        assertThat(rec.getFirstName(), is("John"));
        assertThat(rec.getLastName(), is("Smith"));

        rec.setName("Blair, Eric");
        assertThat(rec.getFirstName(), is("Eric"));
        assertThat(rec.getLastName(), is("Blair"));
    }

    @Test
    public void testMessageThreadSchema() throws Exception {
        JsonNode msgThreadJson = mapper.readTree(fromFile(NORMAL_REQUEST_BODY_FILE)).at("/data");

        JsonSchemaValidator messageThreadValidator = new JsonSchemaValidator(fromFile("messagethread-schema.json"));

        assertTrue(messageThreadValidator.validate(msgThreadJson));
    }

    @Test
    public void testGetImage() throws Exception {
        perform(get("/images/connector.png"))
                .andExpect(status().isOk())
                .andExpect(header().longValue(CONTENT_LENGTH, 18239))
                .andExpect(header().string(CONTENT_TYPE, IMAGE_PNG_VALUE))
                .andExpect((content().bytes(bytesFromFile("/static/images/connector.png"))));
    }

    private void performTestWithRequestBody(String requestBodyFile, ResultMatcher... expectations) throws Exception {
        String requestBodyJson = fromFile(requestBodyFile);

        MockHttpServletRequestBuilder builder = post("/conversations").with(token(getAccessToken()))
                .contentType(APPLICATION_JSON)
                .header("x-socialcast-authorization", AUTHORIZATION_HEADER)
                .header("x-socialcast-base-url", URL_BASE)
                .content(requestBodyJson);

        ResultActions testRequest = perform(builder);

        for (ResultMatcher expectedResult : expectations) {
            testRequest = testRequest.andExpect(expectedResult);
        }

        mockSocialcastService.verify();
    }

    private void expectSuccessFromUserQuery() throws IOException {
        // Set up response for user search
        mockSocialcastService.expect(requestTo(startsWith(URL_BASE + "/api/users/search.json")))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, AUTHORIZATION_HEADER))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(fromFile(USER_QUERY_RESPONSE_FILE)));
    }

    private void expectErrorFromUserQuery() {
        // Set up response for user search
        mockSocialcastService.expect(requestTo(startsWith(URL_BASE + "/api/users/search.json")))
                .andExpect(method(GET))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, AUTHORIZATION_HEADER))
                .andRespond(withServerError());
    }

    private void expectSuccessFromGroupCreation() throws IOException {
        // Set up response for group creation
        mockSocialcastService.expect(requestTo(URL_BASE + "/api/groups.json"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, AUTHORIZATION_HEADER))
                .andRespond(withStatus(CREATED)
                        .contentType(APPLICATION_JSON)
                        .body(fromFile(GROUP_CREATION_RESPONSE_FILE)));
    }

    private void expectErrorFromGroupCreation() {
        // Set up response for group creation
        mockSocialcastService.expect(requestTo(URL_BASE + "/api/groups.json"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, AUTHORIZATION_HEADER))
                .andRespond(withServerError());
    }

    private void expectSuccessFromUserAddition() throws IOException {
        // Set up response for adding users to group
        mockSocialcastService.expect(requestTo(CoreMatchers.allOf(
                startsWith(URL_BASE + "/api/groups/"),
                CoreMatchers.endsWith("memberships/add_members.json"))))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, AUTHORIZATION_HEADER))
                .andRespond(withStatus(CREATED)
                        .contentType(APPLICATION_JSON)
                        .body(fromFile(USER_ADDITION_RESPONSE_FILE)));
    }

    private void expectErrorFromUserAddition() {
        // Set up response for adding users to group
        mockSocialcastService.expect(requestTo(CoreMatchers.allOf(
                startsWith(URL_BASE + "/api/groups/"),
                CoreMatchers.endsWith("memberships/add_members.json"))))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, AUTHORIZATION_HEADER))
                .andRespond(withServerError());
    }

    private void expectSuccessFromMessagePosting(int howManyTimes) throws IOException {
        // Set up response for posting a message
        // Note that this will send the same response for all three messages in the sample data;
        // this is OK because SocialcastController doesn't look at the ID of the just-posted message in the response
        mockSocialcastService.expect(times(howManyTimes), requestTo(URL_BASE + "/api/messages.json"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, AUTHORIZATION_HEADER))
                .andRespond(withStatus(CREATED)
                        .contentType(APPLICATION_JSON)
                        .body(fromFile(MESSAGE_CREATION_RESPONSE_FILE)));

    }

    private void expectErrorFromMessagePosting(int howManyTimes) {
        // Set up response for posting a message
        mockSocialcastService.expect(times(howManyTimes), requestTo(URL_BASE + "/api/messages.json"))
                .andExpect(method(POST))
                .andExpect(MockRestRequestMatchers.header(AUTHORIZATION, AUTHORIZATION_HEADER))
                .andRespond(withServerError());
    }
}
