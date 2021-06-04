/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.constants;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

public enum ServiceNowTableName {
    CREATE_TICKET_TABLE_NAME("createTicketTableName"), VIEW_TICKET_TABLE_NAME("viewTicketTableName");
    private String serviceNowTableName;

    ServiceNowTableName(String serviceNowTaskType) {
        this.serviceNowTableName = serviceNowTaskType;
    }

    @JsonValue public String getServiceNowTableName() {
        return serviceNowTableName;
    }

    public static Optional<ServiceNowTableName> fromServiceNowTableName(String serviceNowTableName) {
        return Arrays
                .stream(ServiceNowTableName.values())
                .filter(tableName -> tableName.getServiceNowTableName().equalsIgnoreCase(serviceNowTableName))
                .findFirst();
    }

}
