package com.combostrap.vertx;

import com.combostrap.smtp.SmtpConfigBean;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;

import java.util.logging.Logger;


public class VertxNetServer {
    private static final Logger LOGGER = Logger.getLogger(VertxNetServer.class.getName());
    private final builder builder;
    private final ServerHealth serverHealth;
    private final ServerSsl ssl;

    public VertxNetServer(builder builder) {
        this.builder = builder;

        /**
         * Global Failure Handler (Always on)
         * This only catches exceptions thrown inside handlers, not silently-dropped failed futures.
         * A Future.join(...) that nobody listens to won't trigger this.
         */
        TowerFailureHandler failureHandler = new TowerFailureHandler(this);
        builder.vertx.exceptionHandler(failureHandler);
        LOGGER.info("Vertx Failure Handler started");

        // Ssl
        ssl = ServerSsl.create(builder.configAccessor);

        /**
         * At the beginning after data type (jackson) so that
         * other service can register health checks
         */
        this.serverHealth = new ServerHealth(this);
    }


    public static builder create(Vertx vertx, SmtpConfigBean configAccessor) {
        return new builder(vertx, configAccessor);
    }

    public Vertx getVertx() {
        return builder.vertx;
    }

    public SmtpConfigBean getConfigAccessor() {
        return builder.configAccessor;
    }

    public PrometheusMeterRegistry getMetricsRegistry() {
        return MainLauncher.prometheus.getRegistry();
    }


    public String getListeningHost() {
        return builder.listeningHost;
    }

    public int getListeningPort() {
        return builder.listeningPort;
    }


    public ServerSsl getSsl() {
        return this.ssl;
    }

    public ServerHealth getServerHealthCheck() {
        return this.serverHealth;
    }


    public static class builder {


        private final SmtpConfigBean configAccessor;
        private final Vertx vertx;
        private String listeningHost;
        private Integer listeningPort;

        public builder(Vertx vertx, SmtpConfigBean configAccessor) {
            this.configAccessor = configAccessor;
            this.vertx = vertx;
        }


        public builder setFromConfigAccessorWithPort() {
            this.listeningHost = configAccessor.listeningHost;
            LOGGER.info("The listening host was set to: " + this.listeningHost);
            this.listeningPort = configAccessor.listeningPort;
            LOGGER.info("The listening port was set to: " + this.listeningPort);
            Integer publicPort = configAccessor.publicPort;
            LOGGER.info("The public port was set to: " + publicPort);
            return this;
        }


        public VertxNetServer build() {
            return new VertxNetServer(this);
        }
    }
}
