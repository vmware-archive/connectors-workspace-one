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
public class CheckoutResponse {

    private final static String cartIdField = "order_id";
    private final static String subtotalField = "subtotal";

    private String cartId;
    private String cartTotal;

    private static final Logger logger = LoggerFactory.getLogger(CheckoutResponse.class);

    public CheckoutResponse(JsonDocument jsonSource) {

        Map<String, Object> root = jsonSource.read("$.result");

        if (root.containsKey("request_number")) {
            this.setCartId(root.get("request_number").toString());
        }
    }

    public CheckoutResponse() {
    }

    @JsonProperty(CheckoutResponse.cartIdField)
    public String getCartId() {
        return this.cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    @JsonProperty(CheckoutResponse.subtotalField)
    public String getCartTotal() {
        return this.cartTotal;
    }

    public void setCartTotal(String cartTotal) {
       this.cartTotal = cartTotal;
    }
}
