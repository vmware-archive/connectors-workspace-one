/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models;

import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings
class InboxTaskTest {

    private static final String DESCRIPTOR = "Approval by Manager";
    private static final LocalDateTime ASSIGNED = LocalDateTime.parse("2019-11-25T05:32:49.001Z", DateTimeFormatter.ISO_DATE_TIME);
    private static final LocalDate DUE = LocalDate.parse("2019-11-26");
    private static final String ID = "fc844b7a8f6f813f79efb5ffd6115505";
    private static final String WORKDAY_API_URL = "https://workday.com/ccx/api/api/v1/tenant";
    private static final String HREF = WORKDAY_API_URL + "/inboxTasks/" + ID;
    private static final String NO_HREF = null;

    private InboxTask inboxTask = JsonUtils.convertFromJsonFile("inbox_task.json", InboxTask.class);

    @Nested
    class GetDescriptor {
        @Test
        void returnsDescriptor() {
            assertThat(inboxTask.getDescriptor()).isEqualTo(DESCRIPTOR);
        }
    }

    @Nested
    class GetId {
        @Test
        void returnsId() {
            assertThat(inboxTask.getId()).isEqualTo(ID);
        }
    }

    @Nested
    class GetHref {
        @Test
        void returnsHref() {
            assertThat(inboxTask.getHref()).isEqualTo(HREF);
        }
    }

    @Nested
    class GetDue {
        @Test
        void returnsDue() {
            assertThat(inboxTask.getDue()).isEqualTo(DUE);
        }
    }

    @Nested
    class GetAssigned {
        @Test
        void returnsAssigned() {
            assertThat(inboxTask.getAssigned()).isEqualTo(ASSIGNED);
        }
    }

    @Nested
    class GetStepType {
        @Test
        void returnsStepType() {
            final String descriptor = "Approval";
            final String id = "d8c8920e446c11de98360015c5e6daf6";
            final Descriptor stepType = inboxTask.getStepType();
            verifyDescriptorValues(stepType, descriptor, id, NO_HREF);
        }
    }

    @Nested
    class GetOverallProcess {
        @Test
        void returnsOverallProcess() {
            final String id = "fc844b7a8f6f01580738a5ffd6115105";
            final String href = WORKDAY_API_URL + "/timeOffRequest/" + id;
            final String descriptor = "Absence Request: Ashton Burns";
            final Descriptor overallProcess = inboxTask.getOverallProcess();
            verifyDescriptorValues(overallProcess, descriptor, id, href);
        }
    }

    private void verifyDescriptorValues(final Descriptor descriptorObj, final String descriptor, final String id, final String href) {
        assertThat(descriptorObj.getDescriptor()).isEqualTo(descriptor);
        assertThat(descriptorObj.getId()).isEqualTo(id);
        assertThat(descriptorObj.getHref()).isEqualTo(href);
    }

    @Nested
    class GetStatus {
        @Test
        void returnsStatus() {
            final String descriptor = "Awaiting Action";
            final String id = "d9e4108c446c11de98360015c5e6daf6";
            final Descriptor status = inboxTask.getStatus();
            verifyDescriptorValues(status, descriptor, id, NO_HREF);
        }
    }

    @Nested
    class GetSubject {
        @Test
        void returnsSubject() {
            final String id = "1a6228638d1401a7e91052d2b14af008";
            final String href = WORKDAY_API_URL + "/workers/" + id;
            final String descriptor = "Ashton Burns";

            final Descriptor subject = inboxTask.getSubject();
            verifyDescriptorValues(subject, descriptor, id, href);
        }
    }
}
