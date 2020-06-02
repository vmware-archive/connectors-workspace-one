/*
 * Project Service Now Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@SuppressWarnings({"PMD.UseUtilityClass", "checkstyle:HideUtilityClassConstructor"})
public class ServiceNowConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceNowConnectorApplication.class, args);
    }

}
