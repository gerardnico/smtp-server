package com.combostrap.smtp;

import jakarta.validation.constraints.Null;

public class SmtpUserConf {

    @Null
    public SmtpMailBoxConf mailbox;

    public String password;
}
