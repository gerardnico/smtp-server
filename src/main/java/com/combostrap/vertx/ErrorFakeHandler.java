package com.combostrap.vertx;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * This path will throw
 * It's used only in test
 */
public class ErrorFakeHandler implements Handler<RoutingContext> {

  public static final String URI_PATH = "/fail";
  public static final String FAILURE_MESSAGE = "An unexpected error that should be logged";


  @Override
  public void handle(RoutingContext event) {
    throw new RuntimeException(FAILURE_MESSAGE);
  }

}
