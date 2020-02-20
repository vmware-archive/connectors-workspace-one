/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connector.hub.salesforce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Configuration
@EnableScheduling
@SuppressWarnings("PMD.UseUtilityClass")
public class HubSalesForceConnectorApplication {
    public static void main(final String[] args) {
        SpringApplication.run(HubSalesForceConnectorApplication.class, args);
    }
}
