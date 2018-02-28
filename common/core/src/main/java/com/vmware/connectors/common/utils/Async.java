/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.utils;

import com.vmware.connectors.common.context.ContextHolder;
import org.slf4j.MDC;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.HttpStatusCodeException;
import rx.Single;
import rx.SingleSubscriber;
import rx.functions.Func1;

import java.util.Map;
import java.util.function.Function;

/**
 * Utility for Async operations
 *
 * @author Rob Worsnop
 */
public final class Async {
    private Async() {
        // Do not construct
    }

    private static class ContextCopyingListenableFutureCallback <T> implements ListenableFutureCallback<T> {

        private final Map<String, Object> callerContext;
        private final Map<String, String> mdcMap;
        private final SingleSubscriber<T> subscriber;

        // Default access modifier.
        ContextCopyingListenableFutureCallback(
                Map<String, Object> callerContext,
                Map<String, String> mdcMap,
                SingleSubscriber<T> subscriber
        ) {
            this.callerContext = callerContext;
            this.mdcMap = mdcMap;
            this.subscriber = subscriber;
        }

        @Override
        public void onFailure(Throwable throwable) {
            // We do not want to lose the user information("principal") set in MDC.
            final Map<String, Object> callbackContext = ContextHolder.getContext();
            final boolean same = callerContext == callbackContext; // NOPMD - We dont have to check identity.

            if (!same && !CollectionUtils.isEmpty(this.mdcMap)) {
                MDC.setContextMap(this.mdcMap);
            }
            subscriber.onError(throwable);
        }

        @Override
        public void onSuccess(T result) {
            // Copy the context from the calling thread to the callback thread's context
            // unless the calling thread *is* the callback thread
            Map<String, Object> callbackContext = ContextHolder.getContext();
            boolean same = callerContext == callbackContext; // NOPMD I really do want identity
            if (!same) {
                callbackContext.putAll(callerContext);
                if (!CollectionUtils.isEmpty(this.mdcMap)) {
                    MDC.setContextMap(this.mdcMap);
                }
            }
            try {
                subscriber.onSuccess(result);
            } finally {
                if (!same) {
                    callerContext.keySet().forEach(callbackContext::remove);
                    MDC.clear();
                }
            }
        }

    }

    /**
     * Converts a ListenableFuture object to a Single.
     *
     * @param future The future to convert
     * @param <T> The type of object being omitted
     * @return
     */
    public static <T> Single<T> toSingle(ListenableFuture<T> future) {
        // Remember the context from the calling thread
        Map<String, Object> callerContext = ContextHolder.getContext();
        // Remember the MDC from the calling thread
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        return Single.create(subscriber ->
                future.addCallback(new ContextCopyingListenableFutureCallback<>(callerContext, mdcMap, subscriber)));
    }

    public static <T> Func1<Throwable, Single<T>> ifStatusCodeException(Function<HttpStatusCodeException, T> func) {
        return throwable -> {
            if (throwable instanceof HttpStatusCodeException) {
                return Single.just(func.apply((HttpStatusCodeException) throwable));
            } else {
                return Single.error(throwable);
            }
        };
    }

}
