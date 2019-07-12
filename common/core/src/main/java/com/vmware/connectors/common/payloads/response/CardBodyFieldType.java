/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.payloads.response;

public enum CardBodyFieldType {

    /**
     * Display information as title-description pairs.
     */
    GENERAL,

    /**
     * Display information as a free form comment box.
     */
    COMMENT,

    /**
     * Display information in a separate section in the notification hub.
     */
    SECTION,

    /**
     * Contains URL for an expense item report available for download.
     */
    ATTACHMENT_URL,

    /**
     * Display information as attachments.
     */
    ATTACHMENT,

    /**
     * Display information for as a trip/flight.
     */
    TRIPINFO

}
