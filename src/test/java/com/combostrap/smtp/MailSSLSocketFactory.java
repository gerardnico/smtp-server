package com.combostrap.smtp;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

/**
 * When mail.smtp.ssl.socketFactory is set with this class
 * <a href="https://jakarta.ee/specifications/mail/1.6/apidocs/com/sun/mail/smtp/package-summary">...</a>
 * This class is used to create SMTP SSL sockets.
 * <a href="https://www.oracle.com/java/technologies/javamail-sslnotes.html">...</a>
 * !!!!!!!!!!!!!
 * Should not be a SSLSocketFactory. See <a href="https://github.com/eclipse-ee4j/angus-mail/blob/eca8e08baa56a83ab68ae3faaca25d8ca3c0e5ee/core/src/main/java/org/eclipse/angus/mail/util/SocketFetcher.java#L328">...</a>
 * !!!!!!!!!!!!!
 */
public class MailSSLSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory delegate;
    private final String sniHostname;


    public MailSSLSocketFactory(String sniHostname) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, null, null); // use default trust/key managers
        this.delegate = ctx.getSocketFactory();
        this.sniHostname = sniHostname;
    }

    private Socket applySNI(Socket socket) {
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            SSLParameters params = sslSocket.getSSLParameters();
            params.setServerNames(List.of(new SNIHostName(sniHostname)));
            sslSocket.setSSLParameters(params);
        }
        return socket;
    }

    @Override
    public Socket createSocket() throws IOException {
        return applySNI(delegate.createSocket());
    }


    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return applySNI(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return applySNI(delegate.createSocket(host, port));
    }


    // ... implement remaining abstract methods by delegating
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return applySNI(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return applySNI(delegate.createSocket(address, port, localAddress, localPort));
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return applySNI(delegate.createSocket(s, host, port, autoClose));
    }
}
