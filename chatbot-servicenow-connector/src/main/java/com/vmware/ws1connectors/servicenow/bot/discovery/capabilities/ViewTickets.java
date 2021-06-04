/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.bot.discovery.capabilities;

import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.constants.WorkflowId;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.domain.BotAction;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Locale;

import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.VIEW_TASK_TYPE;

@Slf4j
public class ViewTickets implements BotCapability {
    private static final String VIEW_TASK_OBJECT = "viewTaskObject";
    private static final String VIEW_TASK_ACTION = "viewTaskAction";
    private final ConnectorTextAccessor connectorTextAccessor;
    private String taskType;

    public ViewTickets(ConnectorTextAccessor connectorTextAccessor, String taskType) {
        this.connectorTextAccessor = connectorTextAccessor;
        this.taskType = taskType;
    }

    @Override public BotItem describe(String routingPrefix, Locale locale) {
        LOGGER.trace("getBotDiscovery viewTicket object. routingPrefix: {}, viewTaskType: {}", routingPrefix, taskType);
        if (StringUtils.isBlank(taskType)) {
            LOGGER.debug("Table name isn't specified for ticket viewing flow. Taking `task` as default type.");
            taskType = VIEW_TASK_TYPE;
        }
        return new BotItem.Builder()
                .setTitle(connectorTextAccessor.getTitle(VIEW_TASK_OBJECT, locale))
                .setDescription(connectorTextAccessor.getDescription(VIEW_TASK_OBJECT, locale))
                .setWorkflowId(WorkflowId.VIEW_TASK.getId())
                .setWorkflowStep(WorkflowStep.COMPLETE)
                .addAction(getViewMyTaskAction(taskType, routingPrefix, locale))
                .setWorkflowStep(WorkflowStep.COMPLETE)
                .build();
    }

    private BotAction getViewMyTaskAction(String taskType, String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(connectorTextAccessor.getTitle(VIEW_TASK_ACTION, locale))
                .setDescription(connectorTextAccessor.getDescription(VIEW_TASK_ACTION, locale))
                .setType(HttpMethod.POST)
                .setRequestParam("type", taskType)
                .setUrl(new Link(routingPrefix + ServiceNowConstants.VIEW_TASK_URL))
                .setRequestHeaders(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }
}
