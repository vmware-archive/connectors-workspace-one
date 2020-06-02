/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.web.resources;

import com.google.common.collect.Lists;
import com.vmware.ws1connectors.workday.models.WorkdayUser;
import com.vmware.ws1connectors.workday.test.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;

import static com.vmware.ws1connectors.workday.test.JsonUtils.convertToWorkdayResourceFromJson;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings
public class WorkdayResourceTest {
    private static final String USER_INFO = FileUtils.readFileAsString("user_info.json");
    private static final String NO_USER_INFO = FileUtils.readFileAsString("no_data_total_zero.json");
    private static final String NO_USER_INFO_TOTAL_NON_ZERO = FileUtils.readFileAsString("no_data_total_non_zero.json");
    private static final String USER_INFO_TOTAL_ZERO = FileUtils.readFileAsString("user_info_total_zero.json");

    private static final int TOTAL = 1;
    private static final String USER_NAME = "user1";
    private static final String EMAIL = "user1@example.com";
    private static final String WORKER_ID = "247$137";
    private static final String FULL_NAME = "User1 Test";
    private static final String INSTANCE_ID = "39$189";
    private static final String WORDAY_ID = "0a2ee91d9f8f412dbf093881608f5d4e";
    private static final String LOCALE = "en_US";

    private WorkdayResource<WorkdayUser> workdayResource;

    @BeforeEach public void setup() {
        workdayResource = convertToWorkdayResourceFromJson(USER_INFO, WorkdayUser.class);
    }

    @Nested public class GetTotal {
        @Test public void returnsTotal() {
            assertThat(workdayResource.getTotal()).isEqualTo(TOTAL);
        }
    }

    @Nested public class GetData {
        @Test public void returnsData() {
            final WorkdayUser workdayUser = WorkdayUser.builder()
                .userName(USER_NAME)
                .workdayID(WORDAY_ID)
                .workerID(WORKER_ID)
                .email(EMAIL)
                .fullName(FULL_NAME)
                .instanceID(INSTANCE_ID)
                .locale(LOCALE)
                .build();
            assertThat(workdayResource.getData()).hasSize(1);
            assertThat(workdayResource.getData()).containsExactlyElementsOf(Lists.newArrayList(workdayUser));
        }
    }

    @Nested
    @DisplayName("Tests hasData() method")
    public class HasDataSet {

        @Test public void hasData() {
            assertThat(workdayResource.hasData()).isTrue();
        }

        @ParameterizedTest
        @MethodSource("com.vmware.ws1connectors.workday.web.resources.WorkdayResourceTest#workdayResourceHasNoDataInputs")
        public void hasNoData(final String workdayResource) {
            final WorkdayResource<WorkdayUser> emptyWorkDayResource = convertToWorkdayResourceFromJson(workdayResource, WorkdayUser.class);
            assertThat(emptyWorkDayResource.hasData()).isFalse();
        }
    }

    private static List<String> workdayResourceHasNoDataInputs() {
        return Lists.newArrayList(NO_USER_INFO, NO_USER_INFO_TOTAL_NON_ZERO, USER_INFO_TOTAL_ZERO);
    }
}
