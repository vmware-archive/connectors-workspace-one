/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.card;

import com.vmware.connectors.common.payloads.response.Card;
import com.vmware.ws1connectors.workday.models.ApprovalTask;
import com.vmware.ws1connectors.workday.models.RequestInfo;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Validated
public interface NotificationCardBuilder<T extends ApprovalTask> {
    Card createCard(@NotNull T approvalTask, @NotNull RequestInfo requestInfo);
}
