/*
 * Copyright © 2019 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.coupa.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequisitionLineDetails {

    @JsonProperty("description")
    private String description;

    @JsonProperty("total")
    private String total;

    @JsonProperty("quantity")
    private String quantity;

    @JsonProperty("sap-group-material-id")
    private String sapMaterialGroupId;

    @JsonProperty("commodity")
    private Commodity commodity;

    @JsonProperty("need-by-date")
    private String needByDate;

    @JsonProperty("supplier")
    private Supplier supplier;

    @JsonProperty("payment-term")
    private PaymentTerm paymentTerm;

    @JsonProperty("shipping-term")
    private ShippingTerm shippingTerm;

    @JsonProperty("account")
    private Account account;

    @JsonProperty("unit-price")
    private String unitPrice;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getSapMaterialGroupId() {
        return sapMaterialGroupId;
    }

    public void setSapMaterialGroupId(String sapMaterialGroupId) {
        this.sapMaterialGroupId = sapMaterialGroupId;
    }

    public Commodity getCommodity() {
        return commodity;
    }

    public void setCommodity(Commodity commodity) {
        this.commodity = commodity;
    }

    public String getNeedByDate() {
        return needByDate;
    }

    public void setNeedByDate(String needByDate) {
        this.needByDate = needByDate;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public PaymentTerm getPaymentTerm() {
        return paymentTerm;
    }

    public void setPaymentTerm(PaymentTerm paymentTerm) {
        this.paymentTerm = paymentTerm;
    }

    public ShippingTerm getShippingTerm() {
        return shippingTerm;
    }

    public void setShippingTerm(ShippingTerm shippingTerm) {
        this.shippingTerm = shippingTerm;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(String unitPrice) {
        this.unitPrice = unitPrice;
    }
}
