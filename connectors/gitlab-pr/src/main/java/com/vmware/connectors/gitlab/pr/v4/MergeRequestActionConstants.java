/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.gitlab.pr.v4;

public final class MergeRequestActionConstants {

    private MergeRequestActionConstants() {
        // constants class
    }

    public final class Properties {

        /**
         * The commit id of the Merge Request to approve or merge.  This
         * prevents the approving or merging of a Merge Request based on
         * stale data.
         */
        public static final String SHA = "sha";

        /**
         * The state_event used to modify the state of a Merge Request.
         */
        public static final String STATE_EVENT = "state_event";

        /**
         * The body of a Notes (comment).
         *
         * https://docs.gitlab.com/ee/api/notes.html#create-new-merge-request-note
         */
        public static final String BODY = "body";

        private Properties() {
            // constants class
        }

    }

    public enum StateEvent {

        /**
         * Close the Merge Request.
         */
        close,

        /**
         * Reopen the Merge Request.
         */
        reopen

    }

}
