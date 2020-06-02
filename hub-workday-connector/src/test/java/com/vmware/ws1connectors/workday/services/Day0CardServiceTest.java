/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.payloads.response.Cards;
import com.vmware.ws1connectors.workday.card.Day0CardBuilder;
import com.vmware.ws1connectors.workday.exceptions.UserNotFoundException;
import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.models.WorkdayUser;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import com.vmware.ws1connectors.workday.test.FileUtils;
import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static com.vmware.ws1connectors.workday.test.JsonUtils.convertToWorkdayResourceFromJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
public class Day0CardServiceTest extends ServiceTestsBase {

    private static final String NO_CONNECTOR_AUTH = null;
    private static final String NO_EMAIL = null;
    private static final Locale NO_LOCALE = null;
    private static final Locale LOCALE = Locale.ENGLISH;
    private static final String CONNECTOR_AUTH = "connectorAuth";
    private static final String EMAIL = "user1@example.com";
    private static final WorkdayUser WORKDAY_USER = getUser("user_info.json");
    private static final List<InboxTask> INBOX_TASKS = getInboxTasks("no_time_off_inbox_tasks.json");

    @Mock private InboxService mockInboxService;
    @Mock private UserService mockUserService;
    @Mock private Day0CardBuilder mockDay0CardBuilder;
    @InjectMocks private Day0CardService day0CardService;

    @Test public void testGetDay0CardsReturnsExpectedCards() {
        final Card card = JsonUtils.convertFromJsonFile("day0_card.json", Card.class);
        final Cards expectedCards = new Cards();
        expectedCards.getCards().add(card);
        mockServices(card);
        final Mono<Cards> actualCardsMono =
                day0CardService.getDay0Cards(BASE_URL, WORKDAY_TOKEN, EMAIL, LOCALE);
        StepVerifier.create(actualCardsMono).consumeNextWith(actualCards ->
                assertThatJson(actualCards).when(IGNORING_EXTRA_FIELDS, IGNORING_ARRAY_ORDER).isEqualTo(expectedCards))
                .verifyComplete();
    }

    private void mockServices(Card card) {
        when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.just(WORKDAY_USER));
        when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                .thenReturn(Flux.fromIterable(INBOX_TASKS));
        when(mockDay0CardBuilder.createCard(eq(BASE_URL), eq(LOCALE), any(List.class)))
                .thenReturn(card);
    }

    @Test public void whenNoInboxTasksPresentThenReturnsEmptyMono() {
        mockServicesWithNoInboxTask();
        final Mono<Cards> actualCardsMono =
                day0CardService.getDay0Cards(BASE_URL, WORKDAY_TOKEN, EMAIL, LOCALE);
        StepVerifier.create(actualCardsMono)
                .expectNextMatches(cards -> cards.getCards().isEmpty())
                .verifyComplete();
    }

    private void mockServicesWithNoInboxTask() {
        when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.just(WORKDAY_USER));
        when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                .thenReturn(Flux.empty());
    }

    @Test public void whenNoUserFoundThenReturnsEmptyMono() {
        mockServicesWithNoUserFound();
        final Mono<Cards> actualCardsMono =
                day0CardService.getDay0Cards(BASE_URL, WORKDAY_TOKEN, EMAIL, LOCALE);
        StepVerifier.create(actualCardsMono)
                .expectError(UserNotFoundException.class)
                .verify();
        verify(mockUserService).getUser(BASE_URL, WORKDAY_TOKEN, EMAIL);
        verifyGetTasksApiNeverInvoked(BASE_URL, WORKDAY_TOKEN);
    }

    private void mockServicesWithNoUserFound() {
        when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.empty());
    }

    private Flux<InboxTask> verifyGetTasksApiNeverInvoked(String baseUrl, String workdayAuth) {
        return verify(mockInboxService, never()).getTasks(eq(baseUrl), eq(workdayAuth), any(WorkdayUser.class));
    }

    @Test public void whenCardBuilderThrowsExceptionThenReturnsEmptyMono() {
        mockServicesWithCardBuilderThrowsException();
        final Mono<Cards> actualCardsMono =
                day0CardService.getDay0Cards(BASE_URL, WORKDAY_TOKEN, EMAIL, LOCALE);
        StepVerifier.create(actualCardsMono)
                .expectError(RuntimeException.class)
                .verify();
    }

    private void mockServicesWithCardBuilderThrowsException() {
        when(mockUserService.getUser(BASE_URL, WORKDAY_TOKEN, EMAIL))
                .thenReturn(Mono.just(WORKDAY_USER));
        when(mockInboxService.getTasks(BASE_URL, WORKDAY_TOKEN, WORKDAY_USER))
                .thenReturn(Flux.fromIterable(INBOX_TASKS));
        when(mockDay0CardBuilder.createCard(eq(BASE_URL), eq(LOCALE), any(List.class)))
                .thenThrow(RuntimeException.class);
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForBuildDay0Card")
    public void whenCreateCardProvidedWithInvalidInputs(final String baseUrl, final Locale locale,
                                                        final String connectorAuth, final String email) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> day0CardService.getDay0Cards(baseUrl, connectorAuth, email, locale));
    }

    private static Stream<Arguments> invalidInputsForBuildDay0Card() {
        return new ArgumentsStreamBuilder()
                .add(NO_BASE_URL, LOCALE, CONNECTOR_AUTH, EMAIL)
                .add(BASE_URL, LOCALE, NO_CONNECTOR_AUTH, EMAIL)
                .add(BASE_URL, NO_LOCALE, CONNECTOR_AUTH, EMAIL)
                .add(BASE_URL, LOCALE, CONNECTOR_AUTH, NO_EMAIL)
                .build();
    }

    private static WorkdayUser getUser(final String userInfoFile) {
        final String userInfo = FileUtils.readFileAsString(userInfoFile);
        return convertToWorkdayResourceFromJson(userInfo, WorkdayUser.class).getData().get(0);
    }

    private static List<InboxTask> getInboxTasks(final String inboxTasksFile) {
        final String inboxTasks = FileUtils.readFileAsString(inboxTasksFile);
        return convertToWorkdayResourceFromJson(inboxTasks, InboxTask.class).getData();
    }
}
