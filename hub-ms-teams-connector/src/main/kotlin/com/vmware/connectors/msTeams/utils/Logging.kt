package com.vmware.connectors.msTeams.utils

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
