/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.hub.servicenow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

@SpringBootApplication
@EnableResourceServer
@EnableWebSecurity
@EnableScheduling
@SuppressWarnings("PMD.UseUtilityClass")
public class HubServiceNowConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(HubServiceNowConnectorApplication.class, args);
    }

}
