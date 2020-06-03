/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models.timeoff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeOffItemTest {
    private static final LocalDate DATE = LocalDate.now();
    private static final String REQUESTED_TIME_OFF_QUANTITY = "8";
    private static final String UNIT_OF_TIME = "Hours";
    private static final String DAY_OF_WEEK = "Wednesday";
    private static final String TIME_OFF_TYPE = "Sick (Hours)";

    private TimeOffItem timeOffItem;

    @BeforeEach public void setupTimeOffItem() {
        timeOffItem = TimeOffItem.builder()
            .requestedTimeOffQuantity(REQUESTED_TIME_OFF_QUANTITY)
            .unitOfTime(UNIT_OF_TIME)
            .date(DATE)
            .dayOfWeek(DAY_OF_WEEK)
            .type(TIME_OFF_TYPE)
            .build();
    }

    @Nested public class GetRequestedTimeOffQuantity {
        @Test public void returnsRequestedTimeOffQuantity() {
            assertThat(timeOffItem.getRequestedTimeOffQuantity()).isEqualTo(REQUESTED_TIME_OFF_QUANTITY);
        }
    }

    @Nested public class GetUnitOfTime {
        @Test public void returnsUnitOfTime() {
            assertThat(timeOffItem.getUnitOfTime()).isEqualTo(UNIT_OF_TIME);
        }
    }

    @Nested public class GetDate {
        @Test public void returnsDate() {
            assertThat(timeOffItem.getDate()).isEqualTo(DATE);
        }
    }

    @Nested public class GetDayOfWeek {
        @Test public void returnsDayOfWeek() {
            assertThat(timeOffItem.getDayOfWeek()).isEqualTo(DAY_OF_WEEK);
        }
    }

    @Nested public class GetTimeOffType {
        @Test public void returnsTimeOffType() {
            assertThat(timeOffItem.getDayOfWeek()).isEqualTo(DAY_OF_WEEK);
        }
    }

}
