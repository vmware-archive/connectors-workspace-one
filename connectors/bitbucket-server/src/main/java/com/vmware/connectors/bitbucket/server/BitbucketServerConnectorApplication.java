/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.bitbucket.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Configuration
@SuppressWarnings("PMD.UseUtilityClass")
public class BitbucketServerConnectorApplication {
    public static void main(final String[] args) {
        SpringApplication.run(BitbucketServerConnectorApplication.class, args);
    }
}
