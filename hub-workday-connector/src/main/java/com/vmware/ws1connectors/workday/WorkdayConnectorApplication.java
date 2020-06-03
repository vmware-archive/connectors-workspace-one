/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.springframework.boot.Banner.Mode.OFF;

@SpringBootApplication
@SuppressWarnings("PMD.UseUtilityClass")
public class WorkdayConnectorApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(WorkdayConnectorApplication.class)
            .bannerMode(OFF)
            .run(args);
    }
}
