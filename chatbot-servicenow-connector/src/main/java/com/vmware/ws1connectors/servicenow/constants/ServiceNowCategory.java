/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.constants;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ServiceNowCategory {

    DESKTOP("Desktops"), LAPTOP("Laptops"), MOBILE("Mobiles"), TABLET("Tablets");
    private String categoryName;

    ServiceNowCategory(String categoryName) {
        this.categoryName = categoryName;
    }

    @JsonValue public String getCategoryName() {
        return categoryName;
    }

    public static boolean contains(String categoryName) {
        for (ServiceNowCategory categoryEnum : ServiceNowCategory
                .values()) {
            if (categoryEnum.getCategoryName().equalsIgnoreCase(categoryName)) {
                return true;
            }
        }
        return false;
    }

    public static ServiceNowCategory fromString(String category) {
        for (ServiceNowCategory categoryEnum : ServiceNowCategory
                .values()) {
            if (categoryEnum.getCategoryName().equalsIgnoreCase(category)) {
                return categoryEnum;
            }
        }
        return null;
    }
}