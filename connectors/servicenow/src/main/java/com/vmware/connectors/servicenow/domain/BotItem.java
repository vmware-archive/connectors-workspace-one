/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vmware.connectors.common.payloads.response.Link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class BotItem {

    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("context_id")
    private String contextId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("short_description")
    private String shortDescription;

    @JsonProperty("description")
    private String description;

    @JsonProperty("image")
    private Link image;

    @JsonInclude(NON_EMPTY)
    @JsonProperty("actions")
    private final List<BotAction> actions;

    @JsonInclude(NON_EMPTY)
    @JsonProperty("children")
    private final List<BotItem> children;

    @JsonProperty("workflow_id")
    private String workflowId;

    public UUID getId() {
        return id;
    }

    public String getContextId() {
        return contextId;
    }

    public String getTitle() {
        return title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public Link getImage() {
        return image;
    }

    public List<BotAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public List<BotItem> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public String getWorkflowId() {
        return workflowId;
    }

    private BotItem() {
        this.id = UUID.randomUUID();
        this.actions = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public static class Builder {

        private BotItem botItem;

        public Builder() {
            botItem = new BotItem();
        }

        private void reset() {
            botItem = new BotItem();
        }

        public Builder setContextId(String contextId) {
            botItem.contextId = contextId;
            return this;
        }

        public Builder setTitle(String title) {
            botItem.title = title;
            return this;
        }

        public Builder setShortDescription(String shortDescription) {
            botItem.shortDescription = shortDescription;
            return this;
        }

        public Builder setDescription(String description) {
            botItem.description = description;
            return this;
        }

        public Builder setImage(Link imageLink) {
            botItem.image = imageLink;
            return this;
        }

        public Builder addAction(BotAction action) {
            botItem.actions.add(action);
            return this;
        }

        public Builder addChild(BotItem childObject) {
            botItem.children.add(childObject);
            return this;
        }

        public Builder setWorkflowId(String workflowId) {
            botItem.workflowId = workflowId;
            return this;
        }

        public BotItem build() {
            BotItem builtObject = botItem;
            reset();
            return builtObject;
        }
    }
}
