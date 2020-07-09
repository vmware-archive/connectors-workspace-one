/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.sfLeads

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * SfLeadsApplication
 */
@SpringBootApplication
open class SfLeadsApplication

/**
 * Application boot strap
 * @return Void
 */
fun main(args: Array<String>) {
    runApplication<SfLeadsApplication>(*args)
}