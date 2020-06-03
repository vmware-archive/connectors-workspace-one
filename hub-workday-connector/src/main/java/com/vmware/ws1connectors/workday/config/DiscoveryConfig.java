/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.config;

import com.vmware.ws1connectors.workday.discovery.DiscoveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class DiscoveryConfig {

    @Bean public String connectorMetadata(final DiscoveryService discoveryService) {
        return discoveryService.getDiscoveryMetaData();
    }
}
