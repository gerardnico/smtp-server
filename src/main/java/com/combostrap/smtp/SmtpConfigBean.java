package com.combostrap.smtp;

import com.combostrap.smtp.command.SmtpEhloCommandHandler;
import com.combostrap.smtp.command.SmtpHeloCommandHandler;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class SmtpConfigBean {

    /**
     * The software name is given in the {@link SmtpEhloCommandHandler Ehlo command}
     */
    @JsonProperty("softwareName")
    public String softwareName = "Smtp Server";


    public boolean sessionReplayEnabled = false;

    /**
     * Authentication from Localhost is not required by default
     */
    public boolean authLocalhostRequired = false;

    /**
     * Listen from all hostname
     * On ipv4 and Ipv6.
     * The wildcard implementation depends on the language
     * and in Java it works for the 2 Ip formats.
     */
    public static final String WILDCARD_IPV4_ADDRESS = "0.0.0.0";

    @SuppressWarnings("unused")
    public static final String WILDCARD_IPV6_ADDRESS = "[::]";

    /**
     * Listening Host
     */
    @NotNull(message = "Host configuration is mandatory")
    public String listeningHost = WILDCARD_IPV4_ADDRESS;

    /**
     * Port of the HTTP server
     */
    public Integer httpPort = 25026;

    /**
     * The port of the proxy
     */
    public Integer publicHttpPort = 80;

    /**
     * The actual hostname
     * (chosen when the connection is not secured (no TLS) as we don't have the requested host)
     */
    @NotNull
    public String host;

    /**
     * The key is the hostname that reaches this server
     * It's advertised in the {@link SmtpHeloCommandHandler}
     */
    @NotEmpty(message = "Hosts configuration (virtual hosts) is mandatory and was empty")
    @NotNull
    public Map<String, SmtpHostConfig> hosts;

    @NotEmpty(message = "The services configuration is mandatory and was not found")
    @NotNull
    public Map<Integer, SmtpServiceConfig> services;

    public SmtpReceptionConf reception;

    public SmtpDeliveryConf delivery;

    public SmtpLimitsConf limits;

    /**
     * Key:
     * > domain: Users are defined by domain
     * > Then the name
     */
    @NotEmpty(message = "The users configuration is mandatory and was not found")
    public Map<String, Map<String, SmtpUserConf>> users;

    /**
     * The path to the SSL Key
     */
    public String keyPath = "ssl/key.pem";

    /**
     * The path to the SSL Cert
     */
    public String certificatePath = "ssl/cert.pem";

    /**
     * Path where the uploaded temporary artefact  of the HTTP server are stored
     * (Body Handler)
     */
    public String httpUploadDir;
}