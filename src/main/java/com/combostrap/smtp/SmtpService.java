package com.combostrap.smtp;

import com.combostrap.vertx.ConfigAccessor;
import io.vertx.core.net.NetSocket;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.Certificate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <a href="https://www.rfc-editor.org/rfc/rfc5321.html">Simple Mail Transfer Protocol</a>
 * implementation
 * This is the last specification of 2008
 * <p>
 * You can confirm it here
 * <a href="https://www.rfc-editor.org/search/rfc_search_detail.php?sortkey=Date&sorting=DESC&page=25&title=smtp&pubstatus[]=Any&pub_date_type=any">Rfc Search</a>
 */
public class SmtpService {


    /**
     * The port used in test
     */
    public static final Integer PORT_2525 = 2525;

    /**
     * TLS Port
     */
    protected static final int PORT_587 = 465;


    private static final Logger LOGGER = Logger.getLogger(SmtpService.class.getName());

    private final boolean startTlsEnabled;
    private final boolean startTlsRequired;

    private final Boolean enableChunking;
    /**
     * Enable or disable ({@link SmtpPipelining})
     */
    private final Boolean enablePipelining;
    /**
     * Enable or disable {@link SmtpInputType#BINARYMIME}
     * Binary Mime is never supported, nor implemented in client library
     * But there is some code around that we kept for now
     */
    private final boolean enableBinaryMime;
    private final SmtpServer smtpServer;
    private final Boolean requireValidPeerCertificate;
    /**
     * Authentication is required before any command
     */
    private final Boolean authRequired;
    /**
     * Is AUTH enabled
     */
    private final boolean authEnabled;

    private final Boolean tlsEnabled;
    private final Boolean sniEnabled;
    private final int listeningPort;
    private final Boolean enableSpf;
    private final boolean enableDkim;


    public SmtpService(SmtpServer smtpServer, Integer port, ConfigAccessor configAccessor) {

        /**
         * Service settings
         */
        LOGGER.info("Service Settings:");
        this.listeningPort = port;
        LOGGER.info(SmtpSyntax.LOG_TAB + "Listening Port set to " + this.listeningPort);

        /**
         * Save the server
         */
        this.smtpServer = smtpServer;

        /**
         * Configuration: SSL and StartTLS
         */
        this.tlsEnabled = configAccessor.getBoolean("tls.enabled", false);
        LOGGER.info(SmtpSyntax.LOG_TAB + "TLS is " + (this.tlsEnabled ? "enabled" : "disabled"));
        this.sniEnabled = configAccessor.getBoolean("sni.enabled", true);
        LOGGER.info(SmtpSyntax.LOG_TAB + "SNI is " + (this.sniEnabled ? "enabled" : "disabled"));
        if (!this.tlsEnabled) {
            this.startTlsEnabled = configAccessor.getBoolean("starttls.enabled", true);
            this.startTlsRequired = configAccessor.getBoolean("starttls.required", false);
        } else {
            this.startTlsEnabled = false;
            this.startTlsRequired = false;
        }
        LOGGER.info(SmtpSyntax.LOG_TAB + "StartTLS is " + (this.startTlsEnabled ? "enabled" : "disabled"));
        LOGGER.info(SmtpSyntax.LOG_TAB + "StartTLs Required is " + (this.startTlsRequired ? "enabled" : "disabled"));
        this.requireValidPeerCertificate = configAccessor.getBoolean("requireValidPeerCertificate", false);
        LOGGER.info(SmtpSyntax.LOG_TAB + "Required Valid Peer certificate is " + (this.requireValidPeerCertificate ? "enabled" : "disabled"));


        /**
         * Smtp Extension Settings
         */
        LOGGER.info(SmtpSyntax.LOG_TAB + "Smtp Extension Settings:");
        this.enableChunking = configAccessor.getBoolean("chunking.enabled", true);
        LOGGER.info(SmtpSyntax.LOG_TAB + "Chunking is " + (this.enableChunking ? "enabled" : "disabled"));
        /**
         * {@link SmtpInputType#BINARYMIME} is not supported
         * because we can't test it,
         * the default java client does not support it
         * Google does not support it either
         */
        this.enableBinaryMime = false;
        /**
         * Pipelining
         */
        this.enablePipelining = configAccessor.getBoolean("pipelining.enabled", true);
        LOGGER.info(SmtpSyntax.LOG_TAB + "Pipelining is " + (this.enablePipelining ? "enabled" : "disabled"));


        /**
         * Auth Settings
         */
        this.authEnabled = configAccessor.getBoolean("auth.enabled", true);
        LOGGER.info(SmtpSyntax.LOG_TAB + "Authentication is " + (this.authEnabled ? "enabled" : "disabled"));
        this.authRequired = configAccessor.getBoolean("auth.required", false);
        LOGGER.info(SmtpSyntax.LOG_TAB + "Authentication is " + (this.authRequired ? "required" : "optional"));
        /**
         * The Spf and dkim features are not yet implemented
         * They are then not enabled
         */
        this.enableSpf = configAccessor.getBoolean("spf.enabled", false);
        LOGGER.info(SmtpSyntax.LOG_TAB + "Spf is " + (this.enableSpf ? "enabled" : "disabled"));
        this.enableDkim = configAccessor.getBoolean("dkim.enabled", false);
        LOGGER.info(SmtpSyntax.LOG_TAB + "Dkim is " + (this.enableDkim ? "enabled" : "disabled"));


    }


