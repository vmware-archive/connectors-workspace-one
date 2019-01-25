/*
 * Copyright © 2019 VMware, Inc. All rights reserved. This product is protected by
 * copyright and intellectual property laws in the United States and other countries as
 * well as by international treaties. AirWatch products may be covered by one or more
 * patents listed at http://www.vmware.com/go/patents.
 */

package com.vmware.connectors.salesforce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

/**
 * Created by supriyas on 9/16/16.
 */
@SpringBootApplication
@EnableResourceServer
@Configuration
@EnableWebSecurity
@EnableScheduling
@SuppressWarnings("PMD.UseUtilityClass")
public class TestDriveSalesforceConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestDriveSalesforceConnectorApplication.class, args);
    }

}
