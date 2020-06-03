/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.ws1connectors.workday.card.CardBuilder;
import com.vmware.ws1connectors.workday.exceptions.TimeOffTaskException;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffTask;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PtoCardServiceTest extends ServiceTestsBase {
    private static final String NO_ROUTING_PREFIX = null;
    private static final String NO_CONNECTOR_AUTH = null;
    private static final String NO_EMAIL = null;
    private static final Locale NO_LOCALE = null;
    private static final Locale LOCALE = Locale.US;
    private static final ServerHttpRequest NO_REQUEST = null;
    private static final TimeOffTask TIME_OFF_TASK = convertFromJsonFile("time_off_task_1.json", TimeOffTask.class);
    private static final TimeOffTask TIME_OFF_TASK_2 = convertFromJsonFile("time_off_task_2.json", TimeOffTask.class);
    private static final String ROUTING_PREFIX = "https://dev.hero.example.com/connectors/id";
    private static final String CONNECTOR_AUTH = "connectorAuth";
    private static final String EMAIL = "user@example.com";
    private static final ServerHttpRequest MOCK_REQUEST = mock(ServerHttpRequest.class);
    private static final Duration DURATION_2_SECONDS = Duration.ofSeconds(2);

    @InjectMocks private PtoCardService ptoCardService;
    @Mock private TimeOffTaskService mockTimeOffTaskService;
    @Mock private CardBuilder mockCardBuilder;

    private static Stream<Arguments> invalidInputsForCreateCard() {
        return new ArgumentsStreamBuilder()
            .add(NO_BASE_URL, ROUTING_PREFIX, LOCALE, CONNECTOR_AUTH, MOCK_REQUEST, EMAIL)
            .add(BASE_URL, NO_ROUTING_PREFIX, LOCALE, CONNECTOR_AUTH, MOCK_REQUEST, EMAIL)
            .add(BASE_URL, ROUTING_PREFIX, LOCALE, NO_CONNECTOR_AUTH, MOCK_REQUEST, EMAIL)
            .add(BASE_URL, ROUTING_PREFIX, NO_LOCALE, CONNECTOR_AUTH, MOCK_REQUEST, EMAIL)
            .add(BASE_URL, ROUTING_PREFIX, LOCALE, CONNECTOR_AUTH, NO_REQUEST, EMAIL)
            .add(BASE_URL, ROUTING_PREFIX, LOCALE, CONNECTOR_AUTH, NO_REQUEST, NO_EMAIL)
            .build();
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForCreateCard")
    public void whenCreateCardProvidedWithInvalidInputs(final String baseUrl, final String routingPrefix, final Locale locale,
                                                        final String connectorAuth, final ServerHttpRequest request, final String email) {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> ptoCardService.getPtoCards(baseUrl, routingPrefix, connectorAuth, email, locale, request));
    }

    @Test public void canGetCards() {
        when(mockTimeOffTaskService.getTimeOffTasks(BASE_URL, CONNECTOR_AUTH, EMAIL, LOCALE))
            .thenReturn(Flux.just(TIME_OFF_TASK, TIME_OFF_TASK_2));
        final Cards cards = JsonUtils.convertFromJsonFile("cards/cards.json", Cards.class);
        when(mockCardBuilder.createCard(eq(ROUTING_PREFIX), eq(LOCALE), any(TimeOffTask.class), any(ServerHttpRequest.class)))
            .thenReturn(cards.getCards().get(0), cards.getCards().get(1));
        Mono<Cards> actualCardsMono = ptoCardService.getPtoCards(BASE_URL, ROUTING_PREFIX, CONNECTOR_AUTH, EMAIL, LOCALE, MOCK_REQUEST);
        StepVerifier.create(actualCardsMono)
            .consumeNextWith(
                actualCards -> assertThatJson(actualCards).when(IGNORING_EXTRA_FIELDS, IGNORING_ARRAY_ORDER)
                    .isEqualTo(cards))
            .verifyComplete();
    }

    @Test public void canGetEmptyCardsWhenNoTimeTaskFound() {
        when(mockTimeOffTaskService.getTimeOffTasks(BASE_URL, CONNECTOR_AUTH, EMAIL, LOCALE))
            .thenReturn(Flux.empty());
        Mono<Cards> actualCardsMono = ptoCardService.getPtoCards(BASE_URL, ROUTING_PREFIX, CONNECTOR_AUTH, EMAIL, LOCALE, MOCK_REQUEST);
        StepVerifier.create(actualCardsMono)
            .expectNextMatches(actualCards -> actualCards.getCards().isEmpty())
            .verifyComplete();
        verify(mockCardBuilder, never()).createCard(eq(ROUTING_PREFIX), eq(LOCALE), any(TimeOffTask.class), any(ServerHttpRequest.class));
    }

    @Test public void cannotGetCardsWhenGetTimeOffTasksErrorsOut() {
        when(mockTimeOffTaskService.getTimeOffTasks(BASE_URL, CONNECTOR_AUTH, EMAIL, LOCALE))
            .thenReturn(Flux.error(() -> new TimeOffTaskException("Workday API fails", HttpStatus.INTERNAL_SERVER_ERROR)));
        Mono<Cards> actualCardsMono = ptoCardService.getPtoCards(BASE_URL, ROUTING_PREFIX, CONNECTOR_AUTH, EMAIL, LOCALE, MOCK_REQUEST);
        StepVerifier.create(actualCardsMono)
            .expectError(TimeOffTaskException.class)
            .verify(DURATION_2_SECONDS);
    }

    @Test public void canGetCardsWhenCardCreationFailsForSomeOfTimeOffTasks() {
        when(mockTimeOffTaskService.getTimeOffTasks(BASE_URL, CONNECTOR_AUTH, EMAIL, LOCALE))
            .thenReturn(Flux.just(TIME_OFF_TASK, TIME_OFF_TASK_2));
        final Card card = JsonUtils.convertFromJsonFile("card.json", Card.class);
        when(mockCardBuilder.createCard(eq(ROUTING_PREFIX), eq(LOCALE), any(TimeOffTask.class), any(ServerHttpRequest.class)))
            .thenReturn(card)
            .thenThrow(RuntimeException.class);
        Mono<Cards> actualCardsMono = ptoCardService.getPtoCards(BASE_URL, ROUTING_PREFIX, CONNECTOR_AUTH, EMAIL, LOCALE, MOCK_REQUEST);
        StepVerifier.create(actualCardsMono)
            .consumeNextWith(
                actualCards -> {
                    assertThat(actualCards.getCards()).hasSize(1);
                    assertThat(actualCards.getCards()).containsExactly(card);
                })
            .verifyComplete();
    }

}
