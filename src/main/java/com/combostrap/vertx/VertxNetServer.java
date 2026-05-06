package com.combostrap.vertx;

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

    /**
     * @param name       - the name is used on third services (ie JDBC connection)
     * @param confPrefix - a prefix for the server configuration (ie with the http name, the conf key are `http.host, http.port, ...`)
     *                   as you can create more than one server listening
     */
    public static builder create(String name, String confPrefix, Vertx vertx, ConfigAccessor configAccessor) {
        return new builder(name, confPrefix, vertx, configAccessor);
    }

    public Vertx getVertx() {
        return builder.vertx;
    }

    public ConfigAccessor getConfigAccessor() {
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
        static String HOST = "host";
        static String LISTENING_PORT = "port"; // the private listening port
        static String PUBLIC_PORT = "port.public"; // the public port (the proxy port, normally
        /**
         * Listen from all hostname
         * On ipv4 and Ipv6.
         * The wildcard implementation depends on the language
         * and in Java it works for the 2 Ip formats.
         */
        public static final String WILDCARD_IPV4_ADDRESS = "0.0.0.0";
        @SuppressWarnings("unused")
        public static final String WILDCARD_IPV6_ADDRESS = "[::]";

        private final ConfigAccessor configAccessor;
        private final String confPrefix;
        private final Vertx vertx;
        private String listeningHost;
        private Integer listeningPort;
        private Integer publicPort;
        private String smtpClientUserAgentName;

        public builder(String name, String confPrefix, Vertx vertx, ConfigAccessor configAccessor) {
            this.configAccessor = configAccessor;
            this.confPrefix = confPrefix;
            this.vertx = vertx;
        }

        String getListeningHostKey() {
            return this.confPrefix + "." + HOST;
        }

        public builder setFromConfigAccessorWithPort(int listeningPort) {
            this.listeningHost = configAccessor.getString(getListeningHostKey(), WILDCARD_IPV4_ADDRESS);
            LOGGER.info("The listening host was set to: " + this.listeningHost + " via the conf (" + getListeningHostKey() + ")");
            this.listeningPort = configAccessor.getInteger(getListeningPortKey(), listeningPort);
            LOGGER.info("The listening port was set to: " + this.listeningPort + " via the conf (" + getListeningPortKey() + ")");
            this.publicPort = configAccessor.getInteger(getPublicPortKey(), 80);
            LOGGER.info("The public port was set to: " + this.publicPort + " via the conf (" + getPublicPortKey() + ")");

            return this;
        }

        private String getListeningPortKey() {
            return this.confPrefix + "." + LISTENING_PORT;
        }

        private String getPublicPortKey() {
            return this.confPrefix + "." + PUBLIC_PORT;
        }

        public builder enableSmtpClient(String userAgentName) {
            this.smtpClientUserAgentName = userAgentName;
            return this;
        }

        public VertxNetServer build() {
            return new VertxNetServer(this);
        }
    }
}
