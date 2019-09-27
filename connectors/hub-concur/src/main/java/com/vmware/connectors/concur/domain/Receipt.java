/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpMethod;

public class Receipt {

    private HttpMethod type;

    @JsonProperty("report_url")
    private String reportUrl;

    public Receipt() {
        // public method
    }

    public Receipt(String reportUrl) {
        this.type = HttpMethod.GET;
        this.reportUrl = reportUrl;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public HttpMethod getType() {
        return type;
    }

    public void setType(HttpMethod type) {
        this.type = type;
    }

}
