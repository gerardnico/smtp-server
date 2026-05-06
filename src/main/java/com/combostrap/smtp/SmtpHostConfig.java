package com.combostrap.smtp;

import com.combostrap.smtp.command.SmtpEhloCommandHandler;
import com.combostrap.smtp.command.SmtpHeloCommandHandler;
import com.combostrap.smtp.command.SmtpQuitCommandHandler;
import com.combostrap.smtp.command.SmtpRcptCommandHandler;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public class SmtpHostConfig {

    /**
     * Domain is advertised in
     * * {@link SmtpHeloCommandHandler}
     * * {@link SmtpEhloCommandHandler}
     * * {@link SmtpQuitCommandHandler}
     * and is used for the postmaster email (ie postmaster@domain)
     * that can be received in a {@link SmtpRcptCommandHandler}
     */
    @NotNull(message = "The domain for the smtp hostname is mandatory")
    public String domain;

    /**
     * The postmaster that should receive email
     * for any problem
     */
    @NotNull(message = "The postmaster for a smtp host is mandatory")
    @Email
    public String postmaster;

    public String keyPath;
    public String certificatePath;



}
