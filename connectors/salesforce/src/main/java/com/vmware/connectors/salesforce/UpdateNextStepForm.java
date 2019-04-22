/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.salesforce;

import javax.validation.constraints.NotBlank;

public class UpdateNextStepForm {
    @NotBlank
    private String nextStep;

    public String getNextStep() {
        return nextStep;
    }

    public void setNextstep(String nextStep) {
        this.nextStep = nextStep;
    }
}
