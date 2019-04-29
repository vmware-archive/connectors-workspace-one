/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.salesforce;

import javax.validation.constraints.NotBlank;

@SuppressWarnings("PMD.MethodNamingConventions")
public class UpdateNextStepForm {
    @NotBlank
    private String nextStep;

    private String nextStepPreviousValue;

    public String getNextStep() {
        return nextStep;
    }

    public void setNextstep(String nextstep) {
        this.nextStep = nextstep;
    }

    public String getNextStepPreviousValue() {
        return nextStepPreviousValue;
    }

    public void setNextstep_previous_value(String nextStepPreviousValue) {
        this.nextStepPreviousValue = nextStepPreviousValue;
    }
}
