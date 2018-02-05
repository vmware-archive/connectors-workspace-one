/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.servicenow;

/**
 * A class to hold API information (fields, states, etc.) for ServiceNow's sysapproval_approver table.
 */
final class SysApprovalApprover {

    enum Fields {

        /**
         * The system id for an item in the sysapproval_approver table.
         *
         * Example: 070e9a1b4fb903002ba13879b110c7e3
         */
        SYS_ID("sys_id"),

        /**
         * The requested approver.
         *
         * Example: { "link": "https://dev15329.service-now.com/api/now/table/sys_user/71826bf03710200044e0bfc8bcbe5d3b", "value": "070e9a1b4fb903002ba13879b110c7e3"}
         */
        APPROVER("approver"),

        /**
         * The source table of the ticket/incident that created the approval request.
         *
         * Example: sc_request
         */
        SOURCE_TABLE("source_table"),

        /**
         * The task id to be approved.  This is the same sys_id as the sys_id
         * of the request in the sc_request table.
         *
         * Example: { "link": "https://dev15329.service-now.com/api/now/table/task/5fedd61b4fb903002ba13879b110c73f", "value": "5fedd61b4fb903002ba13879b110c73f"}
         */
        SYSAPPROVAL("sysapproval"),

        /**
         * The comments for an approval record.
         *
         * Example: 2017-10-24 09:00:45 - Eric Schroeder (Comment...
         */
        COMMENTS("comments"),

        /**
         * The due date for an approval record.
         *
         * Example: 2017-10-19 15:53:58
         */
        DUE_DATE("due_date"),

        /**
         * The login name of the person requesting approval.
         *
         * Example: abraham.lincoln
         */
        SYS_CREATED_BY("sys_created_by"),

        /**
         * The state of the approval request.
         *
         * Example: requested
         */
        STATE("state");

        private final String snowField;

        Fields(String snowField) {
            this.snowField = snowField;
        }

        @Override
        public String toString() {
            return this.snowField;
        }

    }

    enum States {

        /**
         * The approval request has been submitted, but not approved or rejected yet.
         */
        REQUESTED("requested"),

        /**
         * The approval request has been approved.
         */
        APPROVED("approved"),

        /**
         * The approval request has been rejected.
         */
        REJECTED("rejected");

        private final String snowState;

        States(String snowState) {
            this.snowState = snowState;
        }

        @Override
        public String toString() {
            return this.snowState;
        }

    }

    /**
     * The name of the table for approval requests in ServiceNow.
     */
    static final String TABLE_NAME = "sysapproval_approver";

    private SysApprovalApprover() {
        // empty: utility class
    }

}
