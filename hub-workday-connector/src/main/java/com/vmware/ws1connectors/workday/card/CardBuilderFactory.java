/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.vmware.ws1connectors.workday.exceptions.InvalidApprovalTaskException;
import com.vmware.ws1connectors.workday.models.ApprovalTask;
import com.vmware.ws1connectors.workday.models.BusinessProcessTask;
import com.vmware.ws1connectors.workday.models.BusinessTitleChangeTask;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffTask;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;

@Component
@SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class CardBuilderFactory {
    private static final String APPROVAL_TASK = "Approval task";
    @Autowired private TimeOffCardBuilder timeOffCardBuilder;
    @Autowired private BusinessTitleChangeCardBuilder businessTitleChangeCardBuilder;
    @Autowired private BusinessProcessCardBuilder businessProcessCardBuilder;

    public NotificationCardBuilder getCardBuilder(ApprovalTask approvalTask) {
        checkArgumentNotNull(approvalTask, APPROVAL_TASK);

        if (approvalTask instanceof TimeOffTask) {
            LOGGER.info("NotificationCardBuilder for ApprovalTask: {} NotificationBuilderClass: {} ",
                    approvalTask, timeOffCardBuilder);
            return timeOffCardBuilder;
        } else if (approvalTask instanceof BusinessTitleChangeTask) {
            LOGGER.info("NotificationCardBuilder for ApprovalTask: {} NotificationBuilderClass: {} ",
                    approvalTask, businessTitleChangeCardBuilder);
            return businessTitleChangeCardBuilder;
        } else if (approvalTask instanceof BusinessProcessTask) {
            LOGGER.info("NotificationCardBuilder for ApprovalTask: {} NotificationBuilderClass: {} ",
                    approvalTask, businessProcessCardBuilder);
            return businessProcessCardBuilder;
        }
        throw new InvalidApprovalTaskException();
    }
}
