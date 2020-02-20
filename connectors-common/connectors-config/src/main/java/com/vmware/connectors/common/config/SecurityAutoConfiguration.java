/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.config;

import com.vmware.connectors.common.security.AudienceAuthorizationManager;
import com.vmware.connectors.common.security.ConnectorAuthentication;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

@EnableWebFluxSecurity
public class SecurityAutoConfiguration {

    @Bean
    RSAPublicKey publicKey(
            @Value("${security.oauth2.resource.jwt.key-uri:}") String keyUri,
            @Value("${security.oauth2.resource.jwt.key-value:}") String keyValue
    ) throws IOException, GeneralSecurityException {

        String key = keyValue;
        if (StringUtils.isBlank(key)) {
            key = retrieveKeyFrom(keyUri);
        }

        Reader reader = new StringReader(key);
        var object = new PEMParser(reader).readObject();

        if (object == null) {
            throw new IllegalArgumentException("Invalid key string");
        }

        X509EncodedKeySpec spec = new X509EncodedKeySpec(((ASN1Object)object).getEncoded());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }

    private String retrieveKeyFrom(String keyUri) {
        if (StringUtils.isBlank(keyUri)) {
            throw new IllegalArgumentException("Must specify security.oauth2.resource.jwt.key-uri or security.oauth2.resource.jwt.key-value");
        }
        return new RestTemplate().getForObject(keyUri, String.class);
    }


    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, RSAPublicKey publicKey) {
        http.authorizeExchange().pathMatchers(HttpMethod.GET, "/health", "/templates/**", "/images/**", "/").permitAll()
        .and().csrf().disable()
        .authorizeExchange().anyExchange().access(new AudienceAuthorizationManager())
        .and()
        .oauth2ResourceServer()
        .jwt().publicKey(publicKey).jwtAuthenticationConverter(source -> Mono.just(new ConnectorAuthentication(source)));

        return http.build();
    }

}
