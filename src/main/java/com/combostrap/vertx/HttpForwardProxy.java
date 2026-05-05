package com.combostrap.vertx;

import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.Router;

public class HttpForwardProxy {


  /**
   *
   * The headers are disabled by default to prevent malicious applications
   * to forge their origin and hide where they are really coming from.
   * <p>
   * When behind a proxy the client host ip address will be the proxy server ip address,
   * not the client’s one.
   * <p>
   * To change this behavior: <a href="https://vertx.io/docs/vertx-web/java/#_forward_support">...</a>
   * <p>
   * <p>
   * More info:
   * <a href="https://tools.ietf.org/html/rfc7239#section-4">...</a>
   * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded">...</a>
   * <p>
   *
   */
  public static void addAllowForwardProxy(Router router) {
    /**
     * forward is disabled by default, enabling it
     * to get the
     * {@link HttpHeaders.X_FORWARDED_FOR},
     * {@link HttpHeaders.X_FORWARDED_HOST}
     * {@link HttpHeaders.X_FORWARDED_PROTO}
      */

    router.allowForward(AllowForwardHeaders.FORWARD);
  }

}
