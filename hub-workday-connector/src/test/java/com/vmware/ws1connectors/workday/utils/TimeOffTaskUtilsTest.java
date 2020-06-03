/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.utils;

import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffEvent;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffTask;
import com.vmware.ws1connectors.workday.test.ArgumentsStreamBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static com.vmware.ws1connectors.workday.test.JsonUtils.convertFromJsonFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class TimeOffTaskUtilsTest {
    private static final InboxTask NO_INBOX_TASK = null;
    private static final TimeOffEvent NO_TIME_OFF_EVENT = null;
    private static final Locale NO_LOCALE = null;
    private static final Locale LOCALE = Locale.ENGLISH;
    private static final InboxTask INBOX_TASK = convertFromJsonFile("inbox_task.json", InboxTask.class);
    private static final TimeOffEvent TIME_OFF_EVENT = convertFromJsonFile("time_off_request_1.json", TimeOffEvent.class);

    private static Stream<Arguments> invalidInputsForTimeOffTaskCreation() {
        return new ArgumentsStreamBuilder()
            .add(NO_INBOX_TASK, TIME_OFF_EVENT, LOCALE)
            .add(INBOX_TASK, NO_TIME_OFF_EVENT, LOCALE)
            .add(INBOX_TASK, TIME_OFF_EVENT, NO_LOCALE)
            .build();
    }

    @ParameterizedTest
    @MethodSource("invalidInputsForTimeOffTaskCreation")
    public void whenInvalidInputsProvidedForCreateTimeOffTask(final InboxTask inboxTask, final TimeOffEvent timeOffEvent, final Locale locale) {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> TimeOffTaskUtils.createTimeOffTask(inboxTask, timeOffEvent, locale));
    }

    @Test public void canCreateTimeOffTask() {
        final TimeOffTask actualTimeOffTask = TimeOffTaskUtils.createTimeOffTask(INBOX_TASK, TIME_OFF_EVENT, LOCALE);
        final TimeOffTask expectedTimeOffTask = convertFromJsonFile("time_off_task_1.json", TimeOffTask.class);

        assertThat(actualTimeOffTask).usingRecursiveComparison().isEqualTo(expectedTimeOffTask);
    }
}
