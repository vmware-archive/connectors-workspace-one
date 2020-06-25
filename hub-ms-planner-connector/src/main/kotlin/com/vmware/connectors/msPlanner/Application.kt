/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msPlanner

import org.springframework.boot.SpringApplication.run
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * MsPlannerConnectorApplication
 */
@SpringBootApplication
open class MsPlannerConnectorApplication

/**
 * Application boot strap
 * @return Void
 */
fun main(args: Array<String>) {
    run(MsPlannerConnectorApplication::class.java, *args)
}