/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.sfLeads.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Lead Object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Lead(
        @JsonProperty("Id")
        val id: String,
        @JsonProperty("Name")
        val name: String,
        @JsonProperty("Owner")
        val owner: Owner?,
        @JsonProperty("Company")
        val company: String,
        @JsonProperty("Email")
        val email: String?,
        @JsonProperty("Phone")
        val phone: String?,
        @JsonProperty("Status")
        val status: String,
        @JsonProperty("Description")
        val comments: String?,
        @JsonProperty("link")
        val link: String
)

/**
 * Owner Object
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Owner(
        @JsonProperty("Id")
        val id: String,
        @JsonProperty("Name")
        val name: String
)

/**
 * Lead Info Object
 */
data class LeadInfo(
        @JsonProperty("comments")
        val comments: String
)

