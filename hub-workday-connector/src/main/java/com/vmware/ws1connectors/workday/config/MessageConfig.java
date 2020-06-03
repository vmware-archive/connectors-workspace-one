/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

@Configuration
public class MessageConfig {
    /**
     * Bean for  Error Messages
     * Looks for Files ErrorMessages_{Locale} in the classpath.
     * Example:ErrorMessages_en_US.properties.
     */
    @Bean(name = "errorMessageSource")
    public MessageSource errorMessageSource() {
        ReloadableResourceBundleMessageSource errorMessageSource = new ReloadableResourceBundleMessageSource();
        errorMessageSource.setBasename("classpath:ErrorMessages");
        errorMessageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return errorMessageSource;
    }
}
