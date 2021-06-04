/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.forms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DeclineFormTest {

    private static final String REASON = "reason";
    private static final String TENANT_NAME = "vmware_gms";

    private DeclineForm declineForm;

    @BeforeEach public void setup() {
        declineForm = new DeclineForm(REASON, TENANT_NAME);
    }

    @Test public void returnsComment() {
        assertThat(declineForm.getReason()).isEqualTo(REASON);
    }

    @Test public void returnsTenantName() {
        assertThat(declineForm.getTenantName()).isEqualTo(TENANT_NAME);
    }
}
