/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
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

class BusinessTitleChangeTaskTest {
    private static final String ID = "0db7bcdda1cd01fbdae2376a4844ab18";
    private static final LocalDate DUE = LocalDate.parse("2020-05-11");
    private static final LocalDate EFFECTIVE = LocalDate.parse("2020-05-07");
    private static final String DESCRIPTOR = "Title Change: Abhishek Anand (356404)";
    private static final String CURRENT_BUSINESS_TITLE = "Applications Developer";
    private static final String PROPOSED_BUSINESS_TITLE = "Applications Developer2";
    private static final LocalDateTime INITIATED =
            LocalDateTime.parse("2020-05-07T13:45:44.078Z", DateTimeFormatter.ISO_DATE_TIME);
    private BusinessTitleChangeTask businessTitleChangeTask =
            JsonUtils.convertFromJsonFile("business_title_change.json", BusinessTitleChangeTask.class);

    @Nested
    class TestGetters {

        @Test
        void returnsId() {
            assertThat(businessTitleChangeTask.getId()).isEqualTo(ID);
        }

        @Test
        void returnsDescriptor() {
            assertThat(businessTitleChangeTask.getDescriptor()).isEqualTo(DESCRIPTOR);
        }

        @Test
        void returnsDue() {
            assertThat(businessTitleChangeTask.getDue()).isEqualTo(DUE);
        }

        @Test
        void returnsEffective() {
            assertThat(businessTitleChangeTask.getEffective()).isEqualTo(EFFECTIVE);
        }

        @Test
        void returnsCurrentBusinessTitle() {
            assertThat(businessTitleChangeTask.getCurrentBusinessTitle()).isEqualTo(CURRENT_BUSINESS_TITLE);
        }

        @Test
        void returnsProposedBusinessTitle() {
            assertThat(businessTitleChangeTask.getProposedBusinessTitle()).isEqualTo(PROPOSED_BUSINESS_TITLE);
        }

        @Test
        void returnsInitiated() {
            assertThat(businessTitleChangeTask.getInitiated()).isEqualTo(INITIATED);
        }

        @Test
        void returnsInitiator() {
            String initiatorDescription = "Rahul Sahay (382449)";
            assertThat(businessTitleChangeTask.getInitiator().getDescriptor()).isEqualTo(initiatorDescription);
        }

        @Test
        void returnsSubjectDescriptor() {
            String subjectDescription = "Programmer/Analyst - Professional - Abhishek Anand (356404)";
            assertThat(businessTitleChangeTask.getSubject().getDescriptor()).isEqualTo(subjectDescription);
        }
    }
}
