/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.dynamics

import org.springframework.boot.SpringApplication.run
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * MSDynamicsApplication
 */
@SpringBootApplication
open class MSDynamicsApplication

/**
 * Application boot strap
 * @return Void
 */
fun main(args: Array<String>) {
    run(MSDynamicsApplication::class.java, *args)
}