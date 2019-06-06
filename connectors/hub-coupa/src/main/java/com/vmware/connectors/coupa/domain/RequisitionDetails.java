/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.connectors.coupa.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequisitionDetails {

    @JsonProperty("id")
    private String id;

    @JsonProperty("justification")
    private String justification;

    @JsonProperty("status")
    private String status;

    @JsonProperty("submitted-at")
    private String submittedAt;

    @JsonProperty("ship-to-attention")
    private String shipToAttention;

    @JsonProperty("mobile-total")
    private String mobileTotal;

    @JsonProperty("requisition-description")
    private String requisitionDescription;

    @JsonProperty("currency")
    private Currency currency;

    @JsonProperty("requested-by")
    private UserDetails requestedBy;

    @JsonProperty("current-approval")
    private Approval currentApproval;

    @JsonProperty("approvals")
    private List<Approval> approvals;

    @JsonProperty("created-by")
    private UserDetails createdBy;

    @JsonProperty("updated-by")
    private UserDetails updatedBy;

    @JsonProperty("mobile-currency")
    private Currency mobileCurrency;

    @JsonProperty("requestors-cost-center")
    private String requestorCostCenter;

    @JsonProperty("requisition-lines")
    private List<RequisitionLineDetails> requisitionLinesList;



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getShipToAttention() {
        return shipToAttention;
    }

    public void setShipToAttention(String shipToAttention) {
        this.shipToAttention = shipToAttention;
    }

    public String getMobileTotal() {
        return mobileTotal;
    }

    public void setMobileTotal(String mobileTotal) {
        this.mobileTotal = mobileTotal;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public UserDetails getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(UserDetails requestedBy) {
        this.requestedBy = requestedBy;
    }

    public List<Approval> getApprovals() {
        return approvals;
    }

    public void setApprovals(List<Approval> approvals) {
        this.approvals = approvals;
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

    public Currency getMobileCurrency() {
        return mobileCurrency;
    }

    public void setMobileCurrency(Currency mobileCurrency) {
        this.mobileCurrency = mobileCurrency;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequisitionDescription() {
        return requisitionDescription;
    }

    public void setRequisitionDescription(String requisitionDescription) {
        this.requisitionDescription = requisitionDescription;
    }

    public Approval getCurrentApproval() {
        return currentApproval;
    }

    public void setCurrentApproval(Approval currentApproval) {
        this.currentApproval = currentApproval;
    }

    public String getRequestorCostCenter() {
        return requestorCostCenter;
    }

    public void setRequestorCostCenter(String requestorCostCenter) {
        this.requestorCostCenter = requestorCostCenter;
    }

    public List<RequisitionLineDetails> getRequisitionLinesList() {
        return requisitionLinesList;
    }

    public void setRequisitionLinesList(List<RequisitionLineDetails> requisitionLinesList) {
        this.requisitionLinesList = requisitionLinesList;
    }

}
