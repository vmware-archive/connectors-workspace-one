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

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class BotAction {

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    // ToDo: Remove Workflow id from actions ?
    // Recent schema update doesn't allow it; but it seems to be needed in the catalog flow.
    @JsonProperty("workflowId")
    private String workflowId;

    @JsonProperty("type")
    private HttpMethod type;

    @JsonProperty("url")
    private Link url;

    @JsonProperty("payload")
    private final Map<String, String> payload;

    @JsonProperty("headers")
    private final Map<String, String> headers;

    @JsonProperty("userInput")
    private final List<BotActionUserInput> userInput;

    private BotAction() {
        this.payload = new HashMap<>();
        this.headers = new HashMap<>();
        this.userInput = new ArrayList<>();
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
        return Map.copyOf(payload);
    }

    public Map<String, String> getHeaders() {
        return Map.copyOf(headers);
    }

    public List<BotActionUserInput> getUserInput() {
        return List.copyOf(userInput);
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

        public Builder addReqHeader(String key, String value) {
            botAction.headers.put(key, value);
            return this;
        }

        public Builder addUserInput(BotActionUserInput userInput) {
            botAction.userInput.add(userInput);
            return this;
        }

        public BotAction build() {
            BotAction builtAction = botAction;
            reset();
            return builtAction;
        }
    }
}
