package com.combostrap.vertx;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerFormatter;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.impl.Utils;

import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Due to the proxy, we get always the remote of the proxy as remote
 * <a href="https://github.com/vert-x3/vertx-web/issues/1445">...</a>
 * <p>
 * The solution is not the same, we have created the function {@link HttpRequestUtil#getRealRemoteClientIp(HttpServerRequest)}
 * and used it in the code below to initialize the remote client
 * <p>
 */
public class WebLogger implements LoggerHandler {


    private static final Logger log4jLogger = Logger.getLogger(WebLogger.class.getName());

    /**
     * log before request or after
     */
    private final boolean immediate;

    /**
     * the current chosen format
     */
    private final LoggerFormat format;

    public WebLogger(boolean immediate, LoggerFormat format) {
        this.immediate = immediate;
        this.format = format;
    }

    public WebLogger(LoggerFormat format) {
        this(false, format);
    }


    private void log(RoutingContext context, long timestamp, String remoteClient, HttpVersion version, HttpMethod method, String uri) {
        HttpServerRequest request = context.request();
        long contentLength = 0;
        if (immediate) {
            Object obj = request.headers().get("content-length");
            if (obj != null) {
                try {
                    contentLength = Long.parseLong(obj.toString());
                } catch (NumberFormatException e) {
                    // ignore it and continue
                }
            }
        } else {
            contentLength = request.response().bytesWritten();
        }
        String versionFormatted = "-";
        switch (version) {
            case HTTP_1_0:
                versionFormatted = "HTTP/1.0";
                break;
            case HTTP_1_1:
                versionFormatted = "HTTP/1.1";
                break;
            case HTTP_2:
                versionFormatted = "HTTP/2.0";
                break;
        }

        final MultiMap headers = request.headers();
        int status = request.response().getStatusCode();
        String message = null;

        switch (format) {
            case DEFAULT:
                // as per RFC1945 the header is referer, but it is not mandatory some implementations use referrer
                String referrer = headers.contains("referrer") ? headers.get("referrer") : headers.get("referer");
                String userAgent = request.headers().get("user-agent");
                referrer = referrer == null ? "-" : referrer;
                userAgent = userAgent == null ? "-" : userAgent;
                String xForwardedFor = headers.get("X-Forwarded-For") == null ? "-" : headers.get("X-Forwarded-For");
                message = String.format("%s - - [%s] \"%s %s %s\" %d %d \"%s\" \"%s\" \"%s\"",
                        remoteClient,
                        Utils.formatRFC1123DateTime(timestamp),
                        method,
                        uri,
                        versionFormatted,
                        status,
                        contentLength,
                        referrer,
                        userAgent,
                        xForwardedFor);
                break;
            case SHORT:
                message = String.format("%s - %s %s %s %d %d - %d ms",
                        remoteClient,
                        method,
                        uri,
                        versionFormatted,
                        status,
                        contentLength,
                        (System.currentTimeMillis() - timestamp));
                break;
            case TINY:
                message = String.format("%s %s %d %d - %d ms",
                        method,
                        uri,
                        status,
                        contentLength,
                        (System.currentTimeMillis() - timestamp));
                break;
        }
        doLog(status, message);
    }

    protected void doLog(int status, String message) {
        if (status >= 500) {
            log4jLogger.severe(message);
        } else if (status >= 400) {
            log4jLogger.warning(message);
        } else {
            log4jLogger.info(message);
        }
    }

    @Override
    public void handle(RoutingContext context) {
        // common logging data
        long timestamp = System.currentTimeMillis();
        String remoteClient = HttpRequestUtil.getRealRemoteClientIp(context.request()).orElse("");
        HttpMethod method = context.request().method();
        String uri = context.request().uri();
        HttpVersion version = context.request().version();

        if (immediate) {
            log(context, timestamp, remoteClient, version, method, uri);
        } else {
            context.addBodyEndHandler(v -> log(context, timestamp, remoteClient, version, method, uri));
        }

        context.next();

    }

    @Override
    public LoggerHandler customFormatter(Function<HttpServerRequest, String> function) {
        return null;
    }

    @Override
    public LoggerHandler customFormatter(LoggerFormatter formatter) {
        return null;
    }
}
