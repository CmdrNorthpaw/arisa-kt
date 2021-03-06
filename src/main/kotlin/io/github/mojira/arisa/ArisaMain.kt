package io.github.mojira.arisa

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import io.github.mojira.arisa.infrastructure.QueryCache
import io.github.mojira.arisa.infrastructure.config.Arisa
import io.github.mojira.arisa.infrastructure.jira.connectToJira
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

val log = LoggerFactory.getLogger("Arisa")

fun main() {
    val config = Config { addSpec(Arisa) }
        .from.yaml.watchFile("arisa.yml")
        .from.json.watchFile("arisa.json")
        .from.env()
        .from.systemProperties()

    var jiraClient =
        connectToJira(
            config[Arisa.Credentials.username],
            config[Arisa.Credentials.password],
            config[Arisa.Issues.url]
        )
    var lastRelog = Instant.now()

    log.info("Connected to jira")

    val lastRunFile = File("last-run")
    val lastRun =
        (if (lastRunFile.exists())
            lastRunFile.readText()
        else "")
            .split(",")

    var lastRunTime =
        if (lastRun[0].isNotEmpty())
            Instant.ofEpochMilli(lastRun[0].toLong())
        else Instant.now().minus(5, ChronoUnit.MINUTES)

    var rerunTickets = lastRun.subList(1, lastRun.size).toSet()
    val failedTickets = mutableSetOf<String>()

    val cache = QueryCache()
    var moduleExecutor = ModuleExecutor(jiraClient, config, cache)

    while (true) {
        // save time before run, so nothing happening during the run is missed
        val curRunTime = Instant.now()
        val executionResults = moduleExecutor.execute(lastRunTime, rerunTickets)

        if (executionResults.successful) {
            rerunTickets = emptySet()
            failedTickets.addAll(executionResults.failedTickets)
            val failed = failedTickets.joinToString("") { ",$it" } // even first entry should start with a comma

            lastRunFile.writeText("${curRunTime.toEpochMilli()}$failed")
            lastRunTime = curRunTime
        } else if (lastRelog.plus(1, ChronoUnit.MINUTES).isAfter(Instant.now())) {
            // If last relog was more than a minute before and execution failed with an exception, relog
            jiraClient = connectToJira(
                config[Arisa.Credentials.username],
                config[Arisa.Credentials.password],
                config[Arisa.Issues.url]
            )
            moduleExecutor = ModuleExecutor(jiraClient, config, cache)
        }

        TimeUnit.SECONDS.sleep(config[Arisa.Issues.checkInterval])
    }
}
