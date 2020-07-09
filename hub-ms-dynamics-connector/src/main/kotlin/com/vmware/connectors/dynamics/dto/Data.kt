/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.dynamics.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.vmware.connectors.dynamics.config.DATE_FORMAT_PATTERN
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class Account(
        @JsonProperty("accountid")
        val id: String,
        @JsonProperty("name")
        val name: String,
        @JsonProperty("telephone1")
        val phoneNo: String?,
        @JsonProperty("description")
        val description: String?,
        @JsonProperty("websiteurl")
        val websiteurl: String?,
        @JsonProperty("primaryContact")
        val primaryContact: Contact?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Contact(
        @JsonProperty("_parentcustomerid_value")
        val accountId: String,
        @JsonProperty("yomifullname")
        val name: String,
        @JsonProperty("emailaddress1")
        val emailId: String?
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateTaskRequest(
        @JsonProperty("comments")
        val comments: String,
        @JsonProperty("description")
        val description: String?,
        @DateTimeFormat(pattern = DATE_FORMAT_PATTERN)
        val dueDate: LocalDateTime?
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateAppointmentRequest(
        val comments: String,
        @DateTimeFormat(pattern = DATE_FORMAT_PATTERN)
        val startTime: LocalDateTime?,
        @DateTimeFormat(pattern = DATE_FORMAT_PATTERN)
        val endTime: LocalDateTime?
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class CreatePhoneCallRequest(
        @JsonProperty("comments")
        val comments: String,
        @JsonProperty("description")
        val description: String?,
        @JsonProperty("phoneNo")
        val phoneNo: String?,
        @DateTimeFormat(pattern = DATE_FORMAT_PATTERN)
        val dueDate: LocalDateTime?
)