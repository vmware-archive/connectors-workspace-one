/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.msTeams

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * MsGraphTeamsApplication
 */
@SpringBootApplication
open class MsGraphTeamsApplication

/**
 * Application boot strap
 * @return Void
 */
fun main(args: Array<String>) {
    runApplication<MsGraphTeamsApplication>(*args)
}