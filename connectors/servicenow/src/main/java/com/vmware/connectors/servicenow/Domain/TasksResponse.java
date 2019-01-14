package com.vmware.connectors.servicenow.Domain;

import com.fasterxml.jackson.databind.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.LinkedList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class TasksResponse {
    
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

    private List<TaskDetailsResponse> objects;

    @JsonProperty("objects")
    public List<TaskDetailsResponse> getObjects() {
        return this.objects;
    }

    public void setObjects(List<TaskDetailsResponse> result) {
        this.objects = result;
    }
}