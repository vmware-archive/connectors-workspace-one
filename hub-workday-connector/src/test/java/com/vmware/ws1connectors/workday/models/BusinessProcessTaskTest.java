/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models;

import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

public class BusinessProcessTaskTest {

    private static final String ID = "f387bb35571f013c819744b63a489314";
    private static final String DESCRIPTOR = "Transfer: Prachi Agarwal (33232)";
    private static final LocalDate EFFECTIVE = LocalDate.parse("2020-05-07");
    private static final LocalDate DUE = LocalDate.parse("2020-05-11");
    private static final LocalDateTime INITIATED =
            LocalDateTime.parse("2020-05-07T13:45:44.078Z", DateTimeFormatter.ISO_DATE_TIME);
    private InboxTask inboxTask = JsonUtils.convertFromJsonFile("inbox_task.json", InboxTask.class);
    private BusinessProcessTask businessProcessTask = buildBusinessProcessTaskFromSetters();

    private BusinessProcessTask buildBusinessProcessTaskFromSetters() {
        BusinessProcessTask businessProcessTask = new BusinessProcessTask();
        businessProcessTask.setDue(DUE);
        businessProcessTask.setDescriptor(DESCRIPTOR);
        businessProcessTask.setEffective(EFFECTIVE);
        businessProcessTask.setId(ID);
        businessProcessTask.setInitiated(INITIATED);
        businessProcessTask.setInboxTask(inboxTask);
        Subject subject = JsonUtils.convertFromJsonFile("Business_Title_Change_Subject.json", Subject.class);
        businessProcessTask.setSubject(subject);
        return businessProcessTask;
    }

    @Nested public class TestGetters {
        @Test public void returnsId() {
            assertThat(businessProcessTask.getId()).isEqualTo(ID);
        }

        @Test public void returnsDescriptor() {
            assertThat(businessProcessTask.getDescriptor()).isEqualTo(DESCRIPTOR);
        }

        @Test public void returnsEffective() {
            assertThat(businessProcessTask.getEffective()).isEqualTo(EFFECTIVE);
        }

        @Test public void returnsInitiated() {
            assertThat(businessProcessTask.getInitiated()).isEqualTo(INITIATED);
        }

        @Test public void returnsDue() {
            assertThat(businessProcessTask.getDue()).isEqualTo(DUE);
        }

        @Test public void returnsInboxTask() {
            assertThat(businessProcessTask.getInboxTask()).isEqualTo(inboxTask);
        }

        @Test public void returnsSubjectDescriptor() {
            String subjectDescription = "Programmer/Analyst - Professional - Abhishek Anand (356404)";
            assertThat(businessProcessTask.getSubject().getDescriptor()).isEqualTo(subjectDescription);
        }
    }
}
