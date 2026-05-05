package com.combostrap.vertx;

import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Handler;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for the failures
 * that happen on the Vertx threads
 */
public class TowerFailureHandler implements Handler<Throwable> {

    Logger LOGGER = Logger.getLogger(TowerFailureHandler.class.getName());

    private final Counter failureCounter;


    public TowerFailureHandler(VertxNetServer server) {


        PrometheusMeterRegistry metricsRegistry;
        Counter failureCounterTemp;

        metricsRegistry = server.getMetricsRegistry();
        failureCounterTemp = metricsRegistry
                .counter("vertx_failure");

        failureCounter = failureCounterTemp;

    }

    @Override
    public void handle(Throwable thrown) {

        if (this.failureCounter != null) {
            this.failureCounter.increment();
        }
        LOGGER.log(Level.SEVERE, "Vertx Failure Handler failed", thrown);

    }


}
