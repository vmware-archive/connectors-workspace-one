/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow;

/**
 * A class to hold API information (fields, states, etc.) for ServiceNow's sys_user table.
 */
final class SysUser {

    enum Fields {

        /**
         * The system id for an item in the sys_user table.
         *
         * Example: 71826bf03710200044e0bfc8bcbe5d3b
         */
        SYS_ID("sys_id"),

        /**
         * The user's login name.
         *
         * Example: aileen.mottern
         */
        USER_NAME("user_name"),

        /**
         * The user's email address.
         *
         * Example: aileen.mottern@example.com
         */
        EMAIL("email");

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
     * The name of the user table in ServiceNow.
     */
    static final String TABLE_NAME = "sys_user";

    private SysUser() {
        // empty: utility class
    }

}
