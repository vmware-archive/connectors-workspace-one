/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.web;

import com.vmware.connectors.common.payloads.request.CardRequest;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.ws1connectors.workday.models.RequestInfo;
import com.vmware.ws1connectors.workday.services.CardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.CARDS_REQUESTS_API;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotBlank;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TENANT_NAME;
import static com.vmware.ws1connectors.workday.utils.CardConstants.TENANT_URL;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.BASE_URL_HEADER;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.CONNECTOR_AUTH_HEADER;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.ROUTING_PREFIX_HEADER;
import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class CardsController {
    @Autowired private CardService cardService;

    @PostMapping(path = CARDS_REQUESTS_API, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public Mono<Cards> getCards(final Locale requestLocale,
                                @RequestHeader(AUTHORIZATION) final String authorization,
                                @RequestHeader(CONNECTOR_AUTH_HEADER) final String connectorAuth,
                                @RequestHeader(BASE_URL_HEADER) final String baseUrl,
                                @RequestHeader(ROUTING_PREFIX_HEADER) final String routingPrefix,
                                @RequestBody final CardRequest cardRequest) {
        final String userEmail = AuthUtil.extractUserEmail(authorization);
        final boolean isPreHire = AuthUtil.extractPreHire(authorization);
        final Locale endUserLocale = isNull(requestLocale) ? Locale.US : requestLocale;
        final String tenantName = cardRequest.getConfig().get(TENANT_NAME);
        final String tenantUrl = cardRequest.getConfig().get(TENANT_URL);
        checkArgumentNotBlank(tenantName, TENANT_NAME);
        checkArgumentNotBlank(tenantUrl, TENANT_URL);
        final RequestInfo requestInfo = RequestInfo.builder()
                .baseUrl(baseUrl)
                .routingPrefix(routingPrefix)
                .connectorAuth(connectorAuth)
                .tenantName(tenantName)
                .tenantUrl(tenantUrl)
                .isPreHire(isPreHire)
                .locale(endUserLocale)
                .build();
        LOGGER.info("Received request to get time-off approvals pending in workday for user with email {}. BaseUrl={}, RoutingPrefix={}, locale={}, tenantName={}, tenantUrl={}, isPreHire={}", userEmail, baseUrl, routingPrefix, endUserLocale, tenantName, tenantUrl, isPreHire);
        return cardService.getNotificationCards(requestInfo);
    }

}
