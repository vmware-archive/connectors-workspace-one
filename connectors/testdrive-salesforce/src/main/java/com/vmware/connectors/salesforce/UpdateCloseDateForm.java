/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.salesforce;

import javax.validation.constraints.NotBlank;

public class UpdateCloseDateForm {
    @NotBlank
    private String closeDate;

    public String getCloseDate() {
        return closeDate;
    }

    public void setClosedate(String closeDate) {
        this.closeDate = closeDate;
    }
}
