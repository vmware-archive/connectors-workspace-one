/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.ws1connectors.workday.card.CardBuilderFactory;
import com.vmware.ws1connectors.workday.card.TimeOffCardBuilder;
import com.vmware.ws1connectors.workday.exceptions.TimeOffTaskException;
import com.vmware.ws1connectors.workday.models.ApprovalTask;
import com.vmware.ws1connectors.workday.models.RequestInfo;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffTask;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Locale;
import java.util.stream.Stream;

import static com.vmware.ws1connectors.workday.test.JsonUtils.convertFromJsonFile;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CardServiceTest extends ServiceTestsBase {
    private static final String NO_ROUTING_PREFIX = null;
    private static final String NO_CONNECTOR_AUTH = null;
    private static final Locale NO_LOCALE = null;
    private static final Locale LOCALE = Locale.US;
    private static final TimeOffTask TIME_OFF_TASK = convertFromJsonFile("time_off_task_1.json", TimeOffTask.class);
    private static final TimeOffTask TIME_OFF_TASK_2 = convertFromJsonFile("time_off_task_2.json", TimeOffTask.class);
    private static final String ROUTING_PREFIX = "https://dev.hero.example.com/connectors/id";
    private static final String CONNECTOR_AUTH = "connectorAuth";
    private static final String TENANT_NAME = "vmware_gms";
    private static final String TENANT_URL = "https://impl.workday.com";
    private static final Duration DURATION_2_SECONDS = Duration.ofSeconds(2);
    private static final RequestInfo REQUEST_INFO = RequestInfo.builder().tenantName(TENANT_NAME)
            .baseUrl(BASE_URL)
            .routingPrefix(ROUTING_PREFIX)
            .connectorAuth(CONNECTOR_AUTH)
            .tenantUrl(TENANT_URL)
            .isPreHire(false)
            .locale(Locale.US)
            .build();

    @InjectMocks private CardService cardService;
    @Mock private TimeOffTaskService mockTimeOffTaskService;
    @Mock private TimeOffCardBuilder mockTimeOffCardBuilder;
    @Mock private CardBuilderFactory mockCardBuilderFactory;

    private static Stream<Arguments> invalidInputsForCreateCard() {
        return new ArgumentsStreamBuilder()
            .add(RequestInfo.builder().baseUrl(NO_BASE_URL).build())
            .add(RequestInfo.builder().routingPrefix(NO_ROUTING_PREFIX).build())
            .add(RequestInfo.builder().connectorAuth(NO_CONNECTOR_AUTH).build())
            .add(RequestInfo.builder().locale(NO_LOCALE).build())
            .build();
    }

    @ParameterizedTest
    @NullSource
    public void whenCreateCardProvidedWithNullInput(final RequestInfo requestInfo) {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> cardService.getNotificationCards(requestInfo));
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForCreateCard")
    public void whenCreateCardProvidedWithInvalidInputs(final RequestInfo requestInfo) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> cardService.getNotificationCards(requestInfo));
    }

    @Test public void canGetCards() {
        when(mockTimeOffTaskService.getApprovalTasks(BASE_URL, CONNECTOR_AUTH, TENANT_NAME, LOCALE))
            .thenReturn(Flux.just(TIME_OFF_TASK, TIME_OFF_TASK_2));
        when(mockCardBuilderFactory.getCardBuilder(any(ApprovalTask.class))).thenReturn(mockTimeOffCardBuilder);
        final Cards cards = JsonUtils.convertFromJsonFile("cards/cards.json", Cards.class);
        when(mockTimeOffCardBuilder.createCard(any(TimeOffTask.class), any(RequestInfo.class)))
            .thenReturn(cards.getCards().get(0), cards.getCards().get(1));
        Mono<Cards> actualCardsMono = cardService.getNotificationCards(REQUEST_INFO);
        StepVerifier.create(actualCardsMono)
            .consumeNextWith(
                actualCards -> assertThatJson(actualCards).when(IGNORING_EXTRA_FIELDS, IGNORING_ARRAY_ORDER)
                    .isEqualTo(cards))
            .verifyComplete();
    }

    @Test public void canGetEmptyCardsWhenNoTimeTaskFound() {
        when(mockTimeOffTaskService.getApprovalTasks(BASE_URL, CONNECTOR_AUTH, TENANT_NAME, LOCALE))
            .thenReturn(Flux.empty());
        Mono<Cards> actualCardsMono = cardService.getNotificationCards(REQUEST_INFO);
        StepVerifier.create(actualCardsMono)
            .expectNextMatches(actualCards -> actualCards.getCards().isEmpty())
            .verifyComplete();
        verify(mockTimeOffCardBuilder, never()).createCard(any(TimeOffTask.class), any(RequestInfo.class));
    }

    @Test public void cannotGetCardsWhenGetTimeOffTasksErrorsOut() {
        when(mockTimeOffTaskService.getApprovalTasks(BASE_URL, CONNECTOR_AUTH, TENANT_NAME, LOCALE))
            .thenReturn(Flux.error(() -> new TimeOffTaskException("Workday API fails", HttpStatus.INTERNAL_SERVER_ERROR)));
        Mono<Cards> actualCardsMono = cardService.getNotificationCards(REQUEST_INFO);
        StepVerifier.create(actualCardsMono)
            .expectError(TimeOffTaskException.class)
            .verify(DURATION_2_SECONDS);
    }

    @Test public void canGetCardsWhenCardCreationFailsForSomeOfTimeOffTasks() {
        when(mockTimeOffTaskService.getApprovalTasks(BASE_URL, CONNECTOR_AUTH, TENANT_NAME, LOCALE))
            .thenReturn(Flux.just(TIME_OFF_TASK, TIME_OFF_TASK_2));
        when(mockCardBuilderFactory.getCardBuilder(any(ApprovalTask.class))).thenReturn(mockTimeOffCardBuilder);
        final Card card = JsonUtils.convertFromJsonFile("card.json", Card.class);
        when(mockTimeOffCardBuilder.createCard(any(TimeOffTask.class), any(RequestInfo.class)))
            .thenReturn(card)
            .thenThrow(RuntimeException.class);
        Mono<Cards> actualCardsMono = cardService.getNotificationCards(REQUEST_INFO);
        StepVerifier.create(actualCardsMono)
            .consumeNextWith(
                actualCards -> {
                    assertThat(actualCards.getCards()).hasSize(1);
                    assertThat(actualCards.getCards()).containsExactly(card);
                })
            .verifyComplete();
    }

}
