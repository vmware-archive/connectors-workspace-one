/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow.forms;

import javax.validation.constraints.NotBlank;

@SuppressWarnings("PMD.MethodNamingConventions")
public class DeleteFromCartForm {

    @NotBlank
    private String entryId;

    public String getEntryId() {
        return entryId;
    }

    public void setEntry_id(String entryId) {
        this.entryId = entryId;
    }
}
