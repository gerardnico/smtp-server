package com.combostrap.smtp;

import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLSession;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A wrapper around a {@link NetSocket}
 * to extend
 */
public class SmtpSocket {

    private final NetSocket socket;

    private final Set<String> LOCALHOSTS = Set.of(
            "localhost", // name
            "127.0.0.1", // ipv4
            "0:0:0:0:0:0:0:1", // ipv6
            "0000:0000:0000:0000:0000:0000:0000:0001", // long ipv6
            "::1" // short ipv6
    );

    public SmtpSocket(NetSocket socketConnection) {

        this.socket = socketConnection;


    }


    /**
     * <a href="https://www.ietf.org/rfc/rfc821#section-4.1.1">section 4.1.1 of RFC 821</a>
     * The section 4.1.1 of RFC 821 states that disconnection
     * should only occur after a QUIT command is issued.
     * <p>
     * In the case of any error response,
     * the client SMTP should issue either the HELO or QUIT command.
     * <p>
     * See also: <a href="https://www.ietf.org/rfc/rfc1869.html#section-4.7">Responses from improperly implemented servers</a>
     */
    public void close() {
        socket.close();
    }


    public SocketAddress getRemoteAddress() {
        return this.socket.remoteAddress(true);
    }


    public void write(String string) {
        this.socket.write(string);
    }


    /**
     * Needed by the {@link io.vertx.core.parsetools.RecordParser} handler
     */
    public NetSocket getNetSocket() {
        return this.socket;
    }

    /**
     * The extended domain is the domain written in headers of message
     * for tracing info
     * EBNF where FWS = space
     * Extended-Domain = Domain /
     * ( Domain FWS "(" TCP-info ")" ) /
     * ( Address-literal FWS "(" TCP-info ")" )
     * And TCP info:
     * TCP-info = Address-literal / ( Domain FWS Address-literal )
     * ; Information derived by server from TCP connection
     * ; not client EHLO.
     */
    public String getSourceExtendedDomain() {
        return this.getRemoteAddress().host() + " (" + this.getRemoteAddress().hostAddress() + ")";
    }

    @Override
    public String toString() {
        return this.getRemoteAddress().host() + ":" + this.getRemoteAddress().port() + "!" + this.getLocalAddress().host() + ":" + this.getRemoteAddress().port();
    }


    public SocketAddress getLocalAddress() {
        return this.socket.localAddress(true);
    }

    public Optional<String> getIndicatedServerName() {

        if (!socket.isSsl()) {
            return Optional.empty();
        }

        /**
         * With {@link io.vertx.core.net.NetServerOptions#setSni(boolean)}
         * to true
         */
        String indicatedServerName = socket.indicatedServerName();
        if (indicatedServerName != null) {
            return Optional.of(indicatedServerName);
        }

        SSLSession sslSession = socket.sslSession();
        if (!(sslSession instanceof ExtendedSSLSession)) {
            /**
             * Without TLS, we don't know the server requested
             */
            return Optional.empty();
        }

        ExtendedSSLSession sslSessionExtended = (ExtendedSSLSession) sslSession;

        // localhost value with papercut: 0:0:0:0:0:0:0:1
        // 127.0.0.1
        // this.getSmtpSocket().socket.indicatedServerName()
        List<SNIServerName> serverNames = sslSessionExtended.getRequestedServerNames();
        if (serverNames == null || serverNames.isEmpty()) {
            return Optional.empty();
        }

        SNIServerName serverName = serverNames.get(0);
        if (!(serverName instanceof SNIHostName)) {
            return Optional.empty();
        }
        String asciiName = ((SNIHostName) serverName).getAsciiName();
        return Optional.of(asciiName);

    }

    public boolean isRemoteLocalhost() {
        return LOCALHOSTS.contains(this.getRemoteAddress().host());
    }

}
