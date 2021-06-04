/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.test.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
public class ApprovalTaskServiceFactoryTest {

    private static final InboxTask INBOX_TASK_FOR_TIME_OFF =
            JsonUtils.convertFromJsonFile("inbox_task.json", InboxTask.class);
    private static final InboxTask INBOX_TASK_FOR_BUSINESS_TITLE_CHANGE =
            JsonUtils.convertFromJsonFile("Inbox_Task_Business_Title_Change.json", InboxTask.class);
    private static final InboxTask INBOX_TASK_FOR_BUSINESS_PROCESS =
            JsonUtils.convertFromJsonFile("Inbox_Task_Business_Process.json", InboxTask.class);
    private static final InboxTask INBOX_TASK_HIRE =
            JsonUtils.convertFromJsonFile("inbox_task_onboarding.json", InboxTask.class);

    @Mock private TimeOffTaskService timeOffTaskService;
    @Mock private BusinessTitleChangeService businessTitleChangeService;
    @Mock private BusinessProcessService businessProcessService;
    @InjectMocks private ApprovalTaskServiceFactory approvalTaskServiceFactory;

    @Test public void testGetApprovalTaskWithTimeOffTaskService() {
        assertThat(approvalTaskServiceFactory.getApprovalTaskService(INBOX_TASK_FOR_TIME_OFF)).isNotEmpty();
        assertThat(approvalTaskServiceFactory.getApprovalTaskService(INBOX_TASK_FOR_TIME_OFF))
                .hasValue(timeOffTaskService);
    }

    @Test public void testGetApprovalTaskWithBusinessTitleChangeTaskService() {
        assertThat(approvalTaskServiceFactory.getApprovalTaskService(INBOX_TASK_FOR_BUSINESS_TITLE_CHANGE))
                .isNotEmpty();
        assertThat(approvalTaskServiceFactory.getApprovalTaskService(INBOX_TASK_FOR_BUSINESS_TITLE_CHANGE))
                .hasValue(businessTitleChangeService);
    }

    @Test public void testGetApprovalTaskWithBusinessProcessTaskService() {
        assertThat(approvalTaskServiceFactory.getApprovalTaskService(INBOX_TASK_FOR_BUSINESS_PROCESS))
                .isNotEmpty();
        assertThat(approvalTaskServiceFactory.getApprovalTaskService(INBOX_TASK_FOR_BUSINESS_PROCESS))
                .hasValue(businessProcessService);
    }

    @Test public void testGetApprovalTaskWithHireTaskService() {
        assertThat(approvalTaskServiceFactory.getApprovalTaskService(INBOX_TASK_HIRE)).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    public void testGetApprovalTaskWithInBoxTaskNull(InboxTask inboxTask) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> approvalTaskServiceFactory.getApprovalTaskService(inboxTask));
    }
}
