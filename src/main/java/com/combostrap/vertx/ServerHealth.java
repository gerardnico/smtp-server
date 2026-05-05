package com.combostrap.vertx;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;

import java.util.logging.Logger;

public class ServerHealth {

  private static final Logger LOGGER = Logger.getLogger(HttpServerHealth.class.getName());

  /**
   * <a href="https://vertx.io/docs/vertx-core/java/#_addressing">...</a>
   */
  private static final String HEALTH_EVENT_BUS_ADDRESS = "health";
  private final HealthChecks healthChecks;

  public ServerHealth(VertxNetServer server) {

    /**
     * Create the health checks
     */
    Vertx vertx = server.getVertx();
    this.healthChecks = HealthChecks.create(vertx);

    /**
     * Add the handler to the event bus
     * registered on the event bus
     * <a href="https://vertx.io/docs/vertx-core/java/#event_bus">...</a>
     */
    server
      .getVertx()
      .eventBus()
      .consumer(HEALTH_EVENT_BUS_ADDRESS, message -> healthChecks.checkStatus()
        .onSuccess(message::reply)
        .onFailure(err -> message.fail(0, err.getMessage()))
      );
  }

  public HealthChecks getHealthChecks() {

    return healthChecks;

  }

  /**
   * Register with the default timeout
   * healthChecks.register(
   *       "my-procedure",
   *       promise -> promise.complete(Status.OK())
   * );
   */
  public ServerHealth register(String name, Handler<Promise<Status>> procedure) {

    register(
      name,
      1000L, // the default
      procedure
    );
    return this;
  }

  /**
   * Example
   * healthChecks.register(
   *       "my-procedure",
   *       2000,
   *       promise -> promise.complete(Status.OK())
   * );
   */
  public ServerHealth register(String name, Long timeout, Handler<Promise<Status>> procedure) {

    LOGGER.info("The health check procedure (" + name + ") has been added.");

    healthChecks.register(
      name,
      timeout,
      procedure
    );
    return this;

  }

  public void register(Class<?> aClass, Handler<Promise<Status>> promiseHandler) {
    register(aClass.getSimpleName(), promiseHandler);
  }

  public Future<ServerHealthReport> getServerHealthCheckReport() {

    return this.healthChecks.checkStatus()
      .compose(checkResult-> Future.succeededFuture(new ServerHealthReport(checkResult)));


  }
}
