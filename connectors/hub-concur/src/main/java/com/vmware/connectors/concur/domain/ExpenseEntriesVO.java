/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class ExpenseEntriesVO {

    private String reportEntryID;
    private String expenseTypeID;
    private String expenseTypeName;
    private String spendCategory;
    private String paymentTypeCode;
    private String paymentTypeName;
    private String transactionDate;
    private String transactionCurrencyName;
    private String exchangeRate;
    private String transactionAmount;
    private String postedAmount;
    private String approvedAmount;
    private String businessPurpose;
    private String vendorDescription;
    private String locationName;
    private String locationSubdivision;
    private String locationCountry;
    private String isItemized;

    @JsonProperty("ReportEntryID")
    public String getReportEntryID() {
        return reportEntryID;
    }

    public void setReportEntryID(String reportEntryID) {
        this.reportEntryID = reportEntryID;
    }

    @JsonProperty("ExpenseTypeID")
    public String getExpenseTypeID() {
        return expenseTypeID;
    }

    public void setExpenseTypeID(String expenseTypeID) {
        this.expenseTypeID = expenseTypeID;
    }

    @JsonProperty("ExpenseTypeName")
    public String getExpenseTypeName() {
        return expenseTypeName;
    }

    public void setExpenseTypeName(String expenseTypeName) {
        this.expenseTypeName = expenseTypeName;
    }

    @JsonProperty("SpendCategory")
    public String getSpendCategory() {
        return spendCategory;
    }

    public void setSpendCategory(String spendCategory) {
        this.spendCategory = spendCategory;
    }

    @JsonProperty("PaymentTypeCode")
    public String getPaymentTypeCode() {
        return paymentTypeCode;
    }

    public void setPaymentTypeCode(String paymentTypeCode) {
        this.paymentTypeCode = paymentTypeCode;
    }

    @JsonProperty("PaymentTypeName")
    public String getPaymentTypeName() {
        return paymentTypeName;
    }

    public void setPaymentTypeName(String paymentTypeName) {
        this.paymentTypeName = paymentTypeName;
    }

    @JsonProperty("TransactionDate")
    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    @JsonProperty("TransactionCurrencyName")
    public String getTransactionCurrencyName() {
        return transactionCurrencyName;
    }

    public void setTransactionCurrencyName(String transactionCurrencyName) {
        this.transactionCurrencyName = transactionCurrencyName;
    }

    @JsonProperty("ExchangeRate")
    public String getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(String exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    @JsonProperty("TransactionAmount")
    public String getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(String transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    @JsonProperty("PostedAmount")
    public String getPostedAmount() {
        return postedAmount;
    }

    public void setPostedAmount(String postedAmount) {
        this.postedAmount = postedAmount;
    }

    @JsonProperty("ApprovedAmount")
    public String getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(String approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    @JsonProperty("BusinessPurpose")
    public String getBusinessPurpose() {
        return businessPurpose;
    }

    public void setBusinessPurpose(String businessPurpose) {
        this.businessPurpose = businessPurpose;
    }

    @JsonProperty("VendorDescription")
    public String getVendorDescription() {
        return vendorDescription;
    }

    public void setVendorDescription(String vendorDescription) {
        this.vendorDescription = vendorDescription;
    }

    @JsonProperty("LocationName")
    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    @JsonProperty("LocationSubdivision")
    public String getLocationSubdivision() {
        return locationSubdivision;
    }

    public void setLocationSubdivision(String locationSubdivision) {
        this.locationSubdivision = locationSubdivision;
    }

    @JsonProperty("LocationCountry")
    public String getLocationCountry() {
        return locationCountry;
    }

    public void setLocationCountry(String locationCountry) {
        this.locationCountry = locationCountry;
    }

    @JsonProperty("IsItemized")
    public String getIsItemized() {
        return isItemized;
    }

    public void setIsItemized(String isItemized) {
        this.isItemized = isItemized;
    }

}
