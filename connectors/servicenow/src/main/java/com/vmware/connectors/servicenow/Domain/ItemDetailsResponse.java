package com.vmware.connectors.servicenow.Domain;

import com.vmware.connectors.servicenow.ServiceNowController;
import com.fasterxml.jackson.databind.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class ItemDetailsResponse {

    private final static String shortDescriptionField = "short_description";
    private final static String descriptionField = "description";
    private final static String pictureField = "picture";
    private final static String sysIdField = "sys_id";
    private final static String localizedPriceField = "localized_price";
    private final static String nameField = "name";

    public ItemDetailsResponse(JsonNode jsonSource, String baseUrl) {
        if(jsonSource.has(ItemDetailsResponse.shortDescriptionField)) {
            this.shortDescription = jsonSource.get(ItemDetailsResponse.shortDescriptionField).asText();
        }

        if(jsonSource.has(ItemDetailsResponse.descriptionField)) {
            this.description = jsonSource.get(ItemDetailsResponse.descriptionField).asText();
        }

        if(jsonSource.has(ItemDetailsResponse.pictureField)) {
            this.picture = baseUrl + "/" + jsonSource.get(ItemDetailsResponse.pictureField).asText();
        }

        if(jsonSource.has(ItemDetailsResponse.sysIdField)) {
            this.sysId = jsonSource.get(ItemDetailsResponse.sysIdField).asText();
        }

        if(jsonSource.has(ItemDetailsResponse.localizedPriceField)) {
            this.localizedPrice = jsonSource.get(ItemDetailsResponse.localizedPriceField).asText();
        }

        if(jsonSource.has(ItemDetailsResponse.nameField)) {
            this.name = jsonSource.get(ItemDetailsResponse.nameField).asText();
        }
    }

    private String shortDescription;

    private String description;

    private String picture;

    private String sysId;

    private String localizedPrice;

    private String name;

    private static final Logger logger = LoggerFactory.getLogger(ServiceNowController.class);

    @JsonProperty("short_description")
    public String getShortDescription() { return shortDescription;}

    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }


    @JsonProperty("description")
    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }


    @JsonProperty("picture")
    public String getPicture() { return picture; }

    public void setPicture(String picture) { this.picture = picture; }


    @JsonProperty("sys_id") public String getSysId() { return sysId; }

    public void setSysId(String sysId) { this.sysId = sysId; }


    @JsonProperty("localized_price")
    public String getLocalizedPrice() {return localizedPrice;}

    public void setLocalizedPrice(String localizedPrice) {this.localizedPrice = localizedPrice;}


    @JsonProperty("name")
    public String getName() { return name; }

    public void setName(String name) { this.name = name; }
}
