/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.utils;

import rx.Single;

public final class SingleUtil {
    private SingleUtil() {
        // util class
    }

    public static <R> Single<R> skip404(Throwable throwable) {
        return ObservableUtil.<R>skip404(throwable).toSingle();
    }

    public static <R> Single<R> skip400(Throwable throwable) {
        return ObservableUtil.<R>skip400(throwable).toSingle();
    }
}
