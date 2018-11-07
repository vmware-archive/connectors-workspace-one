package com.vmware.connectors.servicenow.Domain;

import com.fasterxml.jackson.databind.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.LinkedList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class ItemsResponse {

    public ItemsResponse(JsonNode jsonSource, String baseUrl) {
        this.objects = new LinkedList<>();
        if (jsonSource.isArray()) {
            jsonSource.elements().forEachRemaining(s ->
                    this.objects.add(new ItemDetailsResponse(s, baseUrl))
            );
        }
    }

    private List<ItemDetailsResponse>  objects;

    @JsonProperty("objects")
    public List<ItemDetailsResponse> getObjects() {
        return objects;
    }

    public void setObjects(List<ItemDetailsResponse> result) {
        this.objects= result;
    }
}
