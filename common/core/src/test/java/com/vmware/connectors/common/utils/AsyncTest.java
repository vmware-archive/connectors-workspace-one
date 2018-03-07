/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.utils;

import com.vmware.connectors.common.context.ContextHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by Rob Worsnop on 3/31/17.
 */
class AsyncTest {

    @BeforeEach
    void setup() {
        ContextHolder.getContext().clear();
    }

    @Test
    void testToSingleSingleThreadSuccess() throws Exception {
        testToSingleSuccess(false);
    }

    @Test
    void testToSingleMultithreadSuccess() throws Exception {
        testToSingleSuccess(true);
    }

    @Test
    void testToSingleFailure() throws Exception {
        Assertions.assertThrows(ExecutionException.class, () -> {
            Async.toSingle(listenableFuture(true, true)).toBlocking().toFuture().get();
        });
    }

    private void testToSingleSuccess(boolean threaded) throws Exception {
        ContextHolder.getContext().put("key", "Test");
        String result = Async.toSingle(listenableFuture(threaded, false))
                .map(res -> ContextHolder.getContext().get("key") + res)
                .toBlocking().toFuture().get();
        assertThat(result, equalTo("Test is a success!"));
        // repeat to make sure the context doesn't get whacked by the first one
        result = Async.toSingle(listenableFuture(threaded, false))
                .map(res -> ContextHolder.getContext().get("key") + res)
                .toBlocking().toFuture().get();
        assertThat(result, equalTo("Test is a success!"));
    }


    private static ListenableFuture<String> listenableFuture(boolean threaded, boolean fail) {
        return new ListenableFuture<String>() {
            @Override
            public void addCallback(ListenableFutureCallback<? super String> callback) {
                addCallback(callback, callback);
            }

            @Override
            public void addCallback(SuccessCallback<? super String> successCallback, FailureCallback failureCallback) {
                Runnable success = () -> successCallback.onSuccess(" is a success!");
                Runnable failure = () -> failureCallback.onFailure(new RuntimeException());
                Runnable action = fail ? failure : success;

                if (threaded) {
                    new Thread(action).start();
                } else {
                    action.run();
                }
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public String get() throws InterruptedException, ExecutionException {
                return null;
            }

            @Override
            public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }
        };
    }
}

