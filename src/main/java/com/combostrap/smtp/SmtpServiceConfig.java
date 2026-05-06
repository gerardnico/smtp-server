package com.combostrap.smtp;

public class SmtpServiceConfig {

    public Boolean tlsEnabled=false;
    public Boolean sniEnabled=true;

    public boolean startTlsEnabled = true;
    public boolean startTlsRequired = false;
    public boolean requireValidPeerCertificate = false;

    // Smtp Extensions
    public boolean chunkingEnabled = true;
    /**
     * Enable or disable ({@link SmtpPipelining})
     */
    public boolean pipeliningEnabled = true;

    /**
     * Enabled, not mandatory / required
     * Is AUTH enabled
     */
    public boolean authEnabled = true;
    /**
     * Authentication is required before any command
     */
    public boolean authRequired = false;
}
