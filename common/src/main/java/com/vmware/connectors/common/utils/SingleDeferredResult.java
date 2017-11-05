/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.utils;

import org.springframework.util.Assert;
import org.springframework.web.context.request.async.DeferredResult;
import rx.Single;

/**
 * Created by Rob Worsnop on 10/31/17.
 */
class SingleDeferredResult<T> extends DeferredResult<T> {

    public SingleDeferredResult(Single<T> single) {
        initSingle(single);
    }

    public SingleDeferredResult(long timeout, Single<T> single) {
        super(timeout);
        initSingle(single);
    }

    public SingleDeferredResult(Long timeout, Object timeoutResult, Single<T> single) {
        super(timeout, timeoutResult);
        initSingle(single);
    }

    private void initSingle(Single<T> single) {
        Assert.notNull(single, "single can not be null");
        new DeferredResultSubscriber<>(single.toObservable(), this);
    }
}