/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

public enum CardActionKey {

    /**
     * Complete action directly without user input.
     */
    DIRECT,

    /**
     * Take input from the user in a form.
     */
    USER_INPUT,

    /**
     * Open in a browser,
     */
    OPEN_IN,

    /**
     * User should install our app.
     */
    INSTALL_APP

}
