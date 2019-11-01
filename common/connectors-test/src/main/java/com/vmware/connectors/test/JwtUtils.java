/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.test;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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

import static com.nimbusds.jose.JOSEObjectType.JWT;
import static java.time.temporal.ChronoUnit.HOURS;

/**
 * Created by Rob Worsnop on 11/30/16.
 */
@TestComponent
public final class JwtUtils {

    @Value("classpath:jwt-signer.der")
    private Resource signer;

    public String createConnectorToken(String audience) throws IOException, GeneralSecurityException {
        return createConnectorToken(Instant.now().plus(5, HOURS), audience);
    }

    public String createConnectorToken(Instant expiry, String audience) throws IOException, GeneralSecurityException {

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .claim("prn", "fred@acme")
                .claim("eml", "admin@acme.com")
                .audience(audience)
                .expirationTime(Date.from(expiry))
                .issueTime(Date.from(Instant.now()))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(JWT)
                        .build(),
                claims
        );

        try {
            signedJWT.sign(new RSASSASigner(getSigner()));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new AssertionError("Could not sign JWT", e);
        }
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
