package com.combostrap.smtp;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class SmtpConfigBean {


    @JsonProperty("software.name")
    public String softwareName;

    @JsonProperty("auth.localhost.required")
    public boolean authLocalhostRequired = true; // default: true

    @JsonProperty("session.replay")
    public boolean sessionReplay = false;

    public Map<String, HostConfig> hosts;

    public static class HostConfig {

        @NotNull
        public String domain;

        @NotNull
        @Email
        public String postmaster;

        public String key;   // optional
        public String cert;  // optional
    }
}