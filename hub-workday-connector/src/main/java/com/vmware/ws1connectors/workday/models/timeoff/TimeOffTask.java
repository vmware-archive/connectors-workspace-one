/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models.timeoff;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.vmware.ws1connectors.workday.models.ApprovalTask;
import com.vmware.ws1connectors.workday.models.Descriptor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeOffTask implements ApprovalTask {
    @JsonUnwrapped private Descriptor timeOffTaskDescriptor;
    private String subject;
    private LocalDateTime assignedOn;
    private String overallStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private String totalTimeOffDuration;
    private String inboxTaskId;
    private String inboxTaskHref;
    private List<TimeOffItem> timeOffItems;
}
