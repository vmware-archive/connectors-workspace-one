package com.vmware.connectors.servicenow.domain;

import com.fasterxml.jackson.databind.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.LinkedList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class ItemsResponse {

    private List<ItemDetailsResponse>  objects;

    public ItemsResponse() {
        this.objects = new LinkedList<>();
    }

    public ItemsResponse(JsonNode jsonSource, String baseUrl) {
        this.objects = new LinkedList<>();
        if (jsonSource.isArray()) {
            jsonSource.elements().forEachRemaining(s ->
                    this.objects.add(new ItemDetailsResponse(s, baseUrl))
            );
        }
    }

    @JsonProperty("objects")
    public List<ItemDetailsResponse> getObjects() {
        return this.objects;
    }

    public void setObjects(List<ItemDetailsResponse> result) {
        this.objects= result;
    }
}
