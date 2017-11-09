/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A utility class to hold the objects that are used by every request in the chain;
// primarily exists to streamline method signatures
public class SocialcastRequestContext {

    private static final ObjectWriter JSON_WRITER = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY).writer();
    @JsonProperty("results_data")
    private final Progress resultsData;
    @JsonProperty("sc_base_url")
    private final String scBaseUrl;
    @JsonProperty("headers")
    private final HttpHeaders headers;
    private MessageThread mt;
    @JsonProperty("group_id")
    private String groupId;


    public SocialcastRequestContext(String scBaseUrl, HttpHeaders headers) {
        this.scBaseUrl = scBaseUrl;
        this.headers = headers;
        this.resultsData = new Progress();
    }

    public String getScBaseUrl() {
        return scBaseUrl;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public MessageThread getMessageThread() {
        return mt;
    }

    public void setMessageThread(MessageThread mt) {
        this.mt = mt;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void addResponseCodeForStep(String stepName, String statusCode) {
        resultsData.stepwiseResponseCodes.put(stepName, statusCode);
    }

    public void addUserFound(String email, String scastId) {
        resultsData.usersFound.put(email, scastId);
    }

    public void addUserNotFound(String email) {
        resultsData.usersNotFound.add(email);
    }

    public void setGroupUri(String uri) {
        resultsData.groupUri = uri;
    }

    public void addMessagePosted(String messageUri, String emailId) {
        resultsData.messagesPosted.put(messageUri, emailId);
    }

    public String getResultJson() throws JsonProcessingException {
        return JSON_WRITER.writeValueAsString(resultsData);
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static class Progress {
        @JsonProperty("stepwise_response_codes")
        private final Map<String, String> stepwiseResponseCodes = new HashMap<>();
        @JsonProperty("users_found")
        private final Map<String, String> usersFound = new HashMap<>();
        @JsonProperty("users_not_found")
        private final List<String> usersNotFound = new ArrayList<>();
        @JsonProperty("messages_posted")
        private final Map<String, String> messagesPosted = new HashMap<>();
        @JsonProperty("group_uri")
        private String groupUri;
    }

}
