package com.vmware.connectors.servicenow.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.vmware.connectors.common.json.JsonDocument;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.LinguisticNaming")
public class CartResponse {

    private final static String cartIdField = "cart_id";
    private final static String cartTotalField = "cart_total";
    private final static String subtotalField = "subtotal";
    private final static String itemsField = "items";
    private final static String itemIdField = "catalog_item_id";
    private final static String itemQuantityField = "quantity";

    private String cartId;
    private String cartTotal;
    private Map<String, String> items;

    private static final Logger logger = LoggerFactory.getLogger(CartResponse.class);

    public CartResponse(JsonDocument jsonSource) {
        this.items = new LinkedHashMap<String, String>();

        Map<String, Object> root = jsonSource.read("$.result");

        if (root.containsKey(CartResponse.subtotalField)) {
            this.cartTotal = root.get(CartResponse.subtotalField).toString();
        }

        if (root.containsKey(CartResponse.cartIdField)) {
            this.cartId = root.get(CartResponse.cartIdField).toString();
        }
         
        /*
        Items map are iterated through the nested JSON Object.
        */
        try{
            JsonNode itemsNode = new ObjectMapper().readTree(jsonSource.read("$.result.items").toString());

            if (itemsNode.isArray()) {
                itemsNode.elements().forEachRemaining(item -> {
                    if (item.has(CartResponse.itemIdField) && item.has(CartResponse.itemQuantityField)) {

                        Pair<String, String> itemDetails = this.getItemDetails(item);
                        this.items.put(itemDetails.getLeft(), itemDetails.getRight());
                    }}
                );
            }
        } catch (IOException exe) {
            logger.error("CartResponse -> CartResponse() -> readTree() -> IOException {}", exe.getMessage());
        }
    }

    public CartResponse() {
        this.items = new LinkedHashMap<String, String>();
    }

    @JsonProperty(CartResponse.cartIdField)
    public String getCartId() {
        return this.cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    @JsonProperty(CartResponse.cartTotalField)
    public String getCartTotal() {
        return this.cartTotal;
    }

    public void setCartTotal(String cartTotal) {
       this.cartTotal = cartTotal;
    }

    @JsonProperty(CartResponse.itemsField)
    public Map<String, String> getItems() {
        return this.items;
    }

    public void setItems(Map<String, String> items) {
        this.items = items;
    }

    private Pair<String, String> getItemDetails(JsonNode item) {
        String itemId = item.get(CartResponse.itemIdField).asText();
        String itemQ  = item.get(CartResponse.itemQuantityField).asText();

        if (this.items.containsKey(itemId)) {
            itemQ = this.items.get(itemId);
        }

        return new ImmutablePair<String, String>(itemId, itemQ);
    }
}
