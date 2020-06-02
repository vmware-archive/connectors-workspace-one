/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.web;

import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.connectors.common.utils.AuthUtil;
import com.vmware.ws1connectors.workday.services.Day0CardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static com.vmware.ws1connectors.workday.utils.CardConstants.DAY0_CARDS_URI;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.BASE_URL_HEADER;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.CONNECTOR_AUTH_HEADER;
import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Slf4j
public class Day0CardsController {

    @Autowired private Day0CardService cardService;

    @PostMapping(path = DAY0_CARDS_URI, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public Mono<Cards> getCards(final Locale requestLocale,
                                @RequestHeader(AUTHORIZATION) final String authorization,
                                @RequestHeader(CONNECTOR_AUTH_HEADER) final String connectorAuth,
                                @RequestHeader(BASE_URL_HEADER) final String baseUrl) {
        final String userEmail = AuthUtil.extractUserEmail(authorization);
        final Locale endUserLocale = isNull(requestLocale) ? Locale.US : requestLocale;
        LOGGER.info("Received request to get day0 tasks pending in workday for user with BaseUrl={}, locale={}", baseUrl, endUserLocale);
        return cardService.getDay0Cards(baseUrl, connectorAuth, userEmail, endUserLocale);
    }
}

