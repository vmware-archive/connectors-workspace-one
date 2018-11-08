package com.vmware.connectors.servicenow.domain;

import com.vmware.connectors.common.json.JsonDocument;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.LinguisticNaming")
public class CheckoutResponse {

    private List<CheckoutResponseDetails> objects;

    public CheckoutResponse(CheckoutResponseDetails details) {

        this.objects = new LinkedList<CheckoutResponseDetails>();
        this.objects.add(details);
    }

    public CheckoutResponse(JsonDocument jsonSource) {
        CheckoutResponseDetails details = new CheckoutResponseDetails(jsonSource);

        this.objects = new LinkedList<CheckoutResponseDetails>();
        this.objects.add(details);
    }

    @JsonProperty("objects")
    public List<CheckoutResponseDetails> getObjects() {
        return this.objects;
    }

    public void setObjects(List<CheckoutResponseDetails> result) {
        this.objects= result;
    }

    public void setCartTotal(String cartTotal) {
        for (CheckoutResponseDetails object : this.objects) {
            object.setCartTotal(cartTotal);
        }
     }

    class CheckoutResponseDetails {

        private final static String cartIdField = "order_id";
        private final static String subtotalField = "subtotal";

        private String cartId;
        private String cartTotal;
        
        public CheckoutResponseDetails(JsonDocument jsonSource) {
            Map<String, Object> root = jsonSource.read("$.result");

            if (root.containsKey("request_number")) {
                this.cartId = root.get("request_number").toString();
            }

            this.cartTotal = "";
        }
        
        @JsonProperty(CheckoutResponseDetails.cartIdField)
        public String getCartId() {
            return this.cartId;
        }
    
        public void setCartId(String cartId) {
            this.cartId = cartId;
        }
    
        //TODO: this should be returned, but we would have to maintain state or make an extra call to get the total since it's not returned by the SN checkout api.
        @JsonProperty(CheckoutResponseDetails.subtotalField)
        public String getCartTotal() {
            return this.cartTotal;
        }
    
        public void setCartTotal(String cartTotal) {
           this.cartTotal = cartTotal;
        }
    }
}
