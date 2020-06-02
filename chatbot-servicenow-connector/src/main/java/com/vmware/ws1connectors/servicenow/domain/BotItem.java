/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.utils.ArgumentUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class BotItem {

    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("contextId")
    private String contextId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("subtitle")
    private String subtitle;

    @JsonProperty("shortDescription")
    private String shortDescription;

    @JsonProperty("description")
    private String description;

    @JsonProperty("image")
    private Link image;

    @JsonProperty("url")
    private Link url;

    @JsonProperty("type")
    private String type;

    @JsonInclude(NON_EMPTY)
    @JsonProperty("actions")
    private final List<BotAction> actions;

    @JsonInclude(NON_EMPTY)
    @JsonProperty("children")
    private final List<Map<String, BotItem>> children;

    @JsonProperty("workflowId")
    private String workflowId;

    @JsonProperty("workflowStep")
    private WorkflowStep workflowStep;

    @JsonProperty("tabularData")
    private List<TabularData> tabularDataList;

    public List<TabularData> getTabularDataList() {
        return tabularDataList;
    }

    public void setTabularDataList(List<TabularData> tabularDataList) {
        this.tabularDataList = tabularDataList;
    }

    public WorkflowStep getWorkflowStep() {
        return workflowStep;
    }

    public UUID getId() {
        return id;
    }

    public String getContextId() {
        return contextId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
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

    public Link getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public List<BotAction> getActions() {
        return List.copyOf(actions);
    }

    public List<Map<String, BotItem>> getChildren() {
        return List.copyOf(children);
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

        public Builder setSubtitle(String subtitle) {
            botItem.subtitle = subtitle;
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

        // Its a link to the resource at the backend. Ex - ServiceNow ticket link.
        public Builder setUrl(Link resourceUrl) {
            botItem.url = resourceUrl;
            return this;
        }

        // It is a hint for the UI layer, to determine about how to layout the data.
        public Builder setType(String uiType) {
            botItem.type = uiType;
            return this;
        }

        public Builder addAction(BotAction action) {
            botItem.actions.add(action);
            return this;
        }

        public Builder addChild(BotItem childObject) {
            Map<String, BotItem> botObjectMap = new HashMap<>();
            botObjectMap.put(ServiceNowConstants.ITEM_DETAILS, childObject);
            botItem.children.add(botObjectMap);
            return this;
        }

        public Builder addTabularData(TabularData tabularData) {
            ArgumentUtils.checkArgumentNotNull(tabularData, "tabularData");
            if (botItem.tabularDataList == null) {
                botItem.tabularDataList = new ArrayList<>();
            }
            botItem.tabularDataList.add(tabularData);
            return this;
        }

        public Builder setWorkflowId(String workflowId) {
            botItem.workflowId = workflowId;
            return this;
        }

        public Builder setWorkflowStep(WorkflowStep workflowStep) {
            botItem.workflowStep = workflowStep;
            return this;
        }

        public BotItem build() {
            BotItem builtObject = botItem;
            reset();
            return builtObject;
        }
    }
}
