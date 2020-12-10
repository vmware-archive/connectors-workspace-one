/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.jwt.JwtHelper;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class AuthUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    private AuthUtil() {
        // hide constructor
    }

    /**
     * Extract email out of a Bearer JWT Authorization header value.
     *
     * Note: You must authenticate the JWT to trust its contents.
     *
     * @param authHeaderVal the authorization header value
     * @return the email (eml claim) in the JWT
     * @throws RuntimeException if authHeaderVal is invalid in some way (null,
     * not a Bearer token, not a JWT, invalid format JWT, eml claim is not a String)
     */
    public static String extractUserEmail(String authHeaderVal) {
        Map<String, Object> claims = getClaims(authHeaderVal);
        return (String) claims.get("eml");
    }

    public static Map<String, Object> getClaims(String authHeaderVal) {
        checkArgument(isNotBlank(authHeaderVal), "Auth Header Token cannot be blank");
        String accessToken = authHeaderVal.replaceAll("^Bearer ", "").trim();
        try {
            String claimsJson = JwtHelper.decode(accessToken).getClaims();
            // Note: We are not validating this JWT.  That has to have been authenticated elsewhere.
            return mapper.readValue(claimsJson, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract eml claim out of JWT!", e); // NOPMD
        }
    }

    public static boolean extractPreHire(String authHeaderVal) {
        Map<String, Object> claims = getClaims(authHeaderVal);
        return (Boolean) Optional.ofNullable(claims.get("pre_hire")).orElse(false);
    }

}
