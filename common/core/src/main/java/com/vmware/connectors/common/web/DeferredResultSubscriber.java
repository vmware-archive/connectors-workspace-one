/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.web;

import org.springframework.web.context.request.async.DeferredResult;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 * Created by Rob Worsnop on 10/31/17.
 */
class DeferredResultSubscriber<T> extends Subscriber<T> implements Runnable {

    private final DeferredResult<T> deferredResult;

    private final Subscription subscription;

    private boolean completed;

    public DeferredResultSubscriber(Observable<T> observable, DeferredResult<T> deferredResult) {

        this.deferredResult = deferredResult;
        this.deferredResult.onTimeout(this);
        this.deferredResult.onCompletion(this);
        this.subscription = observable.subscribe(this);
    }

    @Override
    public void onNext(T value) {
        if (!completed) {
            deferredResult.setResult(value);
        }
    }

    @Override
    public void onError(Throwable e) {
        deferredResult.setErrorResult(e);
    }

    @Override
    public void onCompleted() {
        completed = true;
    }

    @Override
    public void run() {
        this.subscription.unsubscribe();
    }
}