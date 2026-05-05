package com.combostrap.smtp;

import com.combostrap.email.BMailMimeMessageHeader;
import com.combostrap.smtp.command.SmtpEhloCommandHandler;
import com.combostrap.smtp.command.SmtpHeloCommandHandler;
import com.combostrap.smtp.command.SmtpQuitCommandHandler;
import com.combostrap.smtp.command.SmtpRcptCommandHandler;
import com.combostrap.smtp.exceptions.ConfigIllegalException;
import jakarta.mail.internet.AddressException;

/**
 * A class that represents a domain with its data (hostname, postmaster)
 */
public class SmtpHost {

  /**
   * Domain is advertised in
   * * {@link SmtpHeloCommandHandler}
   * * {@link SmtpEhloCommandHandler}
   * * {@link SmtpQuitCommandHandler}
   * and is used for the postmaster email (ie postmaster@domain)
   * that can be received in a {@link SmtpRcptCommandHandler}
   */
  private final SmtpDomain domain;

  /**
   * The server name (used to sign
   * and add trace information such as the {@link BMailMimeMessageHeader#RECEIVED}
   * header
   * The hostname that reaches this server
   * It's advertised in the {@link com.combostrap.smtp.command.SmtpHeloCommandHandler}
   */
  private final String hostedHostName;

  /**
   * The postmaster that should receive email
   * for any problem
   */
  private final SmtpPostMaster postmaster;
  private final SmtpHost.conf conf;

  public SmtpHost(conf conf) throws ConfigIllegalException {
    this.hostedHostName = conf.hostName.toLowerCase();
    this.domain = conf.hostedDomainName;
    this.conf = conf;
    try {
      postmaster = SmtpPostMaster.create(this, conf.postmaster);
    } catch (AddressException e) {
      throw new ConfigIllegalException("The postmaster email configuration for the host (" + this.hostedHostName + ") has a email value (" + conf.postmaster + ") that is not valid", e);
    }
  }

  public static SmtpHost.conf createOf(String cname) {
    return new SmtpHost.conf(cname);
  }

  @Override
  public String toString() {
    return hostedHostName;
  }


  public SmtpDomain getDomain() {
    return this.domain;
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

  public static class conf {
    private final String hostName;
    private SmtpDomain hostedDomainName;
    private String postmaster;
    private String keyPath;
    private String certificatePath;

    public conf(String hostName) {
      this.hostName = hostName;
    }

    public conf setHostedDomain(SmtpDomain hostedDomain) {
      this.hostedDomainName = hostedDomain;
      return this;
    }

    public conf setPostmasterEmail(String postMasterEmailConf) {
      this.postmaster = postMasterEmailConf;
      return this;
    }

    public SmtpHost build() throws ConfigIllegalException {
      return new SmtpHost(this);
    }

    /**
     * The private key for SSL
     */
    public conf setPrivateKeyPath(String keyPath) {
      this.keyPath = keyPath;
      return this;
    }

    public conf setCertificatePath(String certificatePath) {
      this.certificatePath = certificatePath;
      return this;
    }
  }
}