    public void handle(NetSocket netSocket) {


        /**
         * SSL/Certification verification
         */
        if (netSocket.isSsl()) {
            /**
             * A SSL session is available at {@link NetSocket#sslSession()}
             */
            try {
                List<Certificate> peerCerts = netSocket.peerCertificates();
                LOGGER.fine("Peer Certificates: " + peerCerts.size());
            } catch (SSLPeerUnverifiedException e) {
                if (this.requireValidPeerCertificate) {
                    String message = "Error unverified peer certificates";
                    LOGGER.log(Level.SEVERE, message, e);
                    netSocket.write(message);
                    netSocket.close();
                    return;
                }
            }
        }

        /**
         * Server name
         */
        LOGGER.fine("Server name broadcast: " + netSocket.indicatedServerName());

        /**
         * Socket creation
         * (Just a wrapper to be sure of what a remote address is)
         */
        SmtpSocket smtpSocket = new SmtpSocket(netSocket);


        /**
         * A session
         * The unit of connection that wraps a socket
         */
        SmtpSession smtpSession = SmtpSession
                .create(this, smtpSocket);

        /**
         * Connection/Session Rate Limiter
         * The rate limiter is here even if it's normally
         * part of a session because the array of connection
         * is here
         */
        try {
            this.smtpServer.connectionRateLimiter(smtpSession);
        } catch (SmtpException e) {
            e.setShouldQuit(true);
            // Exception handling is on the session
            // because the exceptions are
            // by session. This is just a short session.
            smtpSession.handleException(e);
            return;
        }

        /**
         * Start handling
         */
        smtpSession.start();

    }

    public boolean isStartTlsEnabled() {
        return this.startTlsEnabled;
    }

    public boolean isStartTlsRequired() {
        return this.startTlsRequired;
    }


    public boolean isChunkingEnabled() {
        return this.enableChunking;
    }

    public boolean isBinaryMimeEnabled() {
        return this.enableBinaryMime;
    }

    public boolean isPipeliningEnabled() {
        return this.enablePipelining;
    }


    public SmtpServer getSmtpServer() {
        return this.smtpServer;
    }

    public boolean isAuthRequired() {
        return this.authRequired;
    }

    public boolean isAuthEnabled() {
        return this.authEnabled;
    }


    /**
     * By default, this is only TLS
     * SSL is too old, we don't use
     */
    public boolean getIsTlsEnabled() {
        return this.tlsEnabled;
    }

    public boolean getIsSniEnabled() {
        return this.sniEnabled;
    }

    public int getListeningPort() {
        return this.listeningPort;
    }

    @Override
    public String toString() {
        return String.valueOf(listeningPort);
    }


    public boolean isSpfEnabled() {
        return this.enableSpf;
    }

    public boolean isDkimEnabled() {
        return this.enableDkim;
    }


}
