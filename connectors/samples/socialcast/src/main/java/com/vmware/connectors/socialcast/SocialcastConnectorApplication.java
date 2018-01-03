/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.socialcast;

import com.vmware.connectors.common.utils.CardTextAccessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
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
public class SocialcastConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialcastConnectorApplication.class, args);
    }

    @Bean
    public SocialcastMessageFormatter formatter(CardTextAccessor cardTextAccessor) {
        return new SocialcastMessageFormatter(cardTextAccessor);
    }

}
