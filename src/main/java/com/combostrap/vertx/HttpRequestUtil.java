package com.combostrap.vertx;

import com.combostrap.exception.InternalException;
import com.combostrap.type.UriCastException;
import com.combostrap.type.UriEnhanced;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HttpRequestUtil {


    public static final String localHostIpv6 = "0:0:0:0:0:0:0:1";
    public static final String localhostIpv4 = "127.0.0.1";
    public static final String LOCALHOST = "localhost";


    public static boolean isLocalhostRequest(RoutingContext routingContext) {
        SocketAddress clientAddress = routingContext.request().remoteAddress();
        String hostSocketAddress = clientAddress.host();
        boolean isIpv4Localhost = hostSocketAddress.equals(localhostIpv4);
        boolean isIpv6Localhost = hostSocketAddress.equals(localHostIpv6);
        return isIpv4Localhost || isIpv6Localhost;
    }


    public static String getRemoteScheme(RoutingContext routingContext) {
        /**
         * Behind a proxy
         * See also {@link HttpForwardProxy#addAllowForwardProxy(Router)}}
         */
        String scheme = routingContext.request().getHeader(HttpHeaders.X_FORWARDED_PROTO);
        if (scheme != null) {
            return scheme;
        }
        return routingContext.request().scheme();
    }

    public static Map<String, String> paramsToMap(MultiMap params) {
        HashMap<String, String> paramsSingleValue = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entries()) {
            paramsSingleValue.put(entry.getKey(), entry.getValue());
        }
        return paramsSingleValue;
    }


    /**
     * @param routingContext - the routing context
     * @return the host that should be used in external context, such as email
     */
    public static String getRemoteHost(RoutingContext routingContext) {
        /**
         * Behind a proxy
         * See also {@link HttpForwardProxy#addAllowForwardProxy(Router)}}
         */
        String xForwardedHost = routingContext.request().getHeader(HttpHeaders.X_FORWARDED_HOST);
        if (xForwardedHost != null) {
            return xForwardedHost;
        }
        return routingContext.request().authority().toString();
    }

    /**
     * @param xForwardedFor - The content of a X-Forwarded-For header
     * @return the remote ip (not the proxy ip)
     * See also {@link HttpForwardProxy#addAllowForwardProxy(Router)}}
     */
    public static String getRemoteIpFromXForwardedFor(String xForwardedFor) {
        assert xForwardedFor != null;
        String[] xForwardedForParts = xForwardedFor.split(",");
        return xForwardedForParts[0];
    }

    /**
     * @return the IP address of the real client (browser), not the proxy
     * <p>
     * See also {@link HttpForwardProxy#addAllowForwardProxy(Router)}}
     * null when the ip was not found
     */
    public static Optional<String> getRealRemoteClientIp(HttpServerRequest request) {

        final MultiMap headers = request.headers();

        String xForwardedFor = headers.get(HttpHeaders.X_FORWARDED_FOR);
        if (xForwardedFor != null) {
            return Optional.of(getRemoteIpFromXForwardedFor(xForwardedFor));
        }
        String xRealIP = headers.get(HttpHeaders.X_REAL_IP);
        if (xRealIP != null) {
            return Optional.of(xRealIP);
        }

        // remoteClient is the ip of the direct connection (ie a proxy or the real client)
        SocketAddress inetSocketAddress = request.remoteAddress();
        if (inetSocketAddress != null) {
            return Optional.of(inetSocketAddress.host());
        }

        return Optional.empty();

    }

    /**
     * @param routingContext - the routing context
     * @return the base uri of the server (ie scheme+host) that may be used in email, redirect, ...
     */
    public static UriEnhanced geRemoteBaseUri(RoutingContext routingContext) {

        String scheme = getRemoteScheme(routingContext);
        String host = getRemoteHost(routingContext);
        try {
            return UriEnhanced.create()
                    .setScheme(scheme)
                    .setHost(host);
        } catch (UriCastException e) {
            throw new InternalException(e);
        }
    }
}
