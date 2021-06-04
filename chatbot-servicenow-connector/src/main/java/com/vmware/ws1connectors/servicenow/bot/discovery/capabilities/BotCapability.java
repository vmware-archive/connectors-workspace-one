/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.bot.discovery.capabilities;

import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowCategory;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowTableName;
import com.vmware.ws1connectors.servicenow.constants.WorkflowId;
import com.vmware.ws1connectors.servicenow.domain.BotItem;

import java.util.Locale;
import java.util.Map;

public interface BotCapability {

    @SuppressWarnings("PMD.NcssCount")
    static BotCapability build(WorkflowId workflowId, ConnectorTextAccessor connectorTextAccessor, Map<String, String> connectorConfigMap) {
        switch (workflowId) {
            case VIEW_TASK: return new ViewTickets(connectorTextAccessor, connectorConfigMap.get(ServiceNowTableName.VIEW_TICKET_TABLE_NAME.getServiceNowTableName()));
            case CREATE_TASK: return new CreateTask(connectorTextAccessor, connectorConfigMap.get(ServiceNowTableName.CREATE_TICKET_TABLE_NAME.getServiceNowTableName()));
            case ORDER_A_DEVICE: return new OrderDevice(connectorTextAccessor);
            case ORDER_LAPTOP: return new OrderLaptop(connectorTextAccessor);
            case ORDER_MOBILES: return new OrderMobile(connectorTextAccessor);
            case ORDER_DESKTOPS: return new OrderDesktop(connectorTextAccessor);
            case ORDER_TABLETS: return new OrderTablet(connectorTextAccessor);
            default: return null;
        }
    }

    default String buildActionUrl(ServiceNowCategory categoryEnum) {
        return new StringBuilder()
                .append(ServiceNowConstants.DEVICE_LIST_URL)
                .append("?")
                .append(ServiceNowConstants.DEVICE_CATEGORY)
                .append("=")
                .append(categoryEnum.getCategoryName())
                .append("&limit=")
                .append(ServiceNowConstants.ITEM_BY_CATEGORY_LIMIT)
                .append("&offset=").append(ServiceNowConstants.ITEM_BY_CATEGORY_OFFSET).toString();
    }

    BotItem describe(String routingPrefix, Locale locale);
}
