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

    public static String extractUserEmail(String authorization) throws IOException {
        String accessToken = authorization.replaceAll("^Bearer ", "").trim();
        Map claims = mapper.readValue(JwtHelper.decode(accessToken).getClaims(), Map.class);
        return (String) claims.get("eml");
    }

}
