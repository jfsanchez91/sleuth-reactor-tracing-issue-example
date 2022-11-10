package com.example.demo

import org.slf4j.LoggerFactory
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.reactor.ReactorSleuth
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Duration
import kotlin.random.Random

@Component
class ReactorTest(
    private val tracer: Tracer,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ReactorTest::class.java)
    }

    data class Event(val id: Int)

    private fun receive(): Flux<Event> = Flux.fromIterable((0..20))
        .map { Event(id = it) }.flatMap { event ->
            ReactorSleuth.tracedMono(tracer, tracer.nextSpan(tracer.currentSpan())) {
                logger.info("Adding tracing information to Mono<Event>.")
                event.toMono()
            }
        }

    fun consume() = receive()
        .doOnNext { event ->
            logger.info("Fetched event: $event")
        }
        .filter { it.id % 2 == 0 }
        .groupBy { it.id % 4 }
        .flatMap(
            { group ->
                group.concatMap { event ->
                    logger.info("Processing event: $event")
                    Mono.just(event)
                        .delayElement(Duration.ofMillis(Random.nextLong(200, 500)))
                        .delaySubscription(Duration.ofMillis(Random.nextLong(200, 500)))
                        .map { Event(id = it.id * 2) }
                        .doOnNext { logger.info("Math is done $event") }
                        .onErrorReturn(event)
                        .doFinally {
                            logger.info("Processing done")
                        }
                }
            },
            4,
        )
        .doOnError { error ->
            logger.error("There was an unhandled error while processing the Event. $error")
        }
        .doFinally {
            logger.info("DONE")
        }
}
