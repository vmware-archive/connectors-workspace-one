package com.vmware.connectors.servicenow.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)

@SuppressWarnings("PMD.LinguisticNaming")
public class TaskDetailsResponse {
    
    private final static String taskNumberField = "number";
    private final static String createdOnField = "sys_created_on";
    private final static String createdByField = "sys_created_by";
    private final static String shortDescriptionField = "short_description";

    private String number;

    private String createdOn;

    private String createdBy;

    private String shortDescription;

    public TaskDetailsResponse(JsonNode jsonSource) {

        JsonNode jsonObject;
        if(jsonSource.isArray()) {
            jsonObject = jsonSource.elements().next();
        } else {
            jsonObject = jsonSource;
        }

        if(jsonObject.has(TaskDetailsResponse.taskNumberField)) {
            this.number = jsonObject.get(TaskDetailsResponse.taskNumberField).asText();
        }

        if(jsonObject.has(TaskDetailsResponse.createdOnField)) {
            this.createdOn = jsonObject.get(TaskDetailsResponse.createdOnField).asText();
        }

        if(jsonObject.has(TaskDetailsResponse.createdByField)) {
             this.createdBy = jsonObject.get(TaskDetailsResponse.createdByField).asText();
        }

        if(jsonObject.has(TaskDetailsResponse.shortDescriptionField)) {
            this.shortDescription = jsonObject.get(TaskDetailsResponse.shortDescriptionField).asText();
        }
    }

    @JsonProperty(taskNumberField)
    public String getNumber() { return number; }

    public void getNumber(String number) { this.number = number; }

    @JsonProperty(createdOnField)
    public String getCreatedOn() { return createdOn; }

    public void setCreatedOn(String createdOn) { this.createdOn = createdOn; }

    @JsonProperty(createdByField)
    public String getCreatedBy() { return createdBy; }

    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    @JsonProperty(shortDescriptionField)
    public String getShortDescription() { return shortDescription; }

    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
}