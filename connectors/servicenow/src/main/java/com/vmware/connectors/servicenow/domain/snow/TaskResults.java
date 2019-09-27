/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow.domain.snow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskResults {

    private final List<Task> result;

    public TaskResults(@JsonProperty("result") List<Task> result) {
        this.result = result;
    }

    @JsonProperty("result")
    public List<Task> getResult() {
        return result;
    }

}
