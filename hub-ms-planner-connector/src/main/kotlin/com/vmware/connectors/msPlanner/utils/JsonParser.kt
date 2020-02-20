package com.vmware.connectors.msPlanner.utils

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Utility class for JSON Serialization/Deserialization
 */
object JsonParser {

    val mapper = jacksonObjectMapper()

    val logger = getLogger()

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
        return mapper.readValue<Map<String, Any>>(string)
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
    } catch (ex: Exception) { // raised when preconditions not met
        logger.error(ex) { "Failed to deserialize: $string" }
        throw Exception(ex)
    }

    /**
     * deserialize json string with custom types to type [T]
     *
     * @param T: output type
     * @param MT: Mixin Type
     * @param M: Mixin
     * @param string: json string
     * @exception Exception: throws exception if input json string can not be serializable to given type T
     * @return [T]: deserialize json string to [T]
     */
    inline fun <reified T : Any, reified MT : Any, reified M : Any> deserializeWithMixins(string: String) = try {
        mapper.addMixIn(MT::class.java, M::class.java).readValue<T>(string)
    } catch (ex: Exception) { // raised when preconditions not met
        logger.error(ex) { "Failed to deserializeWithMixins: $string" }
        throw Exception(ex)
    }

    /**
     * convert value to given Type [T]
     *
     * @param value: given input object
     * @return [T]: deserialize json string to [T]
     */
    inline fun <reified T : Any> convertValue(value: Any) = mapper.convertValue<T>(value)

    /**
     * convert value to given type [claz]
     *
     * @param value: given input object
     * @param claz: given transformer type
     * @return [claz]: deserialize json string to [claz]
     */
    fun convertValue(value: Any, claz: Class<Any>) = mapper.convertValue(value, claz)
}
