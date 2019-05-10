/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow.forms;

import javax.validation.constraints.NotBlank;

@SuppressWarnings("PMD.MethodNamingConventions")
public class CreateTaskForm {

    @NotBlank
    private String type;

    @NotBlank
    private String shortDescription;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShort_description(String shortDescription) {
        this.shortDescription = shortDescription;
    }
}
