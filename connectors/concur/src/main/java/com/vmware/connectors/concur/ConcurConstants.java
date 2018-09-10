/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.concur;

/**
 * Constants for Concur connector;
 */
@SuppressWarnings("PMD.ClassNamingConventions")
public final class ConcurConstants {

    private ConcurConstants() {
    }

    public final static class Fields {
        // Unique ticket request identifier for retrieving travel information.
        public static final String EXPENSE_REPORT_ID = "expense_report_id";

        // Concur auto generated email subject.
        public static final String CONCUR_AUTOMATED_EMAIL_SUBJECT = "concur_automated_email_subject";

        private Fields() {
        }
    }

    public final static class PathVariable {
        // Concur request identifier. Usage: /travelrequest/requests/{id}
        public static final String EXPENSE_REPORT_ID = "expenseReportId";

        private PathVariable() {
        }
    }

    public final static class RequestParam {
        /*
           Login ID of the user.
           The user must have the Web Services Admin (Professional) or
           Can Administer (Standard) user role to use this parameter.
        */
        public final static String USER = "user";

        // User input to approve or reject a expense.
        public final static String REASON = "reason";

        private RequestParam() {
        }
    }

    public static final class Header {
        public final static String AUTHORIZATION_HEADER = "X-Connector-Authorization";
        public final static String BACKEND_BASE_URL_HEADER = "X-Connector-Base-Url";
        public final static String ROUTING_PREFIX = "x-routing-prefix";

        private Header() {
        }
    }

    public static final class ConcurRequestActions {
        public final static String APPROVE = "Approve";
        public final static String REJECT = "Send Back to Employee";

        public final static String ACTION_PLACEHOLDER = "${action}";
        public final static String COMMENT_PLACEHOLDER = "${comment}";

        private ConcurRequestActions() {
        }
    }

    public static final class ConcurResponseActions {
        public final static String APPROVED = "Approved";

        public final static String SUBMITTED_AND_PENDING_APPROVAL = "Submitted & Pending Approval";

        public final static String SEND_BACK_TO_EMPLOYEE = "Sent Back to Employee";

        public final static String APPROVED_AND_IN_ACCOUNT_REVIEW = "Approved & In Accounting Review";
    }
}
