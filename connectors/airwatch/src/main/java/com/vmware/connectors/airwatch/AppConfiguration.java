/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch;

import com.vmware.connectors.airwatch.service.AppConfigService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/**
 * Created by harshas on 9/19/17.
 */
@Configuration
public class AppConfiguration {

    private final ConfigurableEnvironment environment;

    private final Resource metadataHalResource;

    @Autowired
    public AppConfiguration(ConfigurableEnvironment environment,
                            @Value("classpath:static/discovery/metadata.hal") Resource metadataHalResource) {
        this.environment = environment;
        this.metadataHalResource = metadataHalResource;
    }

    @Bean
    public AppConfigService appConfigService(Environment environment) {
        return new AppConfigService(environment);
    }

    /*
    Build a regex to find one or more managed apps.
    Modify the connector metadata to include this regex.
     */
    @Bean
    public String connectorMetadata() throws IOException {
        final List<String> supportedPlatforms = Arrays.asList("android", "ios");

        final Set<String> appNames = new HashSet<>();
        environment.getPropertySources().iterator().forEachRemaining(propertySource -> {
            if (propertySource instanceof MapPropertySource) {
                ((MapPropertySource) propertySource).getSource().forEach((key, value) -> {
                    for (String platform : supportedPlatforms) {
                        if (key.startsWith(platform + ".")) {
                            appNames.add(StringUtils.substringAfterLast(key, platform + "."));
                        }
                    }
                });
            }
        });

        StringBuilder regexBuilder = new StringBuilder("(?i)");
        appNames.forEach(appName -> regexBuilder.append(appName).append("|"));
        String connectorRegex = StringUtils.substringBeforeLast(regexBuilder.toString(), "|");

        String metaData = IOUtils.toString(metadataHalResource.getInputStream(), Charset.defaultCharset());
        return metaData.replace("CONNECTOR_REGEX", connectorRegex);
    }
}
