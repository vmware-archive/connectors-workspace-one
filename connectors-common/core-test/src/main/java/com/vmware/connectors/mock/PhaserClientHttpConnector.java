/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.mock;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class PhaserClientHttpConnector implements ClientHttpConnector {
    private final Scheduler scheduler = Schedulers.elastic();
    private final ReactorClientHttpConnector reactorClientHttpConnector = new ReactorClientHttpConnector();

    // Use a phaser to ensure we don't emit a response until all parallel calls have received responses.
    // This prevents non-deterministic behavior, where OkHttp /might/ return an error status before a
    // second request has been sent. Since the failure can cancel the subscription,
    // verifying that the second call happened will fail.
    private final Phaser phaser = new Phaser() {
        @Override
        protected boolean onAdvance(int phase, int registeredParties) {
            return false;
        }
    };

    @Override
    public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {
        int phase = phaser.getPhase();
        phaser.register();
        return reactorClientHttpConnector.connect(method, uri, requestCallback)
                .doOnSuccessOrError((response, throwable) -> phaser.arriveAndDeregister())
                .delayUntil(response -> awaitAdvance(phase));
    }

    private Mono<?> awaitAdvance(int phase) {
        // Subscribe on an elastic pool because awaitAdvanceInterruptibly blocks and we don't want to
        // starve the finite reactor pool.
        return Mono.fromCallable(() -> phaser.awaitAdvanceInterruptibly(phase, 4, TimeUnit.SECONDS))
                .subscribeOn(scheduler);
    }
}
