/*
 * Copyright © 2017 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.common.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Rob Worsnop on 3/30/17.
 */
public final class ContextHolder {
    private final static ThreadLocal<Map<String, Object>> context = ThreadLocal.withInitial(HashMap::new);

    private ContextHolder() {
        // do not create
    }

    public static Map<String, Object> getContext() {
        return context.get();
    }
}
