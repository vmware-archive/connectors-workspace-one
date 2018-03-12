/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.utils;

import org.reactivestreams.Publisher;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.util.context.Context;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Utility for Reactive operations
 *
 * @author Rob Worsnop
 */
public final class Reactive {
    private Reactive() {
        // Do not construct
    }

    /**
     * Sets up the reactive context based on the MDC
     * already set on the thread by servlet filter and an interceptor.
     * This method should be called at the end of each composed Flux and
     * passed to Flux.subscriberContext
     * This is a temporary solution that works while we're still stuck on
     * Servlet 3.0 instead of being fully reactive.
     * When we move to full-blown webflux we can set up the reactive context
     * in a WebFilter, with no need to do anything in controllers at all.
     * This method will be removed once we have gone fully reactive.
     * @return the reactive context
     */
    public static Context setupContext() {
        return Context.empty().put("mdc", MDC.getCopyOfContextMap());
    }

    /**
     * Intended for use with Flux.doOnEach, this method allows processing
     * on a item (e.g., logging) to be done with MDC set on the
     * thread.
     * @param consumer the action to be performed on the emitted item
     * @param <R> The item type
     * @return a signal consumer to be passed to Flux.doOnEach
     */
    public static <R> Consumer<Signal<R>> wrapForItem(Consumer<R> consumer) {
        return signal -> {
            if (signal.isOnNext()) {
                wrapCall(signal.getContext(), () -> {
                    consumer.accept(signal.get());
                    return null;
                });
            }
        };
    }

    /**
     * Allows mapping methods to assume that MDC is set on the thread.
     * Replace .map(foo) with .flatMap(Reactive.wrapMapper(foo))
     *
     * @param mapper The function for transforming the item
     * @param <T> The type of the item being transformed
     * @param <R> The type being transformed to
     * @return
     */
    public static <T, R> Function<T, Publisher<R>> wrapMapper(Function<? super T, ? extends R> mapper) {
        return item -> Mono.subscriberContext()
                .map(context -> wrapCall(context, () -> mapper.apply(item)));
    }

    public static <T, R> Function<T, Publisher<R>> wrapFlatMapper(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return item -> Mono.subscriberContext().flux()
                .flatMap(context -> wrapCall(context, () -> mapper.apply(item)));
    }

    public static Mono<ClientResponse> checkStatus(ClientResponse response) {
        return checkStatus(response, httpStatus -> !httpStatus.isError());
    }

    public static <T> Mono<ResponseEntity<T>> toResponseEntity(ClientResponse response, Class<T> clazz) {
        return response.bodyToMono(clazz)
                .map(body -> ResponseEntity.status(response.statusCode())
                        .headers(response.headers().asHttpHeaders())
                        .body(body));
    }

    public static Mono<ClientResponse> checkStatus(ClientResponse response, Predicate<HttpStatus> statusPredicate) {
        if (statusPredicate.test(response.statusCode())) {
            return Mono.just(response);
        }
        Charset charset = response.headers().contentType()
                .map(MimeType::getCharset)
                .orElse(StandardCharsets.ISO_8859_1);
        return response.bodyToMono(byte[].class)
                .flatMap(body -> Mono.error(new WebClientResponseException(
                        "Unexpected response",
                        response.statusCode().value(),
                        response.statusCode().getReasonPhrase(),
                        response.headers().asHttpHeaders(),
                        body,
                        charset
                )));
    }

    public static <R> Flux<R> skipOnStatus(Throwable throwable, Predicate<HttpStatus> statusPredicate) {
        if (throwable instanceof WebClientResponseException
                && statusPredicate.test(WebClientResponseException.class.cast(throwable).getStatusCode())) {
            return Flux.empty();
        } else {
            return Flux.error(throwable);
        }
    }

    public static <R> Flux<R> skipOnBadRequest(Throwable throwable) {
        return skipOnStatus(throwable, HttpStatus.BAD_REQUEST);
    }

    public static <R> Flux<R> skipOnNotFound(Throwable throwable) {
        return skipOnStatus(throwable, HttpStatus.NOT_FOUND);
    }

    public static <R> Flux<R> skipOnStatus(Throwable throwable, HttpStatus httpStatus) {
        return skipOnStatus(throwable, status -> status.equals(httpStatus));
    }

    public static Flux<ClientResponse> skipOnStatus(ClientResponse clientResponse, Predicate<HttpStatus> statusPredicate) {
        if (statusPredicate.test(clientResponse.statusCode())) {
            return Flux.empty();
        } else {
            return Flux.just(clientResponse);
        }
    }

    public static Flux<ClientResponse> skipOnStatus(ClientResponse clientResponse, HttpStatus httpStatus) {
        return skipOnStatus(clientResponse, status -> status.equals(httpStatus));
    }

    private static <R> R wrapCall(Context context, Supplier<R> supplier) {
        Map<String, String> savedContextMap = MDC.getCopyOfContextMap();
        MDC.setContextMap(context.get("mdc"));

        try {
            return supplier.get();
        } finally {
            MDC.setContextMap(savedContextMap);
        }
    }
}
