/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vmware.connectors.common.payloads.response.Link;
import org.springframework.http.HttpMethod;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class BotAction {

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("workflow_id")
    private String workflowId;

    @JsonProperty("type")
    private HttpMethod type;

    @JsonProperty("url")
    private Link url;

    @JsonProperty("payload")
    private final Map<String, String> payload;

    @JsonProperty("user_inputs")
    private final Map<String, String> userInputs;

    private BotAction() {
        this.payload = new HashMap<>();
        this.userInputs = new HashMap<>();
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public HttpMethod getType() {
        return type;
    }

    public Link getUrl() {
        return url;
    }

    public Map<String, String> getPayload() {
        return Collections.unmodifiableMap(payload);
    }

    public Map<String, String> getUserInputs() {
        return Collections.unmodifiableMap(userInputs);
    }

    public static class Builder {

        private BotAction botAction;

        public Builder() {
            botAction = new BotAction();
        }

        private void reset() {
            botAction = new BotAction();
        }

        public Builder setTitle(String title) {
            botAction.title = title;
            return this;
        }

        public Builder setDescription(String description) {
            botAction.description = description;
            return this;
        }

        public Builder setWorkflowId(String workflowId) {
            botAction.workflowId = workflowId;
            return this;
        }

        public Builder setType(HttpMethod methodType) {
            botAction.type = methodType;
            return this;
        }

        public Builder setUrl(Link actionUrl) {
            botAction.url = actionUrl;
            return this;
        }

        public Builder addReqParam(String key, String value) {
            botAction.payload.put(key, value);
            return this;
        }

        public Builder addUserInputParam(String key, String msgLabel) {
            botAction.userInputs.put(key, msgLabel);
            return this;
        }

        public BotAction build() {
            BotAction builtAction = botAction;
            reset();
            return builtAction;
        }
    }
}
