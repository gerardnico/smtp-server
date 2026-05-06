package com.combostrap.smtp;

import com.combostrap.common.JavaEnvs;
import com.combostrap.type.DnsCastException;
import com.combostrap.type.DnsName;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;


import java.util.Optional;

/**
 * A class to greet and hold connection information
 * <a href="https://datatracker.ietf.org/doc/html/rfc5321#section-3.1">Session Initiation</a>
 * <a href="https://datatracker.ietf.org/doc/html/rfc5321#section-4.3.1">Sequencing Overview</a>
 */
public class SmtpGreeting implements SmtpSessionInteraction {


    private final SmtpSession smtpSession;
    private String sslProtocol;
    private SmtpMxHost requestedHost;
    private SocketAddress clientAddress;

    public SmtpGreeting(SmtpSession smtpSession) {
        this.smtpSession = smtpSession;
    }

    public static SmtpGreeting create(SmtpSession smtpSession) {
        return new SmtpGreeting(smtpSession);
    }

    public void updateRequestedHost() throws SmtpException {

        this.requestedHost = this.determineRequestedHost().orElse(null);

        if (this.requestedHost == null) {
            if (this.smtpSession.getSmtpSocket().getNetSocket().isSsl()) {
                throw SmtpException.create(SmtpReplyCode.SSL_REQUIRED_538, "SNI is required with SSL. We couldn't find the hostname");
            }
        }

    }

    /**
     * Determine the Requested host
     * <p>
     */
    private Optional<SmtpMxHost> determineRequestedHost() throws SmtpException {


        String indicatedServerName = this.smtpSession.getSmtpSocket().getIndicatedServerName().orElse(null);
        if (indicatedServerName == null) {
            return Optional.empty();
        }

        DnsName sniAsDnsName;
        try {
            sniAsDnsName = DnsName.create(indicatedServerName);
        } catch (DnsCastException e) {
            throw new RuntimeException("The SNI found on the connection (" + indicatedServerName + ") is not a valid DNS Name. Error:" + e.getMessage(), e);
        }

        SmtpMxHost smtpMxHost = this.smtpSession.getSmtpService().getSmtpServer().getHostedHosts().get(sniAsDnsName);
        if (smtpMxHost == null) {
            SmtpException smtpException = SmtpException.create(SmtpReplyCode.CONNECTION_WITH_BAD_HOSTNAME_899, "The requested hostname (" + indicatedServerName + ") is unknown")
                    .setShouldQuit(true);
            if (!JavaEnvs.isDev()) {
                smtpException.setShouldBeSilentQuit(true);
            }
            throw smtpException;
        }
        return Optional.of(smtpMxHost);

    }


    /**
     * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-3.1">...</a>
     * We may answer with:
     * * {@link SmtpReplyCode#GREETING_220}
     * * {@link SmtpReplyCode#TRANSACTION_FAILED_554}
     */
    public SmtpReply greet() throws SmtpException {

        /**
         * Check IP
         */
        SmtpFiltering.checkIp(this.smtpSession);


        /**
         * Determine, set the requested host
         */
        this.updateRequestedHost();


        SmtpSocket smtpSocket = this.smtpSession.getSmtpSocket();
        NetSocket netSocket = smtpSocket.getNetSocket();
        if (!netSocket.isSsl()) {
            this.sslProtocol = "plain";
        } else {
            this.sslProtocol = netSocket.sslSession().getProtocol();
        }
        this.clientAddress = smtpSocket.getRemoteAddress();


        this.smtpSession.getSessionHistory().addInteraction(this);

        /**
         * Greetings
         */
        return SmtpReply.create(
                SmtpReplyCode.GREETING_220,
                this.getRequestedHostOrDefault().getDomain() + " " +
                        this.smtpSession.getSmtpProtocol() + " " +
                        this.smtpSession.getSmtpService().getSmtpServer().getSoftwareName()
        );
    }

    @Override
    public String getSessionHistoryLine() {
        return "Connection to " + this.getRequestedHostOrDefault().getDomain() + " in " + this.sslProtocol + " with " + this.clientAddress + SmtpSyntax.LINE_DELIMITER;
    }

    /**
     * Without TLS, we don't know the server requested
     * This function return:
     * * the default hostname if we don't know the requested hostname
     * * the domain if we know the requested hostname
     * Why?
     * In the documentation, the hostname of the server
     * is given in the greeting and the domain is given afterward.
     * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.1.1.1">Domain in EHLO</a>
     * and Domain in QUIT
     * <p>
     * Hosting: in case you host multiple, it's completely futile.
     * You can't advertise more than one domain or hosts.
     * <p>
     * Example: With Smtp In Server from SES
     * * the hosted domain is: inbox.mailbrew.com
     * * the MX is: inbound-smtp.us-east-1.amazonaws.com
     * On this MX, a connection gives:
     * 250-inbound-smtp.us-east-1.amazonaws.com
     * 250-8BITMIME
     * 250-STARTTLS
     * 250 Ok
     */
    public SmtpMxHost getRequestedHostOrDefault() {

        SmtpMxHost requestHost = getRequestedHost().orElse(null);
        if (requestHost != null) {
            return requestHost;
        }
        return this.smtpSession.getSmtpService().getSmtpServer().getDefaultHostedHost();

    }

    /**
     * Without TLS, we don't know the server requested
     */
    public Optional<SmtpMxHost> getRequestedHost() {
        if (this.requestedHost == null) {
            return Optional.empty();
        }
        return Optional.of(this.requestedHost);
    }


}
