/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.config;

import com.vmware.connectors.common.json.JsonDocumentDecoder;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.utils.ConnectorTextAccessor;
import com.vmware.connectors.common.web.ConnectorRootController;
import com.vmware.connectors.common.web.ExceptionHandlers;
import com.vmware.connectors.common.web.FormWebFilter;
import com.vmware.connectors.common.web.SecurityContextWebFilter;
import com.vmware.connectors.common.web.ServerHeaderWebFilter;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebFilter;

import java.io.IOException;
import java.nio.charset.Charset;


/**
 * Created by Rob Worsnop on 11/29/16.
 */
@Configuration
@Import({ExceptionHandlers.class, ConnectorRootController.class})
public class ConnectorsAutoConfiguration {

    private final Resource metadataHalResource;

    @Autowired
    public ConnectorsAutoConfiguration(@Value("classpath:static/discovery/metadata.json") Resource metadataHalResource) {
        this.metadataHalResource = metadataHalResource;
    }

    @Bean
    public WebFilter securityContextFilter() {
        return new SecurityContextWebFilter();
    }

    @Bean
    public WebFilter formFilter() {
        return new FormWebFilter();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter serverHeaderFilter(ServerProperties serverProperties) {
        return new ServerHeaderWebFilter(serverProperties.getServerHeader());
    }


    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource ret = new ResourceBundleMessageSource();
        ret.setFallbackToSystemLocale(false);
        ret.setBasename("cards/text");
        return ret;
    }

    @Bean
    public CardTextAccessor cardTextAccessor(MessageSource messageSource) {
        return new CardTextAccessor(messageSource);
    }

    @Bean
    public ConnectorTextAccessor connectorTextAccessor(MessageSource messageSource) {
        return new ConnectorTextAccessor(messageSource);
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public CodecCustomizer codecCustomizer() {
        return configurer -> configurer.customCodecs().decoder(new JsonDocumentDecoder());
    }

    @Bean
    @ConditionalOnMissingBean(name = "connectorMetadata")
    public String connectorMetadata() throws IOException {
        return IOUtils.toString(metadataHalResource.getInputStream(), Charset.defaultCharset());
    }
}
