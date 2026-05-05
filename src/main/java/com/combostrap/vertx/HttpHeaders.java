package com.combostrap.vertx;

/**
 * Headers are case-insensitive
 */
public class HttpHeaders {
  public static final String CACHE_CONTROL = "Cache-Control";
  public static final String CACHE_CONTROL_NO_STORE = "no-store";

  /**
   * The standard {@link HttpForwardProxy forward} proxy
   * Not used by many proxy and not on nginx, so we can't test
   * and implement it
   */
  @SuppressWarnings("unused")
  public final static String FORWARD = "forward";
  public final static String X_FORWARDED_FOR = "X-Forwarded-For";
  public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
  public static final String X_FORWARDED_HOST = "X-Forwarded-Host";
  public static final String X_REAL_IP = "X-Real-IP";
  public static final String HOST = "Host";

  /**
   * The http header for the token defined in the openapi specification
   */
  public static final String X_API_KEY = "x-api-key";
  public static final String ACCEPT = "accept";

  /**
   * This is a pretty hard requirement on a javascript dev server to have a
   * ACCEPT headers otherwise, it will respond with a 404
   * Below is the standard browser value (copied from chrome)
   */
  public static final String ACCEPT_STANDARD_BROWSER_VALUE = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String ORIGIN = "origin";
  public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "access-control-allow-origin";
  public static final String ACCESS_CONTROL_ALLOW_HEADERS = "access-control-allow-headers";

  public static final String ACCESS_CONTROL_ALLOW_CREDENTIAL = "Access-Control-Allow-Credentials";

  public static final String REFERER = "referer";

  public static final String SET_COOKIE = "set-cookie";

  public static final String AUTHORIZATION = "authorization";
  public static final String LOCATION = "Location";

  public static final String USER_AGENT = "User-Agent";

}
