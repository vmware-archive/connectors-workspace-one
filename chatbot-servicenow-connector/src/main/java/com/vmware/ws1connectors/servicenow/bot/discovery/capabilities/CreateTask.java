/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.bot.discovery.capabilities;

import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.utils.BotTextAccessor;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.constants.WorkflowId;
import com.vmware.ws1connectors.servicenow.domain.BotAction;
import com.vmware.ws1connectors.servicenow.domain.BotActionUserInput;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.utils.UriBuilderUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Locale;

public class CreateTask implements BotCapability {
    private final BotTextAccessor botTextAccessor;
    private final String appContextPath;
    private static final int DESCRIPTION_MIN_LENGTH = 1;
    private static final int DESCRIPTION_MAX_LENGTH = 160;
    private static final String SHORT_DESCRIPTION = "shortDescription";
    private static final String TEXT_AREA = "textarea";

    public CreateTask(BotTextAccessor botTextAccessor, String appContextPath) {
        this.botTextAccessor = botTextAccessor;
        this.appContextPath = appContextPath;
    }

    @Override public BotItem describe(String taskType, String routingPrefix, Locale locale) {
        return new BotItem.Builder()
                .setTitle(botTextAccessor.getMessage("createTaskObject.title", locale))
                .setDescription(botTextAccessor.getMessage("createTaskObject.description", locale))
                .setWorkflowId(WorkflowId.CREATE_TASK.getId())
                .addAction(getCreateTaskAction(taskType, routingPrefix, locale))
                .setWorkflowStep(WorkflowStep.COMPLETE)
                .build();
    }

    private BotAction getCreateTaskAction(String taskType, String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(botTextAccessor.getMessage("createTaskAction.title", locale))
                .setDescription(botTextAccessor.getMessage("createTaskAction.description", locale))
                .setType(HttpMethod.POST)
                .setUrl(new Link(UriBuilderUtils.createConnectorContextUrl(routingPrefix, appContextPath) + ServiceNowConstants.URL_PATH_SEPERATOR + ServiceNowConstants.CREATE_TASK_URL))
                .setRequestParam("type", taskType)
                .setRequestHeaders(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .setUserInput(getTicketDescriptionUserInput(locale))
                .build();
    }

    private BotActionUserInput getTicketDescriptionUserInput(Locale locale) {
        return new BotActionUserInput.Builder()
                .setId(SHORT_DESCRIPTION)
                .setFormat(TEXT_AREA)
                .setLabel(botTextAccessor.getMessage("createTaskAction.shortDescription.label", locale))
                // MinLength is unnecessary from ServiceNow point of view.
                // API allows to create without any description.
                .setMinLength(DESCRIPTION_MIN_LENGTH)
                // I see it as 160 characters in the ServiceNow dev instance.
                // If you try with more than limit, ServiceNow would just ignore whatever is beyond 160.
                .setMaxLength(DESCRIPTION_MAX_LENGTH)
                .build();
    }
}
