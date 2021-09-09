/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.ws1connectors.workday.card.Day0CardBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkBasicConnectorArgumentsNotBlank;
import static com.vmware.ws1connectors.workday.utils.WorkdayConnectorConstants.LOCALE;

@Service
@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class Day0CardService {
    @Autowired InboxService inboxService;
    @Autowired Day0CardBuilder cardBuilder;

    public Mono<Cards> getDay0Cards(final String baseUrl, final String authToken, final String tenantName,
                                    final Locale locale) {
        checkBasicConnectorArgumentsNotBlank(baseUrl, authToken, tenantName);
        checkArgumentNotNull(locale, LOCALE);

        return inboxService.getTasks(baseUrl, authToken, tenantName)
                .collectList()
                .filter(inboxTasks -> !CollectionUtils.isEmpty(inboxTasks))
                .map(inboxTasks -> cardBuilder.createCard(baseUrl, locale, inboxTasks))
                .map(card -> {
                    Cards cards = new Cards();
                    cards.getCards().add(card);
                    return cards;
                }).switchIfEmpty(Mono.just(new Cards()));
    }
}

