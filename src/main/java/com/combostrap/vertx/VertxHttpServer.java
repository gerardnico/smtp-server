package com.combostrap.vertx;

import com.combostrap.common.Javas;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerFormat;

import java.nio.file.NotDirectoryException;
import java.util.logging.Logger;


public class VertxHttpServer {
    private static final Logger LOGGER = Logger.getLogger(VertxHttpServer.class.getName());
    private final builder builder;
    private Router router;


    public VertxHttpServer(builder builder) {
        this.builder = builder;
    }

    public static builder builderFromServer(VertxNetServer server) {

        return new builder(server);

    }

    /**
     * Mount, Listen and starts
     *
     * @param appName - the App name for logging
     */
    public Future<VertxHttpServer> mountListen(String appName) {
        HttpServerOptions options = new HttpServerOptions()
                .setLogActivity(false)
                .setHost(this.builder.netServer.getListeningHost())
                .setPort(this.builder.netServer.getListeningPort());
        /**
         * https://vertx.io/docs/apidocs/io/vertx/core/net/PemKeyCertOptions.html
         */
        ServerSsl ssl = this.builder.netServer.getSsl();
        if (ssl.isOn()) {
            options
                    .setKeyCertOptions(new PemKeyCertOptions()
                            .addKeyPath(ssl.getKey().toAbsolutePath().toString())
                            .addCertPath(ssl.getCert().toAbsolutePath().toString()))
                    .setSsl(true);
        } else {
            options.setSsl(false);
        }
        io.vertx.core.http.HttpServer httpServer = this.builder.netServer.getVertx().createHttpServer(options);

        /**
         * Health Check
         * At the end because the services can register
         * during the mount
         * (The argument is passed by reference, it may then also work at the beginning?)
         */

        HttpServerHealth.addHandler(this);

        /**
         * Listen
         */
        return httpServer
                .requestHandler(router)
                .listen()
                .compose(vertxHttpServer -> {
                    LOGGER.info(appName + " HTTP server mounted on port " + vertxHttpServer.actualPort());
                    return Future.succeededFuture(this);
                });

    }

    VertxNetServer getNetServer() {
        return this.builder.netServer;
    }

    public Router getRouter() {
        return router;
    }

    /**
     * @return the scheme always secure
     */
    public String getHttpScheme() {
        if (getNetServer().getSsl().isOn()) return "https";
        return "http";
    }


    public static class builder {
        private final VertxNetServer netServer;
        private boolean addBodyHandler;
        private boolean addWebLog;
        private boolean isBehindProxy;
        private boolean enableMetrics;
        private boolean enableFailureHandler;

        public builder(VertxNetServer server) {
            this.netServer = server;
        }

        /**
         * A handler which gathers the entire request body and sets it on the {@link RoutingContext}
         * You can't request the body from the request after-wards
         * You need to get if from the context object
         * <p>
         * BodyHandler is required to process POST requests for instance
         */
        public builder addBodyHandler() {
            this.addBodyHandler = true;
            return this;
        }

        /**
         * Logging Web Request
         */
        public builder addWebLog() {
            this.addWebLog = true;
            return this;
        }

        public builder setBehindProxy() {
            this.isBehindProxy = true;
            return this;
        }

        public builder addMetrics() {
            this.enableMetrics = true;
            return this;
        }

        public VertxHttpServer build() {
            VertxHttpServer httpServer = new VertxHttpServer(this);
            httpServer.router = this.buildRouter(httpServer);


            return httpServer;
        }

        private Router buildRouter(VertxHttpServer httpServer) {
            Vertx vertx = this.netServer.getVertx();
            Router router = Router.router(vertx);
            if (this.addBodyHandler) {
                /**
                 * It works also with OpenApi
                 */
                int bodyLimit5mb = 1024 * 1024 * 5;
                BodyHandler bodyHandler = BodyHandler
                        .create()
                        .setHandleFileUploads(true)
                        .setBodyLimit(bodyLimit5mb);
                final String BODY_HANDLER_CONF = "body.handler.upload-dir";
                String uploadDir = this.netServer.getConfigAccessor().getString(BODY_HANDLER_CONF);
                if (uploadDir == null) {
                    try {
                        uploadDir = Javas.getBuildDirectory(VertxHttpServer.class).resolve("vertx-uploaded-files").toAbsolutePath().toString();
                    } catch (NotDirectoryException e) {
                        uploadDir = "temp/vertx-uploaded-files";
                    }

                }
                bodyHandler.setUploadsDirectory(uploadDir);
                router.route().handler(bodyHandler);
            }
            if (this.addWebLog) {
                router.route().handler(new WebLogger(LoggerFormat.DEFAULT));
            }
            if (this.isBehindProxy) {
                HttpForwardProxy.addAllowForwardProxy(router);
            }
            if (this.enableMetrics) {
                MainLauncher.prometheus.mountOnRouter(router, VertxPrometheusMetrics.DEFAULT_METRICS_PATH);
            }
            /**
             * Produce an error, not only for dev, also for production to live test (mail, ...)
             */
            router.get(ErrorFakeHandler.URI_PATH).handler(new ErrorFakeHandler());
            return router;
        }
    }
}
