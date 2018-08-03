/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.config;

import com.vmware.connectors.common.json.JsonDocumentDecoder;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.web.ExceptionHandlers;
import com.vmware.connectors.common.web.MdcFilter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.security.oauth2.resource.JwtAccessTokenConverterConfigurer;
import org.springframework.boot.autoconfigure.security.oauth2.resource.JwtAccessTokenConverterRestTemplateCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.*;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.Filter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Created by Rob Worsnop on 11/29/16.
 */
@Configuration
@AutoConfigureBefore(ServletWebServerFactoryAutoConfiguration.class)
@Import({ExceptionHandlers.class})
public class ConnectorsAutoConfiguration {


    @Bean
    public ConfigurableServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
        mappings.add("hbs", "text/x-handlebars-template");
        mappings.add("hal", "application/hal+json");
        factory.setMimeMappings(mappings);
        return factory;
    }

    @Bean
    public Filter mdcFilter() {
        return new MdcFilter();
    }


    @Bean
    public JwtAccessTokenConverterRestTemplateCustomizer jwtAccessTokenConverterRestTemplateCustomizer() {
        return new JwtAccessTokenConverterRestTemplateCustomizer() {
            @Override
            public void customize(RestTemplate template) {
                template.getMessageConverters().add(0, new AbstractHttpMessageConverter<Map>(MediaType.ALL) {
                    @Override
                    protected boolean supports(Class<?> clazz) {
                        return clazz.equals(Map.class);
                    }

                    @Override
                    protected Map readInternal(Class<? extends Map> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
                        return Collections.singletonMap("value", IOUtils.toString(inputMessage.getBody(), UTF_8));
                    }

                    @Override
                    protected void writeInternal(Map map, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
                        throw new UnsupportedOperationException();
                    }
                });
            }
        };
    }

    @Bean
    public JwtAccessTokenConverterConfigurer jwtAccessTokenConverterConfigurer () {
        return new JwtConverter();
    }

    /*
     * Since Spring beans are eagerly loaded by default, this will check that either
     * the vIdmPubKeyUrl or vIdmPubKeyValue was configured at startup (allowing users to see their
     * mistake in the logs and systemd status instead of having a running
     * service that doesn't actually work).
     *
     * Note: I don't expect anyone to actually inject this String.
     */
    @Bean
    public String vIdmPubKey(@Value("${security.oauth2.resource.jwt.key-uri:}") String vIdmPubKeyUrl,
                             @Value("${security.oauth2.resource.jwt.key-value:}") String vIdmPubKeyValue) {
        if (!BooleanUtils.xor(new boolean[]{StringUtils.isEmpty(vIdmPubKeyUrl), StringUtils.isEmpty(vIdmPubKeyValue)})) {
            throw new IllegalArgumentException("Exactly one of security.oauth2.resource.jwt.key-uri/value must be configured");
        }
        return vIdmPubKeyUrl;
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer(@Value("${static.cacheControl.maxAge:1}") long maxAge,
                                             @Value("${static.cacheControl.unit:DAYS}") TimeUnit unit) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                CacheControl cacheControl = CacheControl.maxAge(maxAge, unit);
                registry.addResourceHandler("/templates/**")
                        .addResourceLocations("classpath:/static/templates/")
                        .setCacheControl(cacheControl);
                registry.addResourceHandler("/discovery/**")
                        .addResourceLocations("classpath:/static/discovery/")
                        .setCacheControl(cacheControl);
                registry.addResourceHandler("/images/**")
                        .addResourceLocations("classpath:/static/images/")
                        .setCacheControl(cacheControl);
            }
        };
    }

    @Bean
    public ResourceServerConfigurer resourceServer() {
        return new ResourceServerConfigurerAdapter() {
            @Override
            @SuppressWarnings("PMD.SignatureDeclareThrowsException")
            public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
                resources.resourceId(null);
            }

            @Override
            @SuppressWarnings("PMD.SignatureDeclareThrowsException")
            public void configure(HttpSecurity http) throws Exception {
                http.anonymous()
                        .and()
                        .authorizeRequests()
                        .antMatchers(HttpMethod.GET, "/health", "/templates/**", "/discovery/**", "/images/**", "/").permitAll()
                        .and()
                        .authorizeRequests()
                        .antMatchers("/**").authenticated();
            }
        };
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
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public CodecCustomizer codecCustomizer() {
        return configurer -> configurer.customCodecs().decoder(new JsonDocumentDecoder());
    }
}
