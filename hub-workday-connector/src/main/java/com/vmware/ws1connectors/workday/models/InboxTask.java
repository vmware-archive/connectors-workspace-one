/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = InboxTask.Builder.class)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InboxTask {
    @JsonProperty("id")
    private String id;
    @JsonProperty("descriptor")
    private String descriptor;
    @JsonProperty("href")
    private String href;
    @JsonProperty("subject")
    private Descriptor subject;
    @JsonProperty("status")
    private Descriptor status;
    @JsonProperty("assigned")
    private LocalDateTime assigned;
    @JsonProperty("stepType")
    private Descriptor stepType;
    @JsonProperty("overallProcess")
    private Descriptor overallProcess;
    @JsonProperty("due")
    private LocalDate due;
}
