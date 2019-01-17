package com.vmware.connectors.concur.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonPropertyOrder({ "uuid", "requestDate", "submittedDate", "status", "statusDescription", "requestSummary",
        "requestDetails", "receipts", "action" })
public class Data {

    private UUID uuid;

    @JsonProperty("request_date")
    private String requestDate;

    @JsonProperty("submitted_date")
    private String submittedDate;

    @JsonProperty("status")
    private String status;

    @JsonProperty("status_description")
    private String statusDescription;

    @JsonProperty("request_summary")
    private Map<String, String> requestSummary;

    @JsonProperty("request_details")
    private RequestDetails requestDetails;

    private List<Receipt> receipts;

    private List<Action> action;

    public Data() {
        this.uuid = UUID.randomUUID();
        this.requestSummary = new LinkedHashMap<>();
        this.receipts = new ArrayList<>();
        this.action = new ArrayList<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    public String getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(String submittedDate) {
        this.submittedDate = submittedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public Map<String, String> getRequestSummary() {
        return requestSummary;
    }

    public void setRequestSummary(Map<String, String> requestSummary) {
        this.requestSummary = requestSummary;
    }

    public RequestDetails getRequestDetails() {
        return requestDetails;
    }

    public void setRequestDetails(RequestDetails requestDetails) {
        this.requestDetails = requestDetails;
    }

    public List<Receipt> getReceipts() {
        return receipts;
    }

    public void setReceipts(List<Receipt> receipts) {
        this.receipts = receipts;
    }

    public List<Action> getAction() {
        return action;
    }

    public void setAction(List<Action> action) {
        this.action = action;
    }

    public void addReceipt(Receipt receipt) {
        this.receipts.add(receipt);
    }

    public void addAction(Action action) {
        this.action.add(action);
    }

    public void addRequestSummary(String key, String value) {
        this.requestSummary.put(key, value);
    }

}
