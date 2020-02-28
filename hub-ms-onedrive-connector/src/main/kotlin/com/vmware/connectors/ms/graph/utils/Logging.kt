package com.vmware.connectors.ms.graph.utils

import mu.KLogger
import mu.KotlinLogging

/**
 * Creates logger for calling class
 */
fun getLogger(): KLogger {
    return getLogger(Thread.currentThread().stackTrace[2].className)
}

///**
// * Creates logger for given class object
// *
// * @param clazz: Class object
// */
//fun getLogger(clazz: Class<Any>) = getLogger(clazz.name)

/**
 * Creates logger for given name
 *
 * @param name
 */
fun getLogger(name: String) = KotlinLogging.logger(name)
