/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.bot.discovery.capabilities;

import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.ws1connectors.servicenow.constants.WorkflowStep;
import com.vmware.ws1connectors.servicenow.utils.BotTextAccessor;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowCategory;
import com.vmware.ws1connectors.servicenow.constants.WorkflowId;
import com.vmware.ws1connectors.servicenow.domain.BotAction;
import com.vmware.ws1connectors.servicenow.domain.BotItem;
import com.vmware.ws1connectors.servicenow.utils.UriBuilderUtils;
import org.springframework.http.HttpMethod;

import java.util.Locale;

public class OrderLaptop implements BotCapability {

    private final BotTextAccessor botTextAccessor;
    private final String appContextPath;

    public OrderLaptop(BotTextAccessor botTextAccessor, String appContextPath) {
        this.botTextAccessor = botTextAccessor;
        this.appContextPath = appContextPath;
    }

    @Override public BotItem describe(String taskType, String routingPrefix, Locale locale) {
        return new BotItem.Builder()
                .setTitle(botTextAccessor.getMessage("orderLaptop.title", locale))
                .setDescription(botTextAccessor.getMessage("orderLaptop.description", locale))
                .setWorkflowId(WorkflowId.ORDER_LAPTOP.getId())
                .addAction(getListOfLaptopsAction(routingPrefix, locale))
                .setWorkflowStep(WorkflowStep.COMPLETE)
                .build();
    }

    public BotAction getListOfLaptopsAction(String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(botTextAccessor.getMessage("orderLaptopAction.title", locale))
                .setDescription(botTextAccessor.getMessage("orderLaptopAction.description", locale))
                .setType(HttpMethod.GET)
                .setUrl(new Link(UriBuilderUtils.createConnectorContextUrl(routingPrefix, buildActionUrl(ServiceNowCategory.LAPTOP, appContextPath))))
                .build();
    }
}
