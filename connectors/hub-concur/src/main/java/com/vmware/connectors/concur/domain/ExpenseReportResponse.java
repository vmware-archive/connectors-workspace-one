package com.vmware.connectors.concur.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class ExpenseReportResponse {

    private String userLoginID;
    private String employeeName;
    private String reportID;
    private String reportKey;
    private String reportName;
    private String purpose;
    private String reportDate;
    private String creationDate;
    private String submitDate;
    private String paidDate;
    private String currencyCode;
    private String costCenter;
    private CustomDetailsVO custom1;
    private String reportImageURL;
    private String hasException;
    private String workflowActionURL;
    private String reportTotal;
    private List<ExpenseEntriesVO> expenseEntriesList;

    @JsonProperty("UserLoginID")
    public String getUserLoginID() {
        return userLoginID;
    }

    public void setUserLoginID(String userLoginID) {
        this.userLoginID = userLoginID;
    }

    @JsonProperty("EmployeeName")
    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    @JsonProperty("ReportID")
    public String getReportID() {
        return reportID;
    }

    public void setReportID(String reportID) {
        this.reportID = reportID;
    }

    @JsonProperty("ReportKey")
    public String getReportKey() {
        return reportKey;
    }

    public void setReportKey(String reportKey) {
        this.reportKey = reportKey;
    }

    @JsonProperty("ReportName")
    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    @JsonProperty("Purpose")
    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    @JsonProperty("ReportDate")
    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    @JsonProperty("CreationDate")
    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    @JsonProperty("SubmitDate")
    public String getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(String submitDate) {
        this.submitDate = submitDate;
    }

    @JsonProperty("PaidDate")
    public String getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(String paidDate) {
        this.paidDate = paidDate;
    }

    @JsonProperty("CurrencyCode")
    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @JsonProperty("Custom1")
    public CustomDetailsVO getCustom1() {
        return custom1;
    }

    public void setCustom1(CustomDetailsVO custom1) {
        this.custom1 = custom1;
    }

    @JsonProperty("ReportImageURL")
    public String getReportImageURL() {
        return reportImageURL;
    }

    public void setReportImageURL(String reportImageURL) {
        this.reportImageURL = reportImageURL;
    }

    @JsonProperty("HasException")
    public String getHasException() {
        return hasException;
    }

    public void setHasException(String hasException) {
        this.hasException = hasException;
    }

    @JsonProperty("WorkflowActionURL")
    public String getWorkflowActionURL() {
        return workflowActionURL;
    }

    public void setWorkflowActionURL(String workflowActionURL) {
        this.workflowActionURL = workflowActionURL;
    }

    @JsonProperty("ExpenseEntriesList")
    public List<ExpenseEntriesVO> getExpenseEntriesList() {
        return expenseEntriesList;
    }

    public void setExpenseEntriesList(List<ExpenseEntriesVO> expenseEntriesList) {
        this.expenseEntriesList = expenseEntriesList;
    }

    @JsonProperty("OrgUnit5")
    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    @JsonProperty("ReportTotal")
    public String getReportTotal() {
        return reportTotal;
    }

    public void setReportTotal(String reportTotal) {
        this.reportTotal = reportTotal;
    }

}
