package com.vmware.connectors.servicenow;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

@AutoProperty
class Request {

    private final String number;
    private final String totalPrice;

    Request(
            String number,
            String totalPrice
    ) {
        this.number = number;
        this.totalPrice = totalPrice;
    }

    String getNumber() {
        return number;
    }

    String getTotalPrice() {
        return totalPrice;
    }

    @Override
    public String toString() {
        return Pojomatic.toString(this);
    }

}
