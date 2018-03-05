/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.mock;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.COOKIE;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

public class MockClientHttpConnector implements ClientHttpConnector {
    private final RequestHandler requestHandler;

    public MockClientHttpConnector(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    @SuppressWarnings("PMD") // Method is long but we can delete when Spring supports mocking WebClient https://jira.spring.io/browse/SPR-15286
    public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {
        try {
            MockClientHttpRequest clientHttpRequest = new MockClientHttpRequest(method, uri);
            requestCallback.apply(clientHttpRequest).block();
            InputStream body = clientHttpRequest.getBody()
                    .reduce((b1, b2) -> b2.write(b1))
                    .map(DataBuffer::asInputStream)
                    .block();
            org.springframework.mock.http.client.MockClientHttpRequest request = new org.springframework.mock.http.client.MockClientHttpRequest();
            request.setMethod(method);
            request.setURI(uri);
            clientHttpRequest.getHeaders().entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(COOKIE))
                    .forEach(entry -> request.getHeaders().addAll(entry.getKey(), entry.getValue()));
            String requestCookies = clientHttpRequest.getCookies().values().stream()
                    .flatMap(Collection::stream)
                    .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                    .collect(Collectors.joining("; "));
            request.getHeaders().add(HttpHeaders.COOKIE, requestCookies);
            if (body != null) {
                IOUtils.copy(body, request.getBody());
            }

            org.springframework.http.client.ClientHttpResponse response = requestHandler.handle(request);

            MockClientHttpResponse mockClientHttpResponse = new MockClientHttpResponse(response.getStatusCode());
            mockClientHttpResponse.getHeaders().addAll(response.getHeaders());

            List<String> responseCookies = Optional.ofNullable(
                    response.getHeaders().get(SET_COOKIE)).orElse(Collections.emptyList());
            responseCookies.forEach(cookie -> {
                ResponseCookie responseCookie = toResponseCookiue(cookie);
                mockClientHttpResponse.getCookies().add(responseCookie.getName(), responseCookie);
            });

            DataBuffer bodyBuffer = new DefaultDataBufferFactory().allocateBuffer();
            IOUtils.copy(response.getBody(), bodyBuffer.asOutputStream());
            mockClientHttpResponse.setBody(Mono.just(bodyBuffer));
            return Mono.just(mockClientHttpResponse);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    ResponseCookie toResponseCookiue(String cookie) {
        String [] tokens = cookie.split("; |;");
        SortedMap<String, String> cookieMap  =
                Arrays.stream(tokens).collect(
                        TreeMap::new,
                        (map, cookie1) -> map.put(StringUtils.substringBefore(cookie1, "="), StringUtils.substringAfter(cookie1, "=")),
                        (m1,m2)->{});

        String name = cookieMap.firstKey();
        String value = cookieMap.get(name);
        return ResponseCookie.from(name, value)
                .domain(cookieMap.get("Domain"))
                .path(cookieMap.get("Path"))
                .secure(cookieMap.containsKey("Secure"))
                .httpOnly(cookieMap.containsKey("HttpOnly")).build();
    }
}
