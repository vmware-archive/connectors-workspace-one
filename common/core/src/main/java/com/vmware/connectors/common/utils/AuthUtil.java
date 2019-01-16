/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.jwt.JwtHelper;

import java.io.IOException;
import java.util.Map;

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
        String accessToken = authHeaderVal.replaceAll("^Bearer ", "").trim();
        try {
            String claimsJson = JwtHelper.decode(accessToken).getClaims();
            // Note: We are not validating this JWT.  That has to have been authenticated elsewhere.
            Map claims = mapper.readValue(claimsJson, Map.class);
            return (String) claims.get("eml");
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract eml claim out of JWT!", e); // NOPMD
        }
    }

}
