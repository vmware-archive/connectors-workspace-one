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
import java.util.UUID;

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

    public String createAccessToken() throws IOException, GeneralSecurityException {
        return createAccessToken(Instant.now().plus(5, HOURS));
    }

    public String createAccessToken(Instant expiry) throws IOException, GeneralSecurityException {
        String user = "fred";
        String tenant = "acme";
        String userName = user + "@" + tenant;
        return Jwts.builder().setHeaderParam(TYPE, JWT_TYPE)
                .setId(UUID.randomUUID().toString())
                .claim("prn", userName)
                .claim("domain", "System Domain")
                .claim("user_id", Integer.toString(userName.hashCode()))
                .claim("auth_time", System.currentTimeMillis() / 1000)
                .setIssuer("https://" + tenant + ".vmwareidentity.com/SAAS/auth")
                .setAudience("https://" + tenant + ".vmwareidentity.com/SAAS/auth/oauthtoken")
                .claim("ctx", "\"[{\\\"mtd\\\":\\\"urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport\\\",\\\"iat\\\":1481122159,\\\"id\\\":756019}]\"")
                .claim("scp", "openid profile user email")
                .claim("idp", "0")
                .claim("eml", "jdoe@vmware.com")
                .claim("cid", "HeroCard_Template1@8f7ea92b-930b-4d47-9a10-f3fb888f2b64")
                .claim("did", user + "-3C580712-06BB-49DD-9389-E8255408EA7A")
                .claim("wid", "")
                .setExpiration(java.util.Date.from(expiry))
                .setIssuedAt(new java.util.Date())
                .setSubject(UUID.nameUUIDFromBytes(userName.getBytes()).toString())
                .claim("prn_type", "USER")
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
