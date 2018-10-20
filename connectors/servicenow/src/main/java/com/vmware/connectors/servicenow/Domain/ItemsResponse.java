package com.vmware.connectors.servicenow.Domain;

import com.fasterxml.jackson.databind.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.LinkedList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class ItemsResponse {

    public ItemsResponse(JsonNode jsonSource) {
        this.result = new LinkedList<>();
        if (jsonSource.isArray()) {
            jsonSource.elements().forEachRemaining(s ->
                    this.result.add(new ItemDetailsResponse(s))
            );
        }
    }

    public ItemsResponse() {
        this.result = new LinkedList<>();
    }

    private List<ItemDetailsResponse>  result;

    @JsonProperty("result")
    public List<ItemDetailsResponse> getResult() {
        return result;
    }

    public void setResult(List<ItemDetailsResponse> result) {
        this.result= result;
    }
}
