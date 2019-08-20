/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.security;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;

public class AudienceAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {
    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
        String audience = UriComponentsBuilder.fromHttpRequest(context.getExchange().getRequest()).build().toUri().toString();
        return authentication.cast(ConnectorAuthentication.class)
                .map(auth -> hasAudience(auth, audience))
                .map(AuthorizationDecision::new);

    }

    private static boolean hasAudience(ConnectorAuthentication authentication , String expectedAudience) {
        return CollectionUtils.containsAny(authentication.getJwt().getAudience(), List.of(expectedAudience));
    }
}
