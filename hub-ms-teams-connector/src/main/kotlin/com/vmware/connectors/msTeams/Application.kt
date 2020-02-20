package com.vmware.connectors.msTeams

import org.springframework.boot.SpringApplication.run
import org.springframework.boot.autoconfigure.SpringBootApplication

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
    run(MsGraphTeamsApplication::class.java, *args)
}