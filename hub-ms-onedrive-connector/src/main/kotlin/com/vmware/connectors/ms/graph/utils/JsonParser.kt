/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.ms.graph.utils

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vmware.connectors.ms.graph.utils.JsonParser.mapper

/**
 * Utility class for JSON Serialization/Deserialization
 *
 * @property mapper jacksonObjectMapper Instance
 */
object JsonParser {

    val mapper = jacksonObjectMapper()

    /**
     * serializes any object to [String]
     *
     * @param value: any object
     * @return [String]
     */
    fun serialize(value: Any): String {
        return mapper.writeValueAsString(value)
    }

    /**
     * deserialize json string to [Map]
     *
     * @param string: json string
     * @return [Map]<String, Any>: deserialize json string to [Map]
     */
    fun deserialize(string: String): Map<String, Any> {
        return mapper.readValue(string)
    }

    /**
     * deserialize json string to type [T]
     *
     * @param T
     * @param string: json string
     * @exception Exception: throws exception if input json string can not be serializable to given type T
     * @return [T]: deserialize json string to [T]
     */
    inline fun <reified T : Any> deserialize(string: String) = try {
        mapper.readValue<T>(string)
    } catch (ex: Exception) {
        ex.printStackTrace()
        throw Exception(ex)
    }

    /**
     * convert value to given Type [T]
     *
     * @param value: given input object
     * @return [T]: deserialize json string to [T]
     */
    inline fun <reified T : Any> convertValue(value: Any) = mapper.convertValue<T>(value)

}