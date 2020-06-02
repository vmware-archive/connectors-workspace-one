/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.bot.discovery.capabilities;

import com.vmware.ws1connectors.servicenow.utils.BotTextAccessor;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants;
import com.vmware.ws1connectors.servicenow.constants.ServiceNowCategory;
import com.vmware.ws1connectors.servicenow.constants.WorkflowId;
import com.vmware.ws1connectors.servicenow.domain.BotItem;

import java.util.Locale;

public interface BotCapability {

    @SuppressWarnings("PMD.NcssCount")
    static BotCapability build(WorkflowId workflowId, BotTextAccessor botTextAccessor, String appContextPath) {
        switch (workflowId) {
            case VIEW_TASK: return new ViewTickets(botTextAccessor, appContextPath);
            case CREATE_TASK: return new CreateTask(botTextAccessor, appContextPath);
            case ORDER_A_DEVICE: return new OrderDevice(botTextAccessor, appContextPath);
            case ORDER_LAPTOP: return new OrderLaptop(botTextAccessor, appContextPath);
            case ORDER_MOBILES: return new OrderMobile(botTextAccessor, appContextPath);
            case ORDER_DESKTOPS: return new OrderDesktop(botTextAccessor, appContextPath);
            case ORDER_TABLETS: return new OrderTablet(botTextAccessor, appContextPath);
            default: return null;
        }
    }

    default String buildActionUrl(ServiceNowCategory categoryEnum, String appContextPath) {
        return new StringBuilder(appContextPath)
                .append(ServiceNowConstants.URL_PATH_SEPERATOR)
                .append(ServiceNowConstants.DEVICE_LIST_URL)
                .append("?")
                .append(ServiceNowConstants.DEVICE_CATEGORY)
                .append("=")
                .append(categoryEnum.getCategoryName())
                .append("&limit=")
                .append(ServiceNowConstants.ITEM_BY_CATEGORY_LIMIT)
                .append("&offset=").append(ServiceNowConstants.ITEM_BY_CATEGORY_OFFSET).toString();
    }

    BotItem describe(String taskType, String routingPrefix, Locale locale);
}
