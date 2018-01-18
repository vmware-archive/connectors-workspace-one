/*
 * Copyright © 2018 VMware, Inc. All Rights Reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */

package com.vmware.connectors.stash.utils;

public final class StashConstants {

    private StashConstants() {

    }

    private static final String STASH_PR_URL_FORMAT = "%s/rest/api/1.0/projects/%s/repos/%s/pull-requests/%s/";

    private static final String STASH_PR_ACTION_FORMAT = STASH_PR_URL_FORMAT + "%s?version=%s";
}
