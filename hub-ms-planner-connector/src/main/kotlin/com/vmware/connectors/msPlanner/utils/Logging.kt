/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msPlanner.utils

import mu.KLogger
import mu.KotlinLogging

/**
 * Creates logger for calling class
 */
fun getLogger(): KLogger {
    return getLogger(Thread.currentThread().stackTrace[2].className)
}

/**
 * Creates logger for given name
 *
 * @param name
 */
fun getLogger(name: String) = KotlinLogging.logger(name)
