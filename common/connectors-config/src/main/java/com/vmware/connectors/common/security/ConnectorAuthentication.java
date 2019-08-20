/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;

public class ConnectorAuthentication extends AbstractAuthenticationToken {
    private final Jwt jwt;

    public ConnectorAuthentication(Jwt jwt) {
        super(Collections.emptyList());
        setAuthenticated(true);
        this.jwt = jwt;
    }

    @Override
    public Object getCredentials() {
        return jwt.getTokenValue();
    }

    @Override
    public Object getPrincipal() {
        return jwt.getClaims().get("prn");
    }

    public Jwt getJwt() {
        return jwt;
    }
}
