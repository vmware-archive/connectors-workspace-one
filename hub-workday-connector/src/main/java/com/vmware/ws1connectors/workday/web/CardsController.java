/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.web;

import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.ws1connectors.workday.services.PtoCardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.CARDS_REQUESTS_API;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.BASE_URL_HEADER;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.CONNECTOR_AUTH_HEADER;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.ROUTING_PREFIX_HEADER;
import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Slf4j
public class CardsController {
    @Autowired PtoCardService ptoCardService;

    @PostMapping(path = CARDS_REQUESTS_API, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public Mono<Cards> getCards(final Locale requestLocale, final ServerHttpRequest request,
                                @RequestHeader(AUTHORIZATION) final String authorization,
                                @RequestHeader(CONNECTOR_AUTH_HEADER) final String connectorAuth,
                                @RequestHeader(BASE_URL_HEADER) final String baseUrl,
                                @RequestHeader(ROUTING_PREFIX_HEADER) final String routingPrefix) {
        final String userEmail = AuthUtil.extractUserEmail(authorization);
        final Locale endUserLocale = isNull(requestLocale) ? Locale.US : requestLocale;
        LOGGER.info("Received request to get time-off approvals pending in workday for user with email {}. BaseUrl={}, RoutingPrefix={}, locale={}", userEmail, baseUrl, routingPrefix, endUserLocale);
        return ptoCardService.getPtoCards(baseUrl, routingPrefix, connectorAuth, userEmail, endUserLocale, request);
    }

}
