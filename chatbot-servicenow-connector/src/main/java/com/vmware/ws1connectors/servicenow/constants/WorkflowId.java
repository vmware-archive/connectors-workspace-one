/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.constants;

import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkflowId {

    CREATE_TASK("vmw_FILE_GENERAL_TICKET"),
    VIEW_TASK("vmw_GET_TICKET_STATUS"),
    ORDER_A_DEVICE("vmw_ORDER_NEW_DEVICE"),
    ORDER_LAPTOP("vmw_ORDER_LAPTOP"),
    ORDER_DESKTOPS("vmw_ORDER_DESKTOP"),
    ORDER_MOBILES("vmw_ORDER_MOBILE"),
    ORDER_TABLETS("vmw_ORDER_TABLET");

    private String workFlowId;

    WorkflowId(String workFlowId) {
        this.workFlowId = workFlowId;
    }

    @JsonValue public String getId() {
        return workFlowId;
    }

}
