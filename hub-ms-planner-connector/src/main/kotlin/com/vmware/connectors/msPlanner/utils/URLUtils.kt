package com.vmware.connectors.msPlanner.utils

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Utility class for URL Encoding/Decoding
 */
object URLDecoding {
    /**
     * this function returns the decoded URL.
     *
     * @param value encoded String
     * @returns the decoded String
     */
    fun decodeValue(value: String): String {
        return try {
            URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
        } catch (ex: UnsupportedEncodingException) {
            throw RuntimeException(ex.cause)
        }
    }

    /**
     * this function returns the encoded URL.
     *
     * @param value decoded String
     * @returns the encoded String
     */
//    fun encodeValue(value: String): String {
//        return try {
//            URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
//        } catch (ex: UnsupportedEncodingException) {
//            throw RuntimeException(ex.cause)
//        }
//    }

}