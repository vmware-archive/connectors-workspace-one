/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.ws1connectors.workday.card.Day0CardBuilder;
import com.vmware.ws1connectors.workday.exceptions.UserNotFoundException;
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
public class Day0CardService {
    @Autowired InboxService inboxService;
    @Autowired UserService userService;
    @Autowired Day0CardBuilder cardBuilder;

    public Mono<Cards> getDay0Cards(final String baseUrl, final String authToken, final String email,
                                    final Locale locale) {
        checkBasicConnectorArgumentsNotBlank(baseUrl, authToken, email);
        checkArgumentNotNull(locale, LOCALE);

        return userService.getUser(baseUrl, authToken, email)
                .switchIfEmpty(Mono.error(UserNotFoundException::new))
                .flatMapMany(user -> inboxService.getTasks(baseUrl, authToken, user))
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

