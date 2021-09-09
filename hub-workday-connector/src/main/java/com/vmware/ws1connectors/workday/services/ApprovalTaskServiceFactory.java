/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.ws1connectors.workday.models.InboxTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.vmware.ws1connectors.workday.utils.ApiUrlConstants.TIME_OFF_REQUEST_API_PATH;
import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;
import static com.vmware.ws1connectors.workday.utils.CardConstants.BUSINESS_PROCESS_PATH;
import static com.vmware.ws1connectors.workday.utils.CardConstants.BUSINESS_TITLE_CHANGE_PATH;

@Component
@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ApprovalTaskServiceFactory {
    private static final String INBOX_TASK = "Inbox Task";
    @Autowired private TimeOffTaskService timeOffTaskService;
    @Autowired private BusinessTitleChangeService businessTitleChangeService;
    @Autowired private BusinessProcessService businessProcessService;

    public Optional<ApprovalTaskService> getApprovalTaskService(InboxTask inboxTask) {
        checkArgumentNotNull(inboxTask, INBOX_TASK);
        LOGGER.info("Fetching ApprovalTaskService with id {} for approval type: status-descriptor: {}, stepType-descriptor: {} and overallProcessHref: {}",
                inboxTask.getId(), inboxTask.getStatus().getDescriptor(), inboxTask.getStepType().getDescriptor(), inboxTask.getOverallProcess().getHref());
        if (inboxTask.getOverallProcess().getHref().contains(TIME_OFF_REQUEST_API_PATH)) {
            return Optional.of(timeOffTaskService);
        } else if (inboxTask.getOverallProcess().getHref().contains(BUSINESS_TITLE_CHANGE_PATH)) {
            return Optional.of(businessTitleChangeService);
        } else if (inboxTask.getOverallProcess().getHref().contains(BUSINESS_PROCESS_PATH)) {
            return Optional.of(businessProcessService);
        }
        return Optional.empty();
    }
}
