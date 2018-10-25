package com.vmware.connectors.servicenow.Domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.connectors.common.json.JsonDocument;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class CartResponse {

    private final static String cartIdField = "order_id";
    private final static String subtotalField = "subtotal";
    private final static String itemsField = "items";
    private final static String itemIdField = "catalog_item_id";
    private final static String itemQuantityField = "quantity";

    private String cartId;
    private String cartTotal;
    private Map<String, Integer> items;

    private static final Logger logger = LoggerFactory.getLogger(CartResponse.class);

    public CartResponse(JsonDocument jsonSource) {
        this.items = new LinkedHashMap<String, Integer>();

        Map<String, Object> root = jsonSource.read("$.result");

        if (root.containsKey(CartResponse.subtotalField)) {
            this.setCartTotal(root.get(CartResponse.subtotalField).toString());
        }
         
        try{
            JsonNode itemsNode = new ObjectMapper().readTree(jsonSource.read("$.result.items").toString());

            if (itemsNode.isArray()) {
                itemsNode.elements().forEachRemaining(item -> {
                    if (item.has(CartResponse.itemIdField) && item.has(CartResponse.itemQuantityField)) {
                        String itemId = item.get(CartResponse.itemIdField).asText();
                        int itemQ  = Integer.parseInt(item.get(CartResponse.itemQuantityField).asText());

                        if (this.items.containsKey(itemId))
                            itemQ = this.items.get(itemId);
                        this.items.put(itemId, itemQ);
                    }}
                );
            }
        } catch (IOException exe) {
            logger.error("CartResponse -> CartResponse() -> readTree() -> IOException {}", exe.getMessage());
        }
    }

    public CartResponse() {
        this.items = new LinkedHashMap<String, Integer>();
    }

    @JsonProperty(CartResponse.cartIdField)
    public String getCartId() {
        return this.cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    @JsonProperty(CartResponse.subtotalField)
    public String getCartTotal() {
        return this.cartTotal;
    }

    public void setCartTotal(String cartTotal) {
       this.cartTotal = cartTotal;
    }

    @JsonProperty("items")
    public Map<String, Integer> getItems() {
        return this.items;
    }

    public void setItems(Map<String, Integer> items) {
        this.items = items;
    }
}
