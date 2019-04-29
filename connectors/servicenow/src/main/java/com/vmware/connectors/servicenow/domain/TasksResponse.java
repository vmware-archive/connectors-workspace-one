package com.vmware.connectors.servicenow.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class TasksResponse {
    
    private List<TaskDetailsResponse> objects;

    public TasksResponse() {
        this.objects = new LinkedList<>();
    }

    public TasksResponse(JsonNode jsonSource) {
        this.objects = new LinkedList<>();
        if (jsonSource.isArray()) {
            jsonSource.elements().forEachRemaining(s ->
                this.objects.add(new TaskDetailsResponse(s))
            );
        }
    }

    @JsonProperty("objects")
    public List<TaskDetailsResponse> getObjects() {
        return this.objects;
    }

    public void setObjects(List<TaskDetailsResponse> result) {
        this.objects = result;
    }
}