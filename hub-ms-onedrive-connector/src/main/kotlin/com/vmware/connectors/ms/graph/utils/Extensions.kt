/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.ms.graph.utils

import com.vmware.connectors.common.payloads.response.Card
import com.vmware.connectors.common.payloads.response.Cards
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.client.*
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets


/**
 * Extension Function for Map which returns the value for the given key as String
 * or null if the value for the given key is not present
 *
 * @receiver Map
 * @param key is the Key of the Map
 * @returns value for the given key as String
 */
fun Map<String, *>.getStringOrNull(key: String) = this[key] as? String

/**
 * Extension Function for Map which returns the value for the given key  as String
 * or Default Value if the value for the given key is not present
 *
 * @receiver Map
 * @param key is the key of the Map
 * @param default is the default Map
 * @returns the value for the given key as String
 */
fun Map<String, *>.getStringOrDefault(
        key: String,
        default: String = ""
) = this.getStringOrNull(key) ?: default

/**
 * Extension Function for Map which returns the value for the given key as List
 * or null if the value for the given key  is not present
 *
 * @receiver Map
 * @param key is the Key of the Map
 * @returns the value for the given key as List
 */
fun <E> Map<String, *>.getListOrNull(key: String) = try {
    getListOrException<E>(key)
} catch (_: Exception) {
    null
}

/**
 * Extension Function for Map which returns the value for the given key as List
 * or throws Exception if the value for the given key is not present
 *
 * @receiver Map
 * @param key is the Key of the Map
 * @returns value for the given key as List
 */
fun <E> Map<String, *>.getListOrException(key: String) = JsonParser.convertValue<List<E>>(this[key]!!)

/**
 * Extension function for Cards class. Adds given card objects to Cards Object.
 *
 * @receiver Cards
 * @param cards: list of Card objects
 * @return Cards object
 */
fun Cards.addCards(cards: List<Card>): Cards = this.apply { this.cards.addAll(cards) }

/**
 * Extension function for Resource class which converts current resource to ByteArray
 *
 * @receiver Resource
 * @return ByteArray
 */
fun Resource.readAsByteArray(): ByteArray = inputStream
        .use {
            it.readAllBytes()
        }

/**
 * Extension function for Resource class which converts current resource to string
 *
 * @receiver Resource
 * @return String
 */
fun Resource.readAsString() = String(readAsByteArray())

/**
 * Creates Response Exception object while http request call
 *
 * @param response: ClientResponse
 * @param request: HttpRequest
 * @return WebClientResponseException
 */
private suspend fun createResponseException(
        response: ClientResponse,
        request: HttpRequest? = null
): WebClientResponseException {
    return DataBufferUtils
            .join(response.body(BodyExtractors.toDataBuffers()))
            .map { dataBuffer ->
                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                DataBufferUtils.release(dataBuffer)
                bytes
            }
            .defaultIfEmpty(ByteArray(0))
            .map { bodyBytes ->
                val charset = response.headers()
                        .contentType()
                        .map { it.charset }
                        .orElse(StandardCharsets.ISO_8859_1)!!
                if (HttpStatus.resolve(response.rawStatusCode()) != null)
                    WebClientResponseException.create(
                            response.statusCode().value(),
                            response.statusCode().reasonPhrase,
                            response.headers().asHttpHeaders(),
                            bodyBytes,
                            charset,
                            request
                    )
                else UnknownHttpStatusCodeException(response.rawStatusCode(), response.headers().asHttpHeaders(), bodyBytes, charset, request)
            }
            .awaitFirst()
}

/**
 * Extension function for WebClient.RequestHeadersSpec which consumes if there is any error while making http call
 *
 * @receiver WebClient.RequestHeadersSpec
 * @param onError: callback function which will be called if there is any error during http call
 * @return ClientResponse
 */
suspend fun WebClient.RequestHeadersSpec<out WebClient.RequestHeadersSpec<*>>.awaitExchangeAndThrowError(onError: ((WebClientResponseException) -> Unit)): ClientResponse {
    return awaitExchange()
            .also { response ->
                if (response.statusCode().isError) {
                    val exp = createResponseException(response)
                    onError(exp)
                    throw exp
                }
            }
}

/**
 * Extension Function For String which returns the Decoded String
 * @receiver [String]
 * @return the urlDecodedString
 */
fun String.urlDecode(): String = URLDecoder.decode(this, Charsets.UTF_8.toString())

/**
 * this function will return the url without parameters
 *
 * @param url URL String
 * @returns the url without parameters
 */
@Throws(URISyntaxException::class)
fun getUrlWithoutParameters(url: String): String {
    val uri = URI(url)
    return URI(uri.scheme,
            uri.authority,
            uri.path,
            null, // Ignore the query part of the input url
            uri.fragment).toString()
}

/**
 * this function will return the url parameters
 *
 * @param url URL String
 * @returns the url parameters
 */
@Throws(UnsupportedEncodingException::class)
fun getUrlQueryParameters(url: String): Map<String, String>? {
    val query = URL(url).query
    val pairs = query.split("&")
    return if (pairs.isNotEmpty()) {
        pairs.map {
            val idx = it.indexOf("=")
            (URLDecoder.decode(it.substring(0, idx), "UTF-8") to URLDecoder.decode(it.substring(idx + 1), "UTF-8"))
        }.toMap()
    } else null
}

/**
 * Extension Function for String which returns the Base64Decoded String
 *
 * @receiver String
 * @returns decoded string
 */
fun String?.toBase64DecodedString(): String? {
    return this?.let {
        Base64Utils.decode(it).toString(Charset.defaultCharset())
    }
}

/**
 * Extension Function for AnyType to convert the Receiver to the Required Type
 *
 * @receiver Any Type
 * @returns the Required Type
 */
inline fun <reified T : Any> Any.convertValue() = JsonParser.convertValue<T>(this)