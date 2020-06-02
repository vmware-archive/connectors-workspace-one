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
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.utils.UriBuilderUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Locale;

public class ViewTickets implements BotCapability {
    private final BotTextAccessor botTextAccessor;
    private final String appContextPath;

    public ViewTickets(BotTextAccessor botTextAccessor, String appContextPath) {
        this.botTextAccessor = botTextAccessor;
        this.appContextPath = appContextPath;
    }

    @Override public BotItem describe(String taskType, String routingPrefix, Locale locale) {
        return new BotItem.Builder()
                .setTitle(botTextAccessor.getMessage("viewTaskObject.title", locale))
                .setDescription(botTextAccessor.getMessage("viewTaskObject.description", locale))
                .setWorkflowId(WorkflowId.VIEW_TASK.getId())
                .setWorkflowStep(WorkflowStep.COMPLETE)
                .addAction(getViewMyTaskAction(routingPrefix, locale))
                .setWorkflowStep(WorkflowStep.COMPLETE)
                .build();
    }

    private BotAction getViewMyTaskAction(String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(botTextAccessor.getMessage("viewTaskAction.title", locale))
                .setDescription(botTextAccessor.getMessage("viewTaskAction.description", locale))
                .setType(HttpMethod.POST)
                .setUrl(new Link(UriBuilderUtils.createConnectorContextUrl(routingPrefix, appContextPath) + ServiceNowConstants.URL_PATH_SEPERATOR + ServiceNowConstants.VIEW_TASK_URL))
                .setRequestHeaders(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }
}
