/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.connectors.test.ControllerTestsBase;
import com.vmware.ws1connectors.workday.models.RequestInfo;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffTask;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintViolationException;
import java.util.Locale;
import java.util.stream.Stream;

import static com.vmware.ws1connectors.workday.test.JsonUtils.convertFromJsonFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TimeOffCardBuilderTest extends ControllerTestsBase {
    private static final TimeOffTask TIME_OFF_TASK = convertFromJsonFile("time_off_task_1.json", TimeOffTask.class);
    private static final TimeOffTask MULTI_DAY_TIME_OFF_TASK = convertFromJsonFile("multi_day_time_off_task.json", TimeOffTask.class);
    private static final String ROUTING_PREFIX = "https://dev.hero.example.com/connectors/id/";
    private static final String NO_ROUTING_PREFIX = null;
    private static final TimeOffTask NO_TIME_OFF_TASK = null;
    private static final Locale NO_LOCALE = null;
    private static final String NO_TENANT_NAME = null;
    private static final String APPROVE = "Approve";
    private static final String DECLINE = "Decline";
    private static final String TENANT_NAME = "vmware_gms";
    private static final String TENANT_URL = "https://impl.workday.com";
    private static final String NO_TENANT_URL = null;
    private static final RequestInfo NO_REQUEST_INFO = null;
    private static final RequestInfo REQUEST_INFO = RequestInfo.builder().tenantName(TENANT_NAME)
            .routingPrefix(ROUTING_PREFIX)
            .tenantUrl(TENANT_URL)
            .isPreHire(false)
            .locale(Locale.ENGLISH)
            .build();

    @Autowired
    private TimeOffCardBuilder timeOffCardBuilder;

    private static Stream<Arguments> invalidInputsForCreateCard() {
        return new ArgumentsStreamBuilder()
                .add(TIME_OFF_TASK, RequestInfo.builder().routingPrefix(NO_ROUTING_PREFIX).build())
                .add(TIME_OFF_TASK, RequestInfo.builder().tenantName(NO_TENANT_NAME).build())
                .add(TIME_OFF_TASK, RequestInfo.builder().tenantUrl(NO_TENANT_URL).build())
                .add(TIME_OFF_TASK, RequestInfo.builder().locale(NO_LOCALE).build())
                .build();
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForCreateCard")
    void whenCreateCardProvidedWithInvalidInputs(final TimeOffTask timeOffTask, final RequestInfo requestInfo) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> timeOffCardBuilder.createCard(timeOffTask, requestInfo));
    }

    private static Stream<Arguments> nullInputsForCreateCard() {
        return new ArgumentsStreamBuilder()
                .add(TIME_OFF_TASK, NO_REQUEST_INFO)
                .add(NO_TIME_OFF_TASK, REQUEST_INFO)
                .build();
    }

    @ParameterizedTest
    @MethodSource("nullInputsForCreateCard")
    void whenCreateCardProvidedWithNullInputs(final TimeOffTask timeOffTask, final RequestInfo requestInfo) {
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> timeOffCardBuilder.createCard(timeOffTask, requestInfo));
    }

    private static Stream<Arguments> getTimeOffTaskArguments() {
        return new ArgumentsStreamBuilder()
                .add(TIME_OFF_TASK, "card.json")
                .add(MULTI_DAY_TIME_OFF_TASK, "cards/multi_day_pto_card.json")
                .build();
    }

    @ParameterizedTest
    @MethodSource("getTimeOffTaskArguments")
    void canBuildCard(final TimeOffTask timeOffTask, final String expectedCardJson) {
        final Card actualCard = timeOffCardBuilder.createCard(timeOffTask, REQUEST_INFO);
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
