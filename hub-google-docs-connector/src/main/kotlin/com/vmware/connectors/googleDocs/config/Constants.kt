/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.googleDocs.config

const val CONNECTOR_AUTH_MESSAGE_HEADER = "X-Connector-Authorization"
const val ROUTING_PREFIX = "x-routing-prefix"
const val AUTHORIZATION = "Authorization"
const val CONNECTOR_BASE_URL_HEADER = "X-Connector-Base-Url"
const val DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
val mailValidateRegex = "^([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})\$".toRegex()
const val DOCUMENT_LOOKUP_WINDOW = 62
const val IGNORE_DOC_STATUS = "resolved"