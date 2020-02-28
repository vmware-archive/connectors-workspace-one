package com.vmware.connectors.msTeams.utils

import java.util.*
/**
 * Utility class for Base64 decoding
 */
object Base64Utils {
    private val decoder = Base64.getDecoder()

    /**
     * this function will return the decoded ByteArray
     *
     * @param s is the encoded String
     * @return ByteArray
     */
    fun decode(s: String): ByteArray = decoder.decode(s)
}