/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.test;

import io.jsonwebtoken.Jwts;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Date;

import static io.jsonwebtoken.Header.JWT_TYPE;
import static io.jsonwebtoken.Header.TYPE;
import static io.jsonwebtoken.SignatureAlgorithm.RS256;
import static java.time.temporal.ChronoUnit.HOURS;

/**
 * Created by Rob Worsnop on 11/30/16.
 */
@TestComponent
public final class JwtUtils {

    @Value("classpath:jwt-signer.der")
    private Resource signer;

    public String createConnectorToken() throws IOException, GeneralSecurityException {
        return createConnectorToken(Instant.now().plus(5, HOURS));
    }

    public String createConnectorToken(Instant expiry) throws IOException, GeneralSecurityException {

        return Jwts.builder().setHeaderParam(TYPE, JWT_TYPE)
                .claim("prn", "fred@acme")
                .setExpiration(Date.from(expiry))
                .setIssuedAt(new Date())
                .signWith(RS256, getSigner())
                .compact();
    }

    private PrivateKey getSigner() throws IOException, GeneralSecurityException {
        try (InputStream inputStream = signer.getInputStream()) {
            PKCS8EncodedKeySpec spec =
                    new PKCS8EncodedKeySpec(IOUtils.toByteArray(inputStream));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(spec);
        }
    }
}
