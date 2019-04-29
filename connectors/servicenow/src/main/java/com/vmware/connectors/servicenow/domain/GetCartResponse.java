package com.vmware.connectors.servicenow.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.vmware.connectors.common.json.JsonDocument;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.LinguisticNaming")
public class GetCartResponse {

    private final static String cartIdField = "cart_id";
    private final static String cartTotalField = "cart_total";
    private final static String subtotalField = "subtotal_price";
    
    private String cartId;
    private String cartTotal;
    
    public GetCartResponse(JsonDocument jsonSource) {

        Map<String, Object> root = jsonSource.read("$.result");

        if (root.containsKey(GetCartResponse.subtotalField)) {
            this.cartTotal = root.get(GetCartResponse.subtotalField).toString();
        }

        if (root.containsKey(GetCartResponse.cartIdField)) {
            this.cartId = root.get(GetCartResponse.cartIdField).toString();
        }
    }

    @JsonProperty(GetCartResponse.cartIdField)
    public String getCartId() {
        return this.cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    @JsonProperty(GetCartResponse.cartTotalField)
    public String getCartTotal() {
        return this.cartTotal;
    }

    public void setCartTotal(String cartTotal) {
       this.cartTotal = cartTotal;
    }
}
