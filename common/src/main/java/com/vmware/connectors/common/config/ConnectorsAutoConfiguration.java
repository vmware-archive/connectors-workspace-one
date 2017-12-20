/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.config;

import com.vmware.connectors.common.RootController;
import com.vmware.connectors.common.ExceptionHandlers;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.connectors.common.context.ContextInterceptor;
import com.vmware.connectors.common.JsonDocumentHttpMessageConverter;
import com.vmware.connectors.common.MdcFilter;
import com.vmware.connectors.common.utils.SingleReturnValueHandler;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.conn.NHttpClientConnectionManager;
import org.apache.http.nio.reactor.IOReactorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.JwtAccessTokenConverterRestTemplateCustomizer;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.MimeMappings;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.util.StringUtils;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.Filter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Created by Rob Worsnop on 11/29/16.
 */
@Configuration
@Import({ExceptionHandlers.class, RootController.class})
public class ConnectorsAutoConfiguration {

    // https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-security.html
    private static final String VIDM_PUB_KEY_URL = "${security.oauth2.resource.jwt.key-uri}";

    @Bean
    public SingleReturnValueHandler singleReturnValueHandler() {
        return new SingleReturnValueHandler();
    }

    @Bean
    public WebMvcConfigurerAdapter observableMVCConfiguration(final SingleReturnValueHandler singleReturnValueHandler) {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
                returnValueHandlers.add(singleReturnValueHandler);
            }
        };
    }

    @Bean
    public EmbeddedServletContainerCustomizer servletCustomizer() {
        return new EmbeddedServletContainerCustomizer() {
            @Override
            public void customize(ConfigurableEmbeddedServletContainer container) {
                MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
                mappings.add("hbs", "text/x-handlebars-template");
                mappings.add("hal", "application/hal+json");
                container.setMimeMappings(mappings);
            }
        };
    }

    @Bean
    public Filter mdcFilter() {
        return new MdcFilter();
    }


    @Bean
    public JwtAccessTokenConverterRestTemplateCustomizer jwtAccessTokenConverterRestTemplateCustomizer() {
        return new JwtAccessTokenConverterRestTemplateCustomizer () {
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

    /*
     * Since Spring beans are eagerly loaded by default, this will check that
     * the vIdmPubKeyUrl was configured at startup (allowing users to see their
     * mistake in the logs and systemd status instead of having a running
     * service that doesn't actually work).
     *
     * Note: I don't expect anyone to actually inject this String.
     */
    @Bean
    public String vIdmPubKeyUrl(@Value(VIDM_PUB_KEY_URL) String vIdmPubKeyUrl) {
        if (StringUtils.isEmpty(vIdmPubKeyUrl)) {
            throw new IllegalArgumentException(VIDM_PUB_KEY_URL + " must be configured");
        }
        return vIdmPubKeyUrl;
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer(@Value("${static.cacheControl.maxAge:1}") long maxAge,
                                             @Value("${static.cacheControl.unit:DAYS}") TimeUnit unit) {
        return new WebMvcConfigurerAdapter() {
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

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new ContextInterceptor());
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
                        .antMatchers(HttpMethod.GET, "/templates/**", "/discovery/**", "/images/**", "/").permitAll()
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
    public AsyncClientHttpRequestFactory asyncClientHttpRequestFactory(
            NHttpClientConnectionManager clientConnectionManager,
            @Value("${http.client.maxConnTotal:1024}") int maxConnTotal,
            @Value("${http.client.maxConnPerRoute:1024}") int maxConnPerRoute) throws InterruptedException {
        CloseableHttpAsyncClient client = HttpAsyncClientBuilder.create()
                // Some backends will return a cookie and prefer it to the Authorization header.
                // This is very bad when we re-use a connection for different users!
                .disableCookieManagement()
                .setMaxConnTotal(maxConnTotal)
                .setMaxConnPerRoute(maxConnPerRoute)
                .setConnectionManager(clientConnectionManager)
                .build();
        return new HttpComponentsAsyncClientHttpRequestFactory(client);
    }

    @Bean
    public NHttpClientConnectionManager clientConnectionManager() throws IOReactorException {
        DefaultConnectingIOReactor connectingIOReactor = new DefaultConnectingIOReactor();
        return new PoolingNHttpClientConnectionManager(connectingIOReactor);
    }

    @Bean
    public AsyncRestOperations rest(AsyncClientHttpRequestFactory requestFactory) {
        AsyncRestTemplate template = new AsyncRestTemplate(requestFactory);
        template.getMessageConverters().add(0, new JsonDocumentHttpMessageConverter());
        return template;
    }

    @Bean
    public IdleConnectionsEvictor idleConnectionsEvictor(NHttpClientConnectionManager connMgr) {
        return new IdleConnectionsEvictor(connMgr);
    }
}
