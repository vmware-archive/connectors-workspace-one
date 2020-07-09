/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.boxNotes.config

const val CONNECTOR_AUTH_MESSAGE_HEADER = "X-Connector-Authorization"
const val ROUTING_PREFIX = "x-routing-prefix"
const val AUTHORIZATION = "Authorization"
const val CONNECTOR_BASE_URL_HEADER = "X-Connector-Base-Url"
val MAIL_VALIDATION_REGEX = "^([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})\$".toRegex()
const val DATE_FORMAT_PATTERN_RETURN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
const val DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss-SS:00"
const val ZONE_ID = "America/Los_Angeles"