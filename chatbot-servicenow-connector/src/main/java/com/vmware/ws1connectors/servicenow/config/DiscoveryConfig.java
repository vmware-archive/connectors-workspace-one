/*
 * Project Service Now Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.ws1connectors.servicenow.config;

import com.vmware.ws1connectors.servicenow.discovery.DiscoveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class DiscoveryConfig {

    @Autowired DiscoveryService discoveryService;

    @Bean public String connectorMetadata() {
        return discoveryService.getDiscoveryMetaData();
    }
}
