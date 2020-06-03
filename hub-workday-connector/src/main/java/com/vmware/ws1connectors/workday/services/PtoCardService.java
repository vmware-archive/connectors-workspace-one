/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.ws1connectors.workday.card.CardBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotBlank;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkBasicConnectorArgumentsNotBlank;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.LOCALE;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.REQUEST;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.ROUTING_PREFIX;

@Service
@Slf4j
public class PtoCardService {

    @Autowired TimeOffTaskService timeOffTaskService;
    @Autowired CardBuilder cardBuilder;

    public Mono<Cards> getPtoCards(final String connectorBaseUrl, final String routingPrefix, final String connectorAuth,
                                   final String userEmail, final Locale locale, final ServerHttpRequest request) {
        checkBasicConnectorArgumentsNotBlank(connectorBaseUrl, connectorAuth, userEmail);
        checkArgumentNotBlank(routingPrefix, ROUTING_PREFIX);
        checkArgumentNotNull(locale, LOCALE);
        checkArgumentNotNull(request, REQUEST);

        return timeOffTaskService.getTimeOffTasks(connectorBaseUrl, connectorAuth, userEmail, locale)
            .concatMap(timeOffTask -> Mono.fromCallable(() -> cardBuilder.createCard(routingPrefix, locale, timeOffTask, request))
                .onErrorResume(throwable -> {
                    LOGGER.error("Error occurred while building notification card for time off request {}", timeOffTask.getInboxTaskId(), throwable);
                    return Mono.empty();
                }))
            .reduce(new Cards(), this::addCard);
    }

    private Cards addCard(Cards cards, Card card) {
        cards.getCards().add(card);
        return cards;
    }

}
