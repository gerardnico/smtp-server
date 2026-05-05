package com.combostrap.smtp.command;

import com.combostrap.smtp.*;
import io.vertx.core.net.NetSocket;

import java.util.logging.Logger;


/**
 * This handler implements:
 * * the {@link SmtpCommand#STARTTLS command}
 * to allow the {@link SmtpExtensionParameter#STARTTLS}
 * <a href="https://datatracker.ietf.org/doc/html/rfc3207">Ref</a>
 */
public class SmtpStartTlsCommandHandler extends SmtpInputCommandDirectReplyHandler {

    public static Logger LOGGER = Logger.getLogger(SmtpStartTlsCommandHandler.class.getName());

    public SmtpStartTlsCommandHandler(SmtpCommand smtpCommand) {
        super(smtpCommand);
    }

    /**
     * Example of scenario:
     * S: 220 mail.example.org ESMTP service ready
     * C: EHLO client.example.org
     * S: 250-mail.example.org offers a warm hug of welcome
     * S: 250 STARTTLS
     * C: STARTTLS
     * S: 220 Go ahead
     * C: <starts TLS negotiation>
     * C & S: <negotiate a TLS session>
     * C & S: <check result of negotiation>
     * C: EHLO client.example.org
     */
    public SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException {

        /**
         * See the START TLS section
         */
        NetSocket netSocket = smtpInputContext.getSession().getSmtpSocket().getNetSocket();
        if (netSocket.isSsl()) {
            throw SmtpException.createForInternalException("The connection is already secured, no need for a STARTTLS");
        }

        /**
         * Note that we reply here
         * because we can't reply after Vertx upgrade.
         * Why?
         * Because Vertx don't use the STARTTls mechanism of Netty
         * and the upgrade just block any respone.
         * Netty doc
         * https://netty.io/4.0/api/io/netty/handler/ssl/SslHandler.html
         * <p>
         * We tried to set it with {@link SmtpSslOptionsWithStartTLS} with no success
         * as it's at the end wrapped.
         * ie with {@link io.vertx.core.net.impl.SSLHelper#resolveEngineOptions(SSLEngineOptions, boolean)}
         * that resolve the {@link io.vertx.core.net.SSLEngineOptions}
         * to the {@link io.vertx.core.net.JdkSSLEngineOptions}
         *
         * Ref doc : Possible replies to a STARTTLS
         * 220 Ready to start TLS
         * 501 Syntax error (no parameters allowed)
         * 454 TLS not available due to temporary reason
         * <p>
         * To make STARTTLS mandatory before any discussion
         * require that the client perform a TLS negotiation except for NOOP, EHLO, STARTTLS, or QUIT
         * with a:
         * 530 Must issue a STARTTLS command first
         */
        SmtpReply response = SmtpReply.create(SmtpReplyCode.GREETING_220, "Go Ahead");
        smtpInputContext.getSession().getSessionHistory().addInteraction(response);
        netSocket.write(response.getReplyLines());

        /**
         * The upgrade makes it a SSL connection
         * and block until there is an handshake
         * The future is the result of the handshake
         */
        netSocket.upgradeToSsl()
                .onComplete(asyncResult -> {
                    /**
                     * This handler is called when the socket has been upgraded.
                     * ie when the handshake has finished
                     * UpgradeToSSL just asd the SSL handler
                     */
                    SmtpSession session = smtpInputContext.getSession();
                    if (asyncResult.failed()) {
                        session.handleException(
                                SmtpException.createForInternalException("Upgrade to TLS failed", asyncResult.cause())
                                        .setShouldQuit(true)
                        );
                        return;
                    }
                    try {
                        /**
                         * Not greet in the SMTP protocol after StartTLS
                         * but as we are in TLS, we can update the requested host
                         */
                        session.getGreeting().updateRequestedHost();
                    } catch (SmtpException e) {
                        session.handleException(e);
                        return;
                    }
                    LOGGER.fine("SSL/TLS Upgraded");
                });

        /**
         * Hack due to the way Vertx upgrade a connection
         * we can't respond after for now
         */
        return null;

    }

}
