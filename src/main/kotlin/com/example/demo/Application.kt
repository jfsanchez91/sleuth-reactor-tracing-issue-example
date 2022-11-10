package com.example.demo

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

@SpringBootApplication
@ConfigurationPropertiesScan
class Application(
    private val reactorTest: ReactorTest,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(Application::class.java)
        private val scheduler: Scheduler = Schedulers.newBoundedElastic(
            Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE,
            Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
            "test",
            60,
            true,
        )
    }

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReadyEvent() {
        logger.info("Application started.")
        reactorTest.consume().count()
            .subscribeOn(scheduler)
            .subscribe()
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
