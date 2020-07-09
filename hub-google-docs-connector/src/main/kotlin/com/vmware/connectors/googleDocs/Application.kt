/*
* Copyright © 2020 VMware, Inc. All Rights Reserved.
* SPDX-License-Identifier: BSD-2-Clause
*/

package com.vmware.connectors.googleDocs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * GoogleDocsApplication
 */
@SpringBootApplication
open class GoogleDocsApplication

/**
 * Application boot strap
 * @return Void
 */
fun main(args: Array<String>) {
    runApplication<GoogleDocsApplication>(*args)
}