/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.airwatch;

import com.vmware.connectors.airwatch.config.AppConfigurations;
import com.vmware.connectors.airwatch.exceptions.ConfigException;
import com.vmware.connectors.airwatch.service.AppConfigService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

import javax.validation.Validator;
import javax.validation.Validation;
import javax.validation.ConstraintViolation;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

/**
 * Created by harshas on 9/19/17.
 */
@Configuration
public class ConnConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ConnConfiguration.class);

    private final AppConfigurations appConfigurations;

    private final Resource metadataHalResource;

    private final Environment environment;

    @Autowired
    public ConnConfiguration(AppConfigurations appConfigurations,
                             Environment environment,
                             @Value("classpath:static/discovery/metadata.json") Resource metadataHalResource) {

        // Validate managed-apps.yml configurations.
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<AppConfigurations>> violations = validator.validate(appConfigurations);
        violations
                .forEach(v -> logger.error("{} Check {} in managed-apps configuration.", v.getMessage(), v.getPropertyPath().toString()));
        if (!violations.isEmpty()) {
            throw new ConfigException("Invalid configurations for managed-apps.");
        }

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
                .collect(Collectors.joining("\\\\b|\\\\b", "\\\\b", "\\\\b"));

        String metaData = IOUtils.toString(metadataHalResource.getInputStream(), Charset.defaultCharset());
        return metaData.replace("CONNECTOR_REGEX", connectorRegex);
    }

    @Bean
    public URI gbBaseUrl() {
        try {
            return fromHttpUrl(environment.getProperty("greenbox.url")).build().toUri();
        } catch (IllegalArgumentException ex) {
            logger.error("Greenbox URL config is invalid. ", ex);
            throw new ConfigException("Greenbox URL config is invalid.", ex);
        }
    }
}
