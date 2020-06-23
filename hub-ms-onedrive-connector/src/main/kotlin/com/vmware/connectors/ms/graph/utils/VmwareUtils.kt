/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.ms.graph.utils

/**
 * Utility Class for Vmware Backend
 */
object VmwareUtils {

    /**
     * gets the emaiId from the Vmware user token
     *
     * @param token retrieved from BackEnd
     * @return the emailId
     */
    fun getUserEmailFromToken(token: String): String? {
        val pl = token
                .split(" ")
                .lastOrNull()
                ?.split(".")
                ?.get(1)
                ?.toBase64DecodedString()

        return pl?.let { JsonParser.deserialize(it) }
                ?.getStringOrNull("eml")
    }
}