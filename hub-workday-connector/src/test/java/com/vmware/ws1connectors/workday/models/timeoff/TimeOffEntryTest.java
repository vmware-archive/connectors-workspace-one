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

import static org.assertj.core.api.Assertions.assertThat;

public class TimeOffEntryTest {
    private static final LocalDate DATE = LocalDate.parse("2019-12-11");
    private static final String TIME_OFF_REQUESTOR = "Ashton Burns";
    private static final String WORKDAY_API_URL = "https://workday.com/ccx/api/api/v1/tenant";

    private static final TimeOffEntry TIME_OFF_ENTRY = JsonUtils.convertFromJsonFile("time_off_entry.json", TimeOffEntry.class);

    @Nested public class GetTimeOffEntryDescriptor {
        @Test public void returnsTimeOffEntryDescriptor() {
            final String id = "fc844b7a8f6f01927d3da5ffd6115205";
            final String descriptor = "12/11/2019 - 8 Hours (Ashton Burns)";
            final Descriptor expectedDescriptor = Descriptor.builder()
                .descriptor(descriptor)
                .id(id)
                .build();

            assertThat(TIME_OFF_ENTRY.getTimeOffEntryDescriptor())
                .usingRecursiveComparison()
                .isEqualTo(expectedDescriptor);
        }
    }

    @Nested public class GetDate {
        @Test public void returnsDue() {
            assertThat(TIME_OFF_ENTRY.getDate()).isEqualTo(DATE);
        }
    }

    @Nested public class GetEmployee {
        @Test public void returnsEmployee() {
            final String id = "1a6228638d1401a7e91052d2b14af008";
            final String href = new StringBuilder(WORKDAY_API_URL)
                .append("/workers/")
                .append(id)
                .toString();
            final Descriptor expectedEmployee = Descriptor.builder()
                .descriptor(TIME_OFF_REQUESTOR)
                .id(id)
                .href(href)
                .build();

            assertThat(TIME_OFF_ENTRY.getEmployee()).usingRecursiveComparison()
                .isEqualTo(expectedEmployee);
        }
    }

    @Nested public class GetUnitOfTime {
        @Test public void returnsUnitOfTime() {
            final String descriptor = "Hours";
            final String id = "c4dacbd56bca4a9a8950e8d3ed21bbdb";
            final Descriptor expectedUnitOfTime = Descriptor.builder()
                .descriptor(descriptor)
                .id(id)
                .build();

            assertThat(TIME_OFF_ENTRY.getUnitOfTime())
                .usingRecursiveComparison()
                .isEqualTo(expectedUnitOfTime);
        }
    }

    @Nested public class GetTimeOffRequest {
        @Test public void returnsTimeOffRequest() {
            final String id = "fc844b7a8f6f01580738a5ffd6115105";
            final String descriptor = "Absence Request: Ashton Burns";
            final String href = new StringBuilder(WORKDAY_API_URL)
                .append("/timeOffRequest/")
                .append(id)
                .toString();
            final Descriptor expectedTimeOffDescriptor = Descriptor.builder()
                .descriptor(descriptor)
                .id(id)
                .href(href)
                .build();

            assertThat(TIME_OFF_ENTRY.getTimeOffRequest())
                .usingRecursiveComparison()
                .isEqualTo(expectedTimeOffDescriptor);
        }
    }

    @Nested public class GetUnits {
        @Test public void returnsUnits() {
            final String units = "8";
            assertThat(TIME_OFF_ENTRY.getUnits()).isEqualTo(units);
        }
    }

    @Nested public class GetTimeOffDetails {
        @Test public void returnsTimeOffDetails() {
            final String id = "14bb677ad94b4bc6acc87d6a652f1a9f";
            final String descriptor = "Sick (Hours)";
            final String href = new StringBuilder(WORKDAY_API_URL)
                .append("/timeOffs/")
                .append(id)
                .toString();
            final Descriptor expectedTimeOffDescriptor = Descriptor.builder()
                .descriptor(descriptor)
                .id(id)
                .href(href)
                .build();

            assertThat(TIME_OFF_ENTRY.getTimeOff())
                .usingRecursiveComparison()
                .isEqualTo(expectedTimeOffDescriptor);
        }
    }
}
