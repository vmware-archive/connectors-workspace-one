/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import com.vmware.connectors.test.ControllerTestsBase;

import static org.assertj.core.api.Assertions.assertThat;

class WorkdayConnectorApplicationTest extends ControllerTestsBase {
    @Autowired private ApplicationContext applicationContext;

    @Test void contextLoadsForWorkdayConnectorApp() {
        assertThat(applicationContext.getId()).isNotBlank();
    }
}
