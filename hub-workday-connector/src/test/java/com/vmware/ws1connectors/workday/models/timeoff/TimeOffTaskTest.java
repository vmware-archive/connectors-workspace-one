/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models.timeoff;

import com.vmware.ws1connectors.workday.models.Descriptor;
import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeOffTaskTest {
    private static final TimeOffTask TIME_OFF_TASK = JsonUtils.convertFromJsonFile("time_off_task_1.json", TimeOffTask.class);
    private static final String TIME_OFF_REQUESTOR = "Ashton Burns";
    private static final String INBOX_TASK_ID = "fc844b7a8f6f813f79efb5ffd6115505";
    private static final String WORKDAY_API_URL = "https://workday.com/ccx/api/api/v1/tenant";
    private static final LocalDate DATE = LocalDate.parse("2019-12-11");

    @Nested public class GetDescriptor {
        @Test public void returnsDescriptor() {
            final String id = "fc844b7a8f6f01580738a5ffd6115105";
            final String descriptor = "Absence Request: Ashton Burns";
            final String href = WORKDAY_API_URL + "/timeOffRequest/fc844b7a8f6f01580738a5ffd6115105";
            final Descriptor expectedDescriptor = Descriptor.builder()
                .descriptor(descriptor)
                .id(id)
                .href(href)
                .build();
            assertThat(TIME_OFF_TASK.getTimeOffTaskDescriptor())
                .usingRecursiveComparison()
                .isEqualTo(expectedDescriptor);
        }
    }

    @Nested public class GetSubject {
        @Test public void returnsSubject() {
            assertThat(TIME_OFF_TASK.getSubject()).isEqualTo(TIME_OFF_REQUESTOR);
        }
    }

    @Nested public class GetAssignedOn {
        @Test public void returnsAssignedOn() {
            final LocalDateTime assignedOn = LocalDateTime.parse("2019-11-25T05:32:49.001Z", DateTimeFormatter.ISO_DATE_TIME);
            assertThat(TIME_OFF_TASK.getAssignedOn()).isEqualTo(assignedOn);
        }
    }

    @Nested public class GetStartDate {
        @Test public void returnsStartDate() {
            assertThat(TIME_OFF_TASK.getStartDate()).isEqualTo(DATE);
        }
    }

    @Nested public class GetEndDate {
        @Test public void returnsEndDate() {
            assertThat(TIME_OFF_TASK.getEndDate()).isEqualTo(DATE);
        }
    }

    @Nested public class GetTotalTimeOffDuration {
        @Test public void returnsTotalTimeOffDuration() {
            final String totalTimeOffDuration = "8 Hours-Sick (Hours)";
            assertThat(TIME_OFF_TASK.getTotalTimeOffDuration()).isEqualTo(totalTimeOffDuration);
        }
    }

    @Nested public class GetInboxTaskId {
        @Test public void returnsInboxTaskId() {
            assertThat(TIME_OFF_TASK.getInboxTaskId()).isEqualTo(INBOX_TASK_ID);
        }
    }

    @Nested public class GetInboxTaskHref {
        @Test public void returnsInboxTaskHref() {
            final String inboxTaskHref = new StringBuilder(WORKDAY_API_URL)
                .append("/inboxTasks/")
                .append(INBOX_TASK_ID)
                .toString();
            assertThat(TIME_OFF_TASK.getInboxTaskHref()).isEqualTo(inboxTaskHref);
        }
    }

    @Nested public class GetTimeOffItems {
        @Test public void returnsTimeOffItems() {
            final TimeOffItem expectedTimeOffItem = TimeOffItem.builder()
                .requestedTimeOffQuantity("8")
                .unitOfTime("Hours")
                .date(DATE)
                .dayOfWeek("Wednesday")
                .type("Sick (Hours)")
                .build();
            final List<TimeOffItem> actualTimeOffItems = TIME_OFF_TASK.getTimeOffItems();
            assertThat(actualTimeOffItems).hasSize(1);
            assertThat(actualTimeOffItems).usingFieldByFieldElementComparator()
                .containsExactly(expectedTimeOffItem);
        }
    }
}