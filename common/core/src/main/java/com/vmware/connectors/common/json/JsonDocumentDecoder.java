/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.json;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.spi.json.JsonProvider;
import org.apache.commons.io.IOUtils;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageDecoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON;

public class JsonDocumentDecoder implements HttpMessageDecoder<JsonDocument> {
    private final JsonProvider jsonProvider = Configuration.defaultConfiguration().jsonProvider();

    @Override
    public Map<String, Object> getDecodeHints(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response) {
        return Collections.emptyMap();
    }

    @Override
    public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
        return elementType.isAssignableFrom(JsonDocument.class)
                && (APPLICATION_JSON.isCompatibleWith(mimeType) || new MediaType("application", "*+json").isCompatibleWith(mimeType));
    }

    @Override
    public Flux<JsonDocument> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return decodeToMono(inputStream, elementType, mimeType, hints).flux();
    }

    @Override
    public Mono<JsonDocument> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        return Flux.from(inputStream)
                .flatMap(buffer -> {
                    try {
                        return Flux.just(IOUtils.toString(buffer.asInputStream(), UTF_8));
                    } catch (IOException e) {
                        return Flux.error(e);
                    }
                })
                .collect(StringBuilder::new, StringBuilder::append)
                .map(StringBuilder::toString)
                .map(message -> new JsonDocument(jsonProvider.parse(message)));
    }

    @Override
    public List<MimeType> getDecodableMimeTypes() {
        return Arrays.asList(APPLICATION_JSON, HAL_JSON);
    }
}
