/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.concur

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * ConcurApplication
 */
@SpringBootApplication
open class ConcurApplication

/**
 * Application boot strap
 * @return Void
 */
fun main(args: Array<String>) {
    runApplication<ConcurApplication>(*args)
}