/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.ws1connectors.workday.card.CardBuilderFactory;
import com.vmware.ws1connectors.workday.models.RequestInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.WORKDAY_ACCESS_TOKEN;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.WORKDAY_BASE_URL;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotBlank;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.CARDS_CONFIG;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.LOCALE;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.ROUTING_PREFIX;

@Service
@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class CardService {

    @Autowired TimeOffTaskService timeOffTaskService;
    @Autowired private CardBuilderFactory cardBuilderFactory;

    public Mono<Cards> getNotificationCards(RequestInfo requestInfo) {
        checkArgumentNotNull(requestInfo, CARDS_CONFIG);
        checkArgumentNotBlank(requestInfo.getBaseUrl(), WORKDAY_BASE_URL);
        checkArgumentNotBlank(requestInfo.getConnectorAuth(), WORKDAY_ACCESS_TOKEN);
        checkArgumentNotBlank(requestInfo.getRoutingPrefix(), ROUTING_PREFIX);
        checkArgumentNotNull(requestInfo.getLocale(), LOCALE);

        return timeOffTaskService.getApprovalTasks(requestInfo.getBaseUrl(), requestInfo.getConnectorAuth(), requestInfo.getTenantName(), requestInfo.getLocale())
                .concatMap(approvalTask -> Mono.fromCallable(() -> cardBuilderFactory.getCardBuilder(approvalTask)
                        .createCard(approvalTask, requestInfo))
                        .onErrorResume(throwable -> {
                            LOGGER.error("Error occurred while building notification card for request {}",
                                    approvalTask, throwable);
                            return Mono.empty();
                        }))
                .reduce(new Cards(), this::addCard);
    }

    private Cards addCard(Cards cards, Card card) {
        cards.getCards().add(card);
        return cards;
    }

}
