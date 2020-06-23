/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msPlanner.utils

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Utility class for URL Encoding/Decoding
 */
object URLDecoding {
    /**
     * returns the decoded URL.
     *
     * @param value encoded String
     * @returns the decoded String
     */
    fun decodeValue(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
    }

}