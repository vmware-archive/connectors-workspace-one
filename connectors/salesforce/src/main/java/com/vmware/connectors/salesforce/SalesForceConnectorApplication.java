/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.salesforce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by supriyas on 9/16/16.
 */
@SpringBootApplication
@Configuration
@EnableScheduling
@SuppressWarnings("PMD.UseUtilityClass")
public class SalesForceConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesForceConnectorApplication.class, args);
    }

}
