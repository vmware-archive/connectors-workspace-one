/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

@SpringBootApplication
@EnableResourceServer
@Configuration
@EnableWebSecurity
@EnableScheduling
@SuppressWarnings("PMD.UseUtilityClass")
public class HubConcurConnectorApplication {
    public static void main(final String[] args) {
        SpringApplication.run(HubConcurConnectorApplication.class, args);
    }
}
