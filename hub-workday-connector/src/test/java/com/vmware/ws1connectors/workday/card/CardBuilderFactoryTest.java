/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.vmware.ws1connectors.workday.exceptions.InvalidApprovalTaskException;
import com.vmware.ws1connectors.workday.models.ApprovalTask;
import com.vmware.ws1connectors.workday.models.BusinessProcessTask;
import com.vmware.ws1connectors.workday.models.BusinessTitleChangeTask;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffTask;
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
class CardBuilderFactoryTest {

    private static final TimeOffTask TIME_OFF_TASK =
            JsonUtils.convertFromJsonFile("time_off_task_1.json", TimeOffTask.class);
    private static final BusinessTitleChangeTask BUSINESS_TITLE_CHANGE_TASK =
            JsonUtils.convertFromJsonFile("business_title_change.json", BusinessTitleChangeTask.class);
    private static final BusinessProcessTask BUSINESS_PROCESS_TASK =
            JsonUtils.convertFromJsonFile("Business_Process_Details.json", BusinessProcessTask.class);

    @Mock
    private TimeOffCardBuilder timeOffCardBuilder;
    @Mock
    private BusinessTitleChangeCardBuilder businessTitleChangeCardBuilder;
    @Mock
    private BusinessProcessCardBuilder businessProcessCardBuilder;
    @InjectMocks
    private CardBuilderFactory cardBuilderFactory;

    @Test
    void testGetApprovalTaskWithTimeOffTaskService() {
        assertThat(cardBuilderFactory.getCardBuilder(TIME_OFF_TASK)).isEqualTo(timeOffCardBuilder);
    }

    @Test
    void testGetApprovalTaskWithBusinessTitleChangeTaskService() {
        assertThat(cardBuilderFactory.getCardBuilder(BUSINESS_TITLE_CHANGE_TASK)).isEqualTo(businessTitleChangeCardBuilder);
    }

    @Test
    void testGetApprovalTaskWithBusinessProcessTaskService() {
        assertThat(cardBuilderFactory.getCardBuilder(BUSINESS_PROCESS_TASK)).isEqualTo(businessProcessCardBuilder);
    }

    @Test
    void testGetApprovalTaskWithHireTaskService() {
        assertThatExceptionOfType(InvalidApprovalTaskException.class)
                .isThrownBy(() -> cardBuilderFactory.getCardBuilder(new ApprovalTaskImplTest()));
    }

    @ParameterizedTest
    @NullSource
    void testGetApprovalTaskWithInBoxTaskNull(ApprovalTask approvalTask) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> cardBuilderFactory.getCardBuilder(approvalTask));
    }

    class ApprovalTaskImplTest implements ApprovalTask {
    }
}
