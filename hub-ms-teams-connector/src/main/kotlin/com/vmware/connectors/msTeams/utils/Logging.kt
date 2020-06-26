/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msTeams.utils

import mu.KLogger
import mu.KotlinLogging

/**
 * Creates logger for calling class
 *
 * @return returns the KLogger
 */
fun getLogger(): KLogger {
    return getLogger(Thread.currentThread().stackTrace[2].className)
}

/**
 * Creates logger for given name
 *
 * @param name name for the logger
 * @return returns KLogger with given name
 */
fun getLogger(name: String) = KotlinLogging.logger(name)
