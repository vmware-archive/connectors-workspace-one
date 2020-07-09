/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.boxNotes

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * BoxNotesApplication
 */
@SpringBootApplication
open class BoxNotesApplication

/**
 * Application boot strap
 * @return Void
 */
fun main(args: Array<String>) {
    runApplication<BoxNotesApplication>(*args)
}