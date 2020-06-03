/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffTask;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.URI;
import java.util.Locale;
import java.util.stream.Stream;

import static com.vmware.ws1connectors.workday.test.JsonUtils.convertFromJsonFile;
import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.WORKDAY_CONNECTOR_CONTEXT_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
@MockitoSettings
public class CardBuilderTest {
    private static final String NO_ROUTING_PREFIX = null;
    private static final Locale NO_LOCALE = null;
    private static final Locale LOCALE = Locale.ENGLISH;
    private static final TimeOffTask NO_TIME_OFF_TASK = null;
    private static final ServerHttpRequest NO_REQUEST = null;
    private static final TimeOffTask TIME_OFF_TASK = convertFromJsonFile("time_off_task_1.json", TimeOffTask.class);
    private static final TimeOffTask MULTI_DAY_TIME_OFF_TASK = convertFromJsonFile("multi_day_time_off_task.json", TimeOffTask.class);
    private static final String ROUTING_PREFIX = "https://dev.hero.example.com/connectors/id";
    private static final ServerHttpRequest MOCK_REQUEST = mock(ServerHttpRequest.class);
    private static final String APPROVE = "Approve";
    private static final String DECLINE = "Decline";
    private static final String TEXT_PROPERTIES = "text";

    private CardTextAccessor cardTextAccessor;
    @Mock ServerProperties mockServerProperties;
    @InjectMocks private CardBuilder cardBuilder;

    @BeforeEach public void setup() {
        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename(TEXT_PROPERTIES);
        cardTextAccessor = new CardTextAccessor(messageSource);
        setField(cardBuilder, "cardTextAccessor", cardTextAccessor);
    }

    private void mockContextPathForServerProperties() {
        final ServerProperties.Servlet mockServlet = mock(ServerProperties.Servlet.class);
        when(mockServerProperties.getServlet()).thenReturn(mockServlet);
        when(mockServlet.getContextPath()).thenReturn(WORKDAY_CONNECTOR_CONTEXT_PATH);
    }

    private static Stream<Arguments> invalidInputsForCreateCard() {
        return new ArgumentsStreamBuilder()
            .add(NO_ROUTING_PREFIX, LOCALE, TIME_OFF_TASK, MOCK_REQUEST)
            .add(ROUTING_PREFIX, LOCALE, NO_TIME_OFF_TASK, MOCK_REQUEST)
            .add(ROUTING_PREFIX, NO_LOCALE, TIME_OFF_TASK, MOCK_REQUEST)
            .add(ROUTING_PREFIX, LOCALE, TIME_OFF_TASK, NO_REQUEST)
            .build();
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForCreateCard")
    public void whenCreateCardProvidedWithInvalidInputs(final String routingPrefix, final Locale locale,
                                                        final TimeOffTask timeOffTask, final ServerHttpRequest request) {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> cardBuilder.createCard(routingPrefix, locale, timeOffTask, request));
    }

    private static Stream<Arguments> getTimeOffTaskArguments() {
        return new ArgumentsStreamBuilder()
            .add(TIME_OFF_TASK, "card.json")
            .add(MULTI_DAY_TIME_OFF_TASK, "cards/multi_day_pto_card.json")
            .build();
    }

    @ParameterizedTest
    @MethodSource("getTimeOffTaskArguments")
    public void canBuildCard(final TimeOffTask timeOffTask, final String expectedCardJson) {
        when(MOCK_REQUEST.getURI()).thenReturn(URI.create(ROUTING_PREFIX));
        when(MOCK_REQUEST.getHeaders()).thenReturn(HttpHeaders.EMPTY);
        mockContextPathForServerProperties();
        final Card actualCard = cardBuilder.createCard(ROUTING_PREFIX, LOCALE, timeOffTask, MOCK_REQUEST);
        final Card expectedCard = JsonUtils.convertFromJsonFile(expectedCardJson, Card.class);
        assertThat(actualCard.getBackendId()).isEqualTo(expectedCard.getBackendId());
        assertThat(actualCard.getBody()).usingRecursiveComparison().isEqualTo(expectedCard.getBody());
        assertThat(actualCard.getHeader()).usingRecursiveComparison().isEqualTo(expectedCard.getHeader());
        assertThat(actualCard.getActions()).hasSize(2);
        verifyAction(actualCard, expectedCard, APPROVE);
        verifyAction(actualCard, expectedCard, DECLINE);
    }

    private void verifyAction(final Card actualCard, final Card expectedCard, final String actionLabel) {
        assertThat(actualCard.getActions()).filteredOn(cardAction -> actionLabel.equals(cardAction.getLabel()))
            .first()
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expectedCard.getActions()
                .stream()
                .filter(cardAction -> actionLabel.equals(cardAction.getLabel()))
                .findFirst()
                .get());
    }
}
