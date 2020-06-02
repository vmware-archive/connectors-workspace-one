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

public class OrderMobile implements BotCapability {
    private final BotTextAccessor botTextAccessor;
    private final String appContextPath;

    public OrderMobile(BotTextAccessor botTextAccessor, String appContextPath) {
        this.botTextAccessor = botTextAccessor;
        this.appContextPath = appContextPath;
    }

    @Override public BotItem describe(String taskType, String routingPrefix, Locale locale) {
        return new BotItem.Builder()
                .setTitle(botTextAccessor.getMessage("orderMobile.title", locale))
                .setDescription(botTextAccessor.getMessage("orderMobile.description", locale))
                .setWorkflowId(WorkflowId.ORDER_MOBILES.getId())
                .addAction(getListOfMobilesAction(routingPrefix, locale))
                .setWorkflowStep(WorkflowStep.COMPLETE)
                .build();
    }

    public BotAction getListOfMobilesAction(String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(botTextAccessor.getMessage("orderMobileAction.title", locale))
                .setDescription(botTextAccessor.getMessage("orderMobileAction.description", locale))
                .setType(HttpMethod.GET)
                .setUrl(new Link(UriBuilderUtils.createConnectorContextUrl(routingPrefix, buildActionUrl(ServiceNowCategory.MOBILE, appContextPath))))
                .build();
    }
}
