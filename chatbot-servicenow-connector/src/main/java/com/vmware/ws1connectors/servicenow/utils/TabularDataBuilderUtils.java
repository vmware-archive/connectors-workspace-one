/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.utils;

import com.vmware.connectors.common.payloads.response.Link;
import com.vmware.ws1connectors.servicenow.domain.TabularData;
import com.vmware.ws1connectors.servicenow.domain.TabularDataItem;
import com.vmware.ws1connectors.servicenow.domain.snow.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.IMPACT;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.SHORT_DESCRIPTION;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.STATUS;
import static com.vmware.ws1connectors.servicenow.constants.ServiceNowConstants.TICKET_NO;

public final class TabularDataBuilderUtils {

    private static final String BASE_URL = "BASE_URL";
    private static final String TASK_OBJ = "TASK_OBJ";
    private static final String VALIDATOR_MSG = "Cannot be Null or blank: ";
    private static final Link NO_URL = null;

    private TabularDataBuilderUtils() { }

    private static TabularDataItem buildTabularDataItem(String title, String shortDescription, Link url) {
        return TabularDataItem.builder()
                .title(title)
                .shortDescription(shortDescription)
                .url(url)
                .build();
    }

    public static TabularData buildTabularDataForTask(Task task, String baseUrl) {
        ArgumentUtils.checkArgumentNotBlank(baseUrl, BASE_URL);
        ArgumentUtils.checkArgumentNotNull(task, TASK_OBJ);
        checkArgument(Objects.nonNull(task), new StringBuilder(VALIDATOR_MSG).append(task).toString());
        List<TabularDataItem> tabularDataItems = new ArrayList<>();
        tabularDataItems.add(TabularDataBuilderUtils.buildTabularDataItem(IMPACT, task.getImpact(), NO_URL));
        tabularDataItems.add(TabularDataBuilderUtils.buildTabularDataItem(STATUS, task.getState(), NO_URL));
        tabularDataItems.add(
                TabularDataBuilderUtils.buildTabularDataItem(SHORT_DESCRIPTION, task.getShortDescription(), NO_URL));
        tabularDataItems.add(
                TabularDataBuilderUtils.buildTabularDataItem(TICKET_NO, task.getNumber(), UriBuilderUtils.getTaskUrl(baseUrl, task)));
        return TabularData.builder().tabularDataItems(tabularDataItems).build();
    }

}
