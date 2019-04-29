package com.vmware.connectors.servicenow.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.vmware.connectors.common.json.JsonDocument;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.LinguisticNaming")
public class AddToCartResponse {

    private final static String cartIdField = "cart_id";
    private final static String cartTotalField = "cart_total";
    private final static String subtotalField = "subtotal";
    private final static String itemsField = "items";
    private final static String itemIdField = "catalog_item_id";
    private final static String itemQuantityField = "quantity";

    private String cartId;
    private String cartTotal;
    private Map<String, String> items;

    private static final Logger logger = LoggerFactory.getLogger(AddToCartResponse.class);

    public AddToCartResponse(JsonDocument jsonSource) {
        this.items = new LinkedHashMap<String, String>();

        Map<String, Object> root = jsonSource.read("$.result");

        if (root.containsKey(AddToCartResponse.subtotalField)) {
            this.cartTotal = root.get(AddToCartResponse.subtotalField).toString();
        }

        if (root.containsKey(AddToCartResponse.cartIdField)) {
            this.cartId = root.get(AddToCartResponse.cartIdField).toString();
        }
         
        /*
        Items map are iterated through the nested JSON Object.
        */
        try{
            JsonNode itemsNode = new ObjectMapper().readTree(jsonSource.read("$.result.items").toString());

            if (itemsNode.isArray()) {
                itemsNode.elements().forEachRemaining(item -> {
                    if (item.has(AddToCartResponse.itemIdField) && item.has(AddToCartResponse.itemQuantityField)) {
                        Pair<String, String> itemDetails = this.getItemDetails(item);
                        this.updateItemsMap(itemDetails.getLeft(), itemDetails.getRight());
                    }}
                );
            }
        } catch (IOException exe) {
            logger.error("AddToCartResponse -> AddToCartResponse() -> readTree() -> IOException {}", exe.getMessage());
        }
    }

    public AddToCartResponse() {
        this.items = new LinkedHashMap<String, String>();
    }

    @JsonProperty(AddToCartResponse.cartIdField)
    public String getCartId() {
        return this.cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    @JsonProperty(AddToCartResponse.cartTotalField)
    public String getCartTotal() {
        return this.cartTotal;
    }

    public void setCartTotal(String cartTotal) {
       this.cartTotal = cartTotal;
    }

    @JsonProperty(AddToCartResponse.itemsField)
    public Map<String, String> getItems() {
        return this.items;
    }

    public void setItems(Map<String, String> items) {
        this.items = items;
    }

    private Pair<String, String> getItemDetails(JsonNode item) {
        String itemId = item.get(AddToCartResponse.itemIdField).asText();
        String itemQ  = item.get(AddToCartResponse.itemQuantityField).asText();

        return new ImmutablePair<String, String>(itemId, itemQ);
    }

    private void updateItemsMap(String itemId, String itemQuantity) {
        if (this.items.containsKey(itemId)) {
            int itemQ = Integer.parseInt(this.items.get(itemId));
            itemQ = itemQ + Integer.parseInt(itemQuantity);

            this.items.put(itemId, String.valueOf(itemQ));
        } else {
            this.items.put(itemId, itemQuantity);
        }
    }
}
