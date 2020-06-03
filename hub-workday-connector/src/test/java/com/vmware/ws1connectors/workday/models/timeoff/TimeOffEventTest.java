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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeOffEventTest {
    private static final String TIME_OFF_REQUESTOR = "Ashton Burns";
    private static final LocalDate DUE = LocalDate.parse("2019-11-26");
    private static final String INBOX_TASK_ID = "fc844b7a8f6f813f79efb5ffd6115505";
    private static final String WORKDAY_API_URL = "https://workday.com/ccx/api/api/v1/tenant";

    private static final TimeOffEvent TIME_OFF_EVENT = JsonUtils.convertFromJsonFile("time_off_request_1.json", TimeOffEvent.class);

    @Nested public class GetTimeOffEventDescriptor {
        @Test public void returnsTimeOffEventDescriptor() {
            final String id = "fc844b7a8f6f01580738a5ffd6115105";
            final String descriptor = "Absence Request: Ashton Burns";
            final String href = new StringBuilder(WORKDAY_API_URL)
                .append("/timeOffRequest/")
                .append(id)
                .toString();
            final Descriptor expectedDescriptor = Descriptor.builder()
                .descriptor(descriptor)
                .id(id)
                .href(href)
                .build();
            assertThat(TIME_OFF_EVENT.getTimeOffEventDescriptor())
                .usingRecursiveComparison()
                .isEqualTo(expectedDescriptor);
        }
    }

    @Nested public class GetDue {
        @Test public void returnsDue() {
            assertThat(TIME_OFF_EVENT.getDue()).isEqualTo(DUE);
        }
    }

    @Nested public class GetTransactionStatus {
        @Test public void returnsTransactionStatus() {
            final String descriptor = "In Progress";
            final String id = "e2d08afc53614c37b32b31270bb8bee3";
            final Descriptor transactionStatusDescriptor = Descriptor.builder()
                .descriptor(descriptor)
                .id(id)
                .build();
            assertThat(TIME_OFF_EVENT.getTransactionStatus())
                .usingRecursiveComparison()
                .isEqualTo(transactionStatusDescriptor);
        }
    }

    @Nested public class GetSubject {
        @Test public void returnsSubject() {
            final String id = "1a6228638d1401a7e91052d2b14af008";
            final String href = new StringBuilder(WORKDAY_API_URL)
                .append("/workers/")
                .append(id)
                .toString();
            final Descriptor expectedSubject = Descriptor.builder()
                .descriptor(TIME_OFF_REQUESTOR)
                .id(id)
                .href(href)
                .build();

            final Descriptor actualSubject = TIME_OFF_EVENT.getSubject();
            assertThat(actualSubject).usingRecursiveComparison()
                .isEqualTo(expectedSubject);
        }
    }

    @Nested public class GetTimeOffEntries {
        @Test public void returnsTimeOffEntries() {
            final TimeOffEntry expectedTimeOffEntry = JsonUtils.convertFromJsonFile("time_off_entry.json", TimeOffEntry.class);

            final List<TimeOffEntry> actualTimeOffEntries = TIME_OFF_EVENT.getTimeOffEntries();
            assertThat(actualTimeOffEntries).hasSize(1);
            assertThat(actualTimeOffEntries).usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expectedTimeOffEntry);
        }
    }

    @Nested public class GetEventRecordsAwaitingAction {
        @Test public void returnsEventRecordsAwaitingAction() {
            final String descriptor = "Approval by Manager";
            final String href = new StringBuilder(WORKDAY_API_URL)
                .append("/inboxTasks/")
                .append(INBOX_TASK_ID)
                .toString();
            final Descriptor expectedActionAwaitingEventRecords = Descriptor.builder()
                .descriptor(descriptor)
                .id(INBOX_TASK_ID)
                .href(href)
                .build();

            final List<Descriptor> actualActionAwaitingEventEventRecords = TIME_OFF_EVENT.getEventRecordsAwaitingAction();
            assertThat(actualActionAwaitingEventEventRecords).hasSize(1);
            assertThat(actualActionAwaitingEventEventRecords).usingFieldByFieldElementComparator()
                .containsExactly(expectedActionAwaitingEventRecords);
        }
    }
}
