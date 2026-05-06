package com.combostrap.smtp;

import io.vertx.core.net.SSLOptions;

public class SmtpLimitsConf {

    public int maxTotalConnections = 50;

    public int maxConnectionByIp = 3;

    /**
     * Not fewer than 100
     * <a href="https://datatracker.ietf.org/doc/html/rfc5321#section-4.5.3.1.8">Recipients Buffer limits</a>
     */
    public int maxRecipientsByEmail = 100;

    /**
     * The maximum number of exception by session
     * before we close the conversation
     */
    public int maximumExceptionCountBySession = 3;

    /**
     * Max Size of a Message
     */
    public int maxMessageSizeInBytes = 1048576;

    public long handShakeTimeoutSecond = SSLOptions.DEFAULT_SSL_HANDSHAKE_TIMEOUT;

    public int idleTimeoutSecond = 5 * 60;

}
