package com.combostrap.smtp;

import com.combostrap.smtp.exceptions.ConfigIllegalException;
import com.combostrap.type.DnsName;
import jakarta.mail.internet.AddressException;

/**
 * A class that represents a domain with its data (hostname, postmaster)
 */
public class SmtpMxHost {

    private final SmtpPostMaster postmaster;
    private final SmtpHostConfig conf;

    private final SmtpDomain smtpDomain;

    private final DnsName virtualHostDnsName;


    /**
     *
     * @param hostDnsName      - The name in the mx
     * @param hostedMailDomain - The domain name in the email
     * @param conf             -  the conf
     * @throws ConfigIllegalException - if any conf error
     */
    public SmtpMxHost(DnsName hostDnsName, SmtpDomain hostedMailDomain, SmtpHostConfig conf) throws ConfigIllegalException {

        /**
         * The name in the mx
         */
        this.virtualHostDnsName = hostDnsName;

        /**
         * The domain name in the email
         */
        this.smtpDomain = hostedMailDomain;

        this.conf = conf;
        try {
            postmaster = SmtpPostMaster.create(this, conf.postmaster);
        } catch (AddressException e) {
            throw new ConfigIllegalException("The postmaster email configuration for the host (" + hostedMailDomain + ") has a email value (" + conf.postmaster + ") that is not valid", e);
        }


    }


    @Override
    public String toString() {
        return this.virtualHostDnsName.toString();
    }


    public SmtpDomain getDomain() {
        return smtpDomain;
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
