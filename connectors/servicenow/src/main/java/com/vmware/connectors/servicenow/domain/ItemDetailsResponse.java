package com.vmware.connectors.servicenow.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.LinguisticNaming")
public class ItemDetailsResponse {

    private final static String shortDescriptionField = "short_description";
    private final static String descriptionField = "description";
    private final static String pictureField = "picture";
    private final static String sysIdField = "sys_id";
    private final static String localizedPriceField = "localized_price";
    private final static String nameField = "name";

    private String shortDescription;

    private String description;

    private String picture;

    private String sysId;

    private String localizedPrice;

    private String name;

    public ItemDetailsResponse(JsonNode jsonSource, String baseUrl) {
        if(jsonSource.has(ItemDetailsResponse.shortDescriptionField)) {
            this.shortDescription = jsonSource.get(ItemDetailsResponse.shortDescriptionField).asText();
        }

        if(jsonSource.has(ItemDetailsResponse.descriptionField)) {
            this.description = jsonSource.get(ItemDetailsResponse.descriptionField).asText();
        }

        if(jsonSource.has(ItemDetailsResponse.pictureField) && jsonSource.get(ItemDetailsResponse.pictureField).asText().length() > 0) {
            this.picture = baseUrl + "/" + jsonSource.get(ItemDetailsResponse.pictureField).asText();
            this.picture = jsonSource.get(ItemDetailsResponse.pictureField).asText();
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

    @JsonProperty(shortDescriptionField)
    public String getShortDescription() { return shortDescription;}

    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }

    @JsonProperty(descriptionField)
    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    @JsonProperty(pictureField)
    public String getPicture() { return picture; }

    public void setPicture(String picture) { this.picture = picture; }

    @JsonProperty(sysIdField)
    public String getSysId() { return sysId; }

    public void setSysId(String sysId) { this.sysId = sysId; }

    @JsonProperty(localizedPriceField)
    public String getLocalizedPrice() {return localizedPrice;}

    public void setLocalizedPrice(String localizedPrice) {this.localizedPrice = localizedPrice;}

    @JsonProperty(nameField)
    public String getName() { return name; }

    public void setName(String name) { this.name = name; }
}
