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
import org.springframework.http.HttpMethod;

import java.util.Locale;

public class OrderDevice implements BotCapability {

    private static final String ORDER_DEVICE = "orderDevice";
    private static final String ORDER_DEVICE_ACTION = "orderDeviceAction";
    private final ConnectorTextAccessor connectorTextAccessor;

    public OrderDevice(ConnectorTextAccessor connectorTextAccessor) {
        this.connectorTextAccessor = connectorTextAccessor;
    }

    @Override public BotItem describe(String routingPrefix, Locale locale) {
        return new BotItem.Builder()
                .setTitle(connectorTextAccessor.getTitle(ORDER_DEVICE, locale))
                .setDescription(connectorTextAccessor.getDescription(ORDER_DEVICE, locale))
                .setWorkflowId(WorkflowId.ORDER_A_DEVICE.getId())
                .addAction(viewDeviceCategoryAction(routingPrefix, locale))
                .setWorkflowStep(WorkflowStep.COMPLETE)
                .build();
    }

    private BotAction viewDeviceCategoryAction(String routingPrefix, Locale locale) {
        return new BotAction.Builder()
                .setTitle(connectorTextAccessor.getTitle(ORDER_DEVICE_ACTION, locale))
                .setDescription(connectorTextAccessor.getDescription(ORDER_DEVICE_ACTION, locale))
                .setType(HttpMethod.GET)
                .setUrl(new Link(routingPrefix + ServiceNowConstants.DEVICE_CATEGORY_URL))
                .build();
    }
}
