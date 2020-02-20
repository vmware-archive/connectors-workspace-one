/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.hub.servicenow;

/**
 * A class to hold API information (fields, states, etc.) for ServiceNow's sc_req_item table.
 */
@SuppressWarnings("PMD.ClassNamingConventions")
final class ScRequestedItem {

    enum Fields {

        /**
         * The system id for an item in the sc_req_item table.
         *
         * Example: aeed229047801200e0ef563dbb9a71c2
         */
        SYS_ID("sys_id"),

        /**
         * The request associated with the item being requested.
         *
         * Example: { "link": "https://dev15329.service-now.com/api/now/table/sc_request/6eed229047801200e0ef563dbb9a71c2", "value": "6eed229047801200e0ef563dbb9a71c2"}
         */
        REQUEST("request"),

        /**
         * The description of the item being requested.
         *
         * Example: Apple iPad 3
         */
        SHORT_DESCRIPTION("short_description"),

        /**
         * The price of the item being requested.
         *
         * Example: 600.00
         */
        PRICE("price"),

        /**
         * The quantity of item being requested.
         *
         * Example: 5
         */
        QUANTITY("quantity");

        private final String snowField;

        Fields(String snowField) {
            this.snowField = snowField;
        }

        @Override
        public String toString() {
            return this.snowField;
        }

    }

    /**
     * The name of the requested item table in ServiceNow.
     */
    static final String TABLE_NAME = "sc_req_item";

    private ScRequestedItem() {
        // empty: utility class
    }

}
