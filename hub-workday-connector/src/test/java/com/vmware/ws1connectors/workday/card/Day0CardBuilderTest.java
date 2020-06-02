/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.common.utils.CardTextAccessor;
import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import com.vmware.ws1connectors.workday.test.FileUtils;
import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static com.vmware.ws1connectors.workday.test.JsonUtils.convertToWorkdayResourceFromJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
@MockitoSettings
public class Day0CardBuilderTest {

    private static final String NO_BASE_URL = null;
    private static final Locale NO_LOCALE = null;
    private static final Locale LOCALE = Locale.ENGLISH;
    private static final String BASE_URL = "http://workday.com";
    private static final String ROUTING_PREFIX = "https://dev.hero.example.com/connectors/id";
    private static final List<InboxTask> NO_TIME_OFF_INBOX_TASKS = getInboxTasks("no_time_off_inbox_tasks.json");
    private static final String TEXT_PROPERTIES = "text";
    private static final List<InboxTask> NO_INBOX_TASKS = null;
    private static final List<InboxTask> EMPTY_INBOX_TASKS = new ArrayList<>();
    private static final String ACTION_LABEL = "Open In Workday";
    private static final String EXPECTED_DAY0_CARD = "day0_card.json";
    private static final String CONNECTOR_IMAGE_DEFAULT_URL = "https://vmw-mf-assets.s3.amazonaws.com/connector-images/hub-workday.png";

    private CardTextAccessor cardTextAccessor;
    @InjectMocks private Day0CardBuilder cardBuilder;

    @BeforeEach public void setup() {
        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename(TEXT_PROPERTIES);
        cardTextAccessor = new CardTextAccessor(messageSource);
        setField(cardBuilder, "cardTextAccessor", cardTextAccessor);
        setField(cardBuilder, "connectorDefaultImageUrl", CONNECTOR_IMAGE_DEFAULT_URL);
    }

    @Test public void canBuildCard() {
        final Card actualCard = cardBuilder.createCard(BASE_URL, LOCALE, NO_TIME_OFF_INBOX_TASKS);
        final Card expectedCard = JsonUtils.convertFromJsonFile(EXPECTED_DAY0_CARD, Card.class);
        assertThat(actualCard.getBody()).usingRecursiveComparison().isEqualTo(expectedCard.getBody());
        assertThat(actualCard.getHeader()).usingRecursiveComparison().isEqualTo(expectedCard.getHeader());
        assertThat(actualCard.getImage().getHref()).isEqualTo(expectedCard.getImage().getHref());
        assertThat(actualCard.getActions()).hasSize(1);
        verifyAction(actualCard, expectedCard, ACTION_LABEL);
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForCreateDay0Card")
    public void whenCreateCardProvidedWithInvalidInputs(final String baseUrl, final Locale locale,
                                                        final List<InboxTask> inboxTasks) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> cardBuilder.createCard(baseUrl, locale, inboxTasks));
    }

    private static Stream<Arguments> invalidInputsForCreateDay0Card() {
        return new ArgumentsStreamBuilder()
                .add(NO_BASE_URL, LOCALE, NO_TIME_OFF_INBOX_TASKS)
                .add(BASE_URL, LOCALE, NO_INBOX_TASKS)
                .add(BASE_URL, LOCALE, EMPTY_INBOX_TASKS)
                .add(BASE_URL, NO_LOCALE, NO_TIME_OFF_INBOX_TASKS)
                .build();
    }

    private static List<InboxTask> getInboxTasks(final String inboxTasksFile) {
        final String inboxTasks = FileUtils.readFileAsString(inboxTasksFile);
        return convertToWorkdayResourceFromJson(inboxTasks, InboxTask.class).getData();
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
