package com.vmware.connectors.servicenow.Domain;

import com.vmware.connectors.servicenow.ServiceNowController;
import com.fasterxml.jackson.databind.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class TaskDetailsResponse {
    
    private final static String taskNumberField = "number";
    private final static String createdOnField = "sys_created_on";
    private final static String createdByField = "sys_created_by";
    private final static String shortDescriptionField = "short_description";
    
    public TaskDetailsResponse(JsonNode jsonSource) {
        if(jsonSource.has(TaskDetailsResponse.taskNumberField)) {
            this.taskNumber = jsonSource.get(TaskDetailsResponse.taskNumberField).asText();
        }

        if(jsonSource.has(TaskDetailsResponse.createdOnField)) {
            this.createdOn = jsonSource.get(TaskDetailsResponse.createdOnField).asText();
        }

        if(jsonSource.has(TaskDetailsResponse.createdByField)) {
             this.createdBy = jsonSource.get(TaskDetailsResponse.createdByField).asText();
        }

        if(jsonSource.has(TaskDetailsResponse.shortDescriptionField)) {
            this.shortDescription = jsonSource.get(TaskDetailsResponse.shortDescriptionField).asText();
        }
    }

    private String taskNumber;

    private String createdOn;

    private String createdBy;

    private String shortDescription;

    @JsonProperty(taskNumberField)
    public String getTaskNumber() { return taskNumber; }

    public void setTaskNumber(String taskNumber) { this.taskNumber = taskNumber; }

    @JsonProperty(createdOnField)
    public String getCreatedOn() { return createdOn; }

    public void setCreatedOn(String createdOn) { this.createdOn = createdOn; }

    @JsonProperty(createdByField)
    public String getCreatedBy() { return createdBy; }

    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    @JsonProperty(shortDescriptionField)
    public String getShortDescriptionField() { return shortDescription; }

    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
}