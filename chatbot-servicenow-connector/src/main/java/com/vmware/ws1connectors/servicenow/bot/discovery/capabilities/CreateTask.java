/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.bot.discovery.capabilities;

import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.ws1connectors.servicenow.constants.WorkflowId;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.domain.BotAction;
import com.vmware.ws1connectors.servicenow.domain.BotActionUserInput;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Locale;

import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.CONFIRM_CREATE_TASK_URL;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.CREATE_TASK_TYPE;

@Slf4j
public class CreateTask implements BotCapability {
    private static final String CREATE_TASK_OBJECT = "createTaskObject";
    private static final String CREATE_TASK_ACTION_CONFIRM = "createTaskAction.confirm";
    private static final String CREATE_TASK_ACTION_SHORT_DESCRIPTION = "createTaskAction.shortDescription";
    private final ConnectorTextAccessor connectorTextAccessor;
    private static final int DESCRIPTION_MIN_LENGTH = 1;
    private static final int DESCRIPTION_MAX_LENGTH = 160;
    private static final String SHORT_DESCRIPTION = "shortDescription";
    private static final String TEXT_AREA = "textarea";
    private static final String TASK_TYPE_PARAM = "type";
    private String taskType;

    public CreateTask(ConnectorTextAccessor connectorTextAccessor, String taskType) {
        this.connectorTextAccessor = connectorTextAccessor;
        this.taskType = taskType;
    }

    @Override public BotItem describe(String routingPrefix, Locale locale) {
        LOGGER.trace("getBotDiscovery createTicket object. routingPrefix: {}, createTaskType: {}", routingPrefix, taskType);
        if (StringUtils.isBlank(taskType)) {
            LOGGER.debug("Table name isn't specified for ticket filing flow. Taking incident as default type.");
            taskType = CREATE_TASK_TYPE;
        }
        return new BotItem.Builder()
                .setTitle(connectorTextAccessor.getTitle(CREATE_TASK_OBJECT, locale))
                .setDescription(connectorTextAccessor.getDescription(CREATE_TASK_OBJECT, locale))
                .setWorkflowId(WorkflowId.CREATE_TASK.getId())
                .addAction(getCreateTaskAction(taskType, routingPrefix, locale))
                .setWorkflowStep(WorkflowStep.COMPLETE)
                .build();
    }

    private BotAction getCreateTaskAction(String taskType, String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(connectorTextAccessor.getTitle(CREATE_TASK_ACTION_CONFIRM, locale))
                .setDescription(connectorTextAccessor.getDescription(CREATE_TASK_ACTION_CONFIRM, locale))
                .setType(HttpMethod.POST)
                .setUrl(new Link(routingPrefix + CONFIRM_CREATE_TASK_URL))
                .setRequestParam(TASK_TYPE_PARAM, taskType)
                .setRequestHeaders(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .setUserInput(getTicketDescriptionUserInput(locale))
                .build();
    }

    private BotActionUserInput getTicketDescriptionUserInput(Locale locale) {
        return new BotActionUserInput.Builder()
                .setId(SHORT_DESCRIPTION)
                .setFormat(TEXT_AREA)
                .setLabel(connectorTextAccessor.getActionLabel(CREATE_TASK_ACTION_SHORT_DESCRIPTION, locale))
                // MinLength is unnecessary from ServiceNow point of view.
                // API allows to create without any description.
                .setMinLength(DESCRIPTION_MIN_LENGTH)
                // I see it as 160 characters in the ServiceNow dev instance.
                // If you try with more than limit, ServiceNow would just ignore whatever is beyond 160.
                .setMaxLength(DESCRIPTION_MAX_LENGTH)
                .build();
    }
}
