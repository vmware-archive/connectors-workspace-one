/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Created by Rob Worsnop on 10/21/16.
 */
@JsonInclude(NON_NULL)
public class ConnectorStatus {
    private final String connector;
    private final HttpStatus status;
    private final HttpStatus backendStatus;

    @JsonCreator
    public ConnectorStatus(@JsonProperty("connector") String connector,
                           @JsonProperty("status") HttpStatus status,
                           @JsonProperty("backend_status") HttpStatus backendStatus) {
        this.connector = connector;
        this.status = status;
        this.backendStatus = backendStatus;
    }

    @JsonProperty("connector")
    public String getConnector() {
        return connector;
    }

    @JsonProperty("status")
    public int getStatus() {
        return status.value();
    }

    @JsonProperty("backend_status")
    public Integer getBackendStatus() {
        return backendStatus == null ? null : backendStatus.value();
    }
}
