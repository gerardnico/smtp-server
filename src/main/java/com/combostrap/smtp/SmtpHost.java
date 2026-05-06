package com.combostrap.smtp;

import com.combostrap.email.BMailMimeMessageHeader;
import com.combostrap.smtp.command.SmtpHeloCommandHandler;
import com.combostrap.smtp.exceptions.ConfigIllegalException;
import jakarta.mail.internet.AddressException;

/**
 * A class that represents a domain with its data (hostname, postmaster)
 */
public class SmtpHost {

    /**
     * The server name (used to sign
     * and add trace information such as the {@link BMailMimeMessageHeader#RECEIVED}
     * header
     * The hostname that reaches this server
     * It's advertised in the {@link SmtpHeloCommandHandler}
     */
    private final String hostedHostName;

    private final SmtpPostMaster postmaster;
    private final SmtpHostConfig conf;

    private final SmtpDomain smtpDomain;


    public SmtpHost(String hostedHostName, SmtpDomain smtpDomain, SmtpHostConfig conf) throws ConfigIllegalException {
        this.hostedHostName = hostedHostName;
        this.conf = conf;
        try {
            postmaster = SmtpPostMaster.create(this, conf.postmaster);
        } catch (AddressException e) {
            throw new ConfigIllegalException("The postmaster email configuration for the host (" + this.hostedHostName + ") has a email value (" + conf.postmaster + ") that is not valid", e);
        }
        this.smtpDomain = smtpDomain;

    }


    @Override
    public String toString() {
        return this.hostedHostName;
    }


    public SmtpDomain getDomain() {
        return smtpDomain;
    }


    public String getHostedHostname() {
        return this.hostedHostName;
    }

    public SmtpPostMaster getPostmaster() {
        return postmaster;
    }

    public String getPrivateKeyPath() {
        return this.conf.keyPath;
    }

    public String getCertificatePath() {
        return this.conf.certificatePath;
    }


}
