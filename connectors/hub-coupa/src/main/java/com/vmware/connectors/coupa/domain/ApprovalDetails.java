/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.coupa.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApprovalDetails {

	@JsonProperty("id")
	private String id;
	@JsonProperty("created-at")
	private String createdAt;
	@JsonProperty("updated-at")
	private String updatedAt;
	@JsonProperty("position")
	private String position;
	@JsonProperty("approval-chain-id")
	private String approvalChainId;
	@JsonProperty("status")
	private String status;
	@JsonProperty("approval-date")
	private String approvalDate;
	@JsonProperty("approvable-type")
	private String approvableType;
	@JsonProperty("approvable-id")
	private String approvableId;
	@JsonProperty("approver")
	private UserDetails approver;
	@JsonProperty("created-by")
	private UserDetails createdBy;
	@JsonProperty("updated-by")
	private UserDetails updatedBy;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public String getApprovalChainId() {
		return approvalChainId;
	}

	public void setApprovalChainId(String approvalChainId) {
		this.approvalChainId = approvalChainId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getApprovalDate() {
		return approvalDate;
	}

	public void setApprovalDate(String approvalDate) {
		this.approvalDate = approvalDate;
	}

	public String getApprovableType() {
		return approvableType;
	}

	public void setApprovableType(String approvableType) {
		this.approvableType = approvableType;
	}

	public String getApprovableId() {
		return approvableId;
	}

	public void setApprovableId(String approvableId) {
		this.approvableId = approvableId;
	}

	public UserDetails getApprover() {
		return approver;
	}

	public void setApprover(UserDetails approver) {
		this.approver = approver;
	}

	public UserDetails getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UserDetails createdBy) {
		this.createdBy = createdBy;
	}

	public UserDetails getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(UserDetails updatedBy) {
		this.updatedBy = updatedBy;
	}

}