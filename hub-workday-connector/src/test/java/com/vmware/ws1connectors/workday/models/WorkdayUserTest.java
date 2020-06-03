/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models;

import com.vmware.ws1connectors.workday.test.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import static com.vmware.ws1connectors.workday.test.JsonUtils.convertToWorkdayResourceFromJson;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings
public class WorkdayUserTest {
    private static final String USER_NAME = "user1";
    private static final String EMAIL = "user1@example.com";
    private static final String WORKER_ID = "247$137";
    private static final String FULL_NAME = "User1 Test";
    private static final String INSTANCE_ID = "39$189";
    private static final String WORDAY_ID = "0a2ee91d9f8f412dbf093881608f5d4e";
    private static final String LOCALE = "en_US";

    private static final String USER_INFO = FileUtils.readFileAsString("user_info.json");

    private WorkdayUser workdayUser;

    @BeforeEach public void setup() {
        workdayUser = new WorkdayUser.Builder()
            .userName(USER_NAME)
            .workdayID(WORDAY_ID)
            .workerID(WORKER_ID)
            .email(EMAIL)
            .fullName(FULL_NAME)
            .instanceID(INSTANCE_ID)
            .locale(LOCALE)
            .build();
    }

    @Nested public class GetUserName {
        @Test public void returnsUserName() {
            assertThat(workdayUser.getUserName()).isEqualTo(USER_NAME);
        }
    }

    @Nested public class GetFullName {
        @Test public void returnsFullName() {
            assertThat(workdayUser.getFullName()).isEqualTo(FULL_NAME);
        }
    }

    @Nested public class GetWorkerID {
        @Test public void returnsWorkerID() {
            assertThat(workdayUser.getWorkdayID()).isEqualTo(WORDAY_ID);
        }
    }

    @Nested public class GetWorkdayID {
        @Test public void returnsWorkdayID() {
            assertThat(workdayUser.getWorkdayID()).isEqualTo(WORDAY_ID);
        }
    }

    @Nested public class GetInstanceID {
        @Test public void returnsInstanceID() {
            assertThat(workdayUser.getInstanceID()).isEqualTo(INSTANCE_ID);
        }
    }

    @Nested public class GetLocale {
        @Test public void returnsLocale() {
            assertThat(workdayUser.getLocale()).isEqualTo(LOCALE);
        }
    }

    @Nested public class EqualsAndHashCode {
        private WorkdayUser otherWorkdayUser = convertToWorkdayResourceFromJson(USER_INFO, WorkdayUser.class).getData().get(0);

        @Test public void equalsToOtherWorkDay() {
            assertThat(workdayUser).isEqualTo(otherWorkdayUser);
        }

        @Test public void hashCodeIsSameAsOtherWorkdayUser() {
            assertThat(workdayUser.hashCode()).isEqualTo(otherWorkdayUser.hashCode());
        }
    }

}
