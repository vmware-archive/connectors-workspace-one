/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models.timeoff;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.vmware.ws1connectors.workday.models.Descriptor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeOffEvent {
    @JsonUnwrapped private Descriptor timeOffEventDescriptor;
    private LocalDate due;
    private Descriptor transactionStatus;
    @JsonProperty("timeOffEntry")
    private List<TimeOffEntry> timeOffEntries;
    private Descriptor subject;
    @JsonProperty("eventRecordsAwaitingAction")
    private List<Descriptor> eventRecordsAwaitingAction;
}
