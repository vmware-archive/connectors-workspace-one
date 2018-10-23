/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.hub.servicenow;

import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

class Request {

    private final String number;
    private final String totalPrice;

    Request(
            String number,
            String totalPrice
    ) {
        this.number = number;
        this.totalPrice = totalPrice;
    }

    String getNumber() {
        return number;
    }

    String getTotalPrice() {
        return totalPrice;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
