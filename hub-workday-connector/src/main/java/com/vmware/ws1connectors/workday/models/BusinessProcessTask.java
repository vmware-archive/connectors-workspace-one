/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessProcessTask implements ApprovalTask {
    private String id;
    private InboxTask inboxTask;
    private LocalDate due;
    private String descriptor;
    private LocalDate effective;
    private LocalDateTime initiated;
    private Initiator initiator;
    private Subject subject;
}
