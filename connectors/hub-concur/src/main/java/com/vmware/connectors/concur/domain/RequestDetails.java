package com.vmware.connectors.concur.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RequestDetails {

    @JsonProperty("expense_items")
    private List<Map<String, String>> expenseItems;

    public RequestDetails() {
        expenseItems = new ArrayList<>();
    }

    public List<Map<String, String>> getExpenseItems() {
        return expenseItems;
    }

    public void setExpenseItems(List<Map<String, String>> expenseItems) {
        this.expenseItems = expenseItems;
    }

    public void addExpenseItem(Map<String, String> item) {
        this.expenseItems.add(item);
    }

}
