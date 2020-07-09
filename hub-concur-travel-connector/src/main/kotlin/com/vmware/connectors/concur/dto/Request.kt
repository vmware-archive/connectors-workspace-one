/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.concur.dto

import com.backflipt.commons.deserialize
import com.backflipt.commons.getStringOrException
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * TravelRequest Object.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class TravelRequest(
        @JsonProperty("RequestID")
        val requestID: String,
        @JsonProperty("RequestName")
        val requestName: String,
        @JsonProperty("ApproverLoginID")
        val approverLoginID: String,
        @JsonProperty("RequestDetailsUrl")
        val requestDetailsUrl: String
)

/**
 * TravelRequestInfo Object
 *
 */
data class TravelRequestInfo(
        @JsonProperty("comments")
        val comments: String,
        @JsonProperty("actionType")
        val actionType: WorkflowAction,
        @JsonProperty("message")
        private val travelRequest: String
) {
    val workflowActionURL = travelRequest
            .deserialize()
            .getStringOrException("workflow-action-url")
}

/**
 * A group of TravelRequestStatus
 */
enum class TravelRequestStatus {
    TOAPPROVE
}

/**
 * WorkflowAction Object
 */
enum class WorkflowAction {
    APPROVE, SEND_BACK;

    /**
     * this function will get the ActionString
     * @returns the ActionString
     */
    fun getActionString(): String = when (this) {
        APPROVE -> "Approve"
        SEND_BACK -> "Send back to Employee"
    }
}
