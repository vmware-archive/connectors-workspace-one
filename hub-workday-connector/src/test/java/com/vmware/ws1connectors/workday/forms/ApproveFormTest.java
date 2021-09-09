/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.forms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApproveFormTest {

    private static final String COMMENT = "comment";
    private static final String TENANT_NAME = "vmware_gms";

    private ApproveForm approveForm;

    @BeforeEach
    void setup() {
        approveForm = new ApproveForm(COMMENT, TENANT_NAME);
    }

    @Test
    void returnsComment() {
        assertThat(approveForm.getComment()).isEqualTo(COMMENT);
    }

    @Test
    void returnsTenantName() {
        assertThat(approveForm.getTenantName()).isEqualTo(TENANT_NAME);
    }
}
