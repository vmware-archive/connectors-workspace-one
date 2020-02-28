/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.hub.servicenow;

/**
 * A class to hold API information (fields, states, etc.) for ServiceNow's sc_request table.
 */

@SuppressWarnings("PMD.ClassNamingConventions")
final class ScRequest {

    enum Fields {

        /**
         * The system id for an item in the sc_request table.
         *
         * Example: 6eed229047801200e0ef563dbb9a71c2
         */
        SYS_ID("sys_id"),

        /**
         * The total price of the request.
         *
         * Example: 3349.95
         */
        PRICE("price"),

        /**
         * The request number.
         *
         * Example: REQ0000001
         */
        NUMBER("number");

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
     * The name of the request table in ServiceNow.
     */
    static final String TABLE_NAME = "sc_request";

    private ScRequest() {
        // empty: utility class
    }

}
