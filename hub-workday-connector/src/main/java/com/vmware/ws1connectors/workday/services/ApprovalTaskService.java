/*
 * Project Workday Connector
 * (c) 2020-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.services;

import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.models.ApprovalTask;
import reactor.core.publisher.Mono;

import java.util.Locale;

public interface ApprovalTaskService {
    Mono<? extends ApprovalTask> getApprovalTaskDetails(String baseUrl, String workdayAccessToken, InboxTask inboxTask, Locale locale);
}
