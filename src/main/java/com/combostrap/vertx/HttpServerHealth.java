package com.combostrap.vertx;

import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.HealthChecks;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Health checks
 * A wrapper around <a href="https://vertx.io/docs/vertx-health-check/java/">...</a>
 */
public class HttpServerHealth {

  private static final Logger LOGGER = Logger.getLogger(HttpServerHealth.class.getName());

  public static String PING_PATH = "/ping";

  public static void addHandler(VertxHttpServer httpServer) {

    /**
     * Http Handler
     */
    VertxNetServer server = httpServer.getNetServer();
    HealthChecks healthChecks = server
      .getServerHealthCheck()
      .getHealthChecks();

    HealthCheckHandler healthCheckHandler = HealthCheckHandler
      .createWithHealthChecks(healthChecks);
    httpServer.getRouter()
      .route(PING_PATH)
      .handler(healthCheckHandler);

    String url;
    try {
      url = new URL(httpServer.getHttpScheme(), "localhost", server.getListeningPort(), PING_PATH).toString();
    } catch (MalformedURLException e) {
      url = PING_PATH;
    }
    LOGGER.info("The health check end point has been added to " + url);

  }


}
