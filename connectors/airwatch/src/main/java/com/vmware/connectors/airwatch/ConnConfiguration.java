/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch;

import com.vmware.connectors.airwatch.config.AppConfigurations;
import com.vmware.connectors.airwatch.service.AppConfigService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

/**
 * Created by harshas on 9/19/17.
 */
@Configuration
public class ConnConfiguration {

    private final AppConfigurations appConfigurations;

    private final Resource metadataHalResource;

    private final Environment environment;

    @Autowired
    public ConnConfiguration(AppConfigurations appConfigurations,
                             Environment environment,
                             @Value("classpath:static/discovery/metadata.json") Resource metadataHalResource) {
        this.appConfigurations = appConfigurations;
        this.environment = environment;
        this.metadataHalResource = metadataHalResource;
    }

    @Bean
    public AppConfigService appConfigService() {
        return new AppConfigService(appConfigurations);
    }

    /*
     * Make a set of app keywords and build regex for the connector.
     * Replace regex place holder in the connector metadata file.
     */
    @Bean
    public String connectorMetadata() throws IOException {

        final String connectorRegex = "(?i)" + appConfigurations.getApps().stream()
                .flatMap(appConfiguration -> appConfiguration.getKeywords().stream())
                .collect(Collectors.joining("|"));

        String metaData = IOUtils.toString(metadataHalResource.getInputStream(), Charset.defaultCharset());
        return metaData.replace("CONNECTOR_REGEX", connectorRegex);
    }

    @Bean
    public URI gbBaseUrl() {
        String gbUrl = environment.getProperty("greenbox.url");
        return fromHttpUrl(gbUrl).build().toUri();
    }
}
