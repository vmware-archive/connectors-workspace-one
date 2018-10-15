package com.vmware.connectors.concur.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PendingApprovalsVO implements Serializable {

    private static final long serialVersionUID = 1334545279846629652L;

    private String name;
    private String total;
    private String currencyCode;
    private String country;
    private String createDate;
    private String submitDate;
    private String ownerName;
    private String approverLoginID;
    private String approverName;
    private String approvalStatusName;
    private String id;

    @JsonProperty("Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("Total")
    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    @JsonProperty("CurrencyCode")
    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @JsonProperty("Country")
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @JsonProperty("CreateDate")
    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    @JsonProperty("SubmitDate")
    public String getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(String submitDate) {
        this.submitDate = submitDate;
    }

    @JsonProperty("OwnerName")
    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    @JsonProperty("ApproverLoginID")
    public String getApproverLoginID() {
        return approverLoginID;
    }

    public void setApproverLoginID(String approverLoginID) {
        this.approverLoginID = approverLoginID;
    }

    @JsonProperty("ApproverName")
    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    @JsonProperty("ApprovalStatusName")
    public String getApprovalStatusName() {
        return approvalStatusName;
    }

    public void setApprovalStatusName(String approvalStatusName) {
        this.approvalStatusName = approvalStatusName;
    }

    @JsonProperty("ID")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
