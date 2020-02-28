package com.vmware.connectors.ms.graph

import org.springframework.boot.SpringApplication.run
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * MSGraphConnectorApplication
 */
@SpringBootApplication
open class MSGraphConnectorApplication

/**
 * Application boot strap
 * @return Void
 */
fun main(args: Array<String>) {
    run(MSGraphConnectorApplication::class.java, *args)
}