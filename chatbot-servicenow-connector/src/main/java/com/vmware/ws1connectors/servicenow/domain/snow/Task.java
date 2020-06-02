/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.domain.snow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {

    private String number;

    private String createdOn;

    private String shortDescription;

    private String sysId;

    private String state;

    private String impact;

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public String getNumber() {
        return number;
    }

    @JsonProperty("number")
    public void setNumber(String number) {
        this.number = number;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    @JsonProperty("sys_created_on")
    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    @JsonProperty("short_description")
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getSysId() {
        return sysId;
    }

    @JsonProperty("sys_id")
    public void setSysId(String sysId) {
        this.sysId = sysId;
    }

    public String getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }
}
