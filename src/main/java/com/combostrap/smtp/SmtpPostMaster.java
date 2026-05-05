package com.combostrap.smtp;

import com.combostrap.email.BMailInternetAddress;
import jakarta.mail.internet.AddressException;

/**
 * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.5.1">...</a>
 * SMTP systems are expected to make every reasonable effort to accept
 * mail directed to Postmaster from any other system on the Internet.
 * In extreme cases --such as to contain a denial of service attack or
 * other breach of security-- an SMTP server may block mail directed to
 * Postmaster.  However, such arrangements SHOULD be narrowly tailored
 * so as, to avoid blocking messages which are not part of such attacks.
 * <p>
 * See also: <a href="https://www.postfix.org/BASIC_CONFIGURATION_README.html#notify">What trouble to report to the postmaster</a>
 */
public class SmtpPostMaster {


  static final String POSTMASTER = "postmaster";
  private final BMailInternetAddress postMasterEmailInConfiguration;
  private final BMailInternetAddress postMasterAtAddress;

  public SmtpPostMaster(SmtpHost smtpHost, String postMasterEmailInConfiguration) throws AddressException {
    this.postMasterEmailInConfiguration = BMailInternetAddress.of(postMasterEmailInConfiguration);
    this.postMasterAtAddress = BMailInternetAddress.of(POSTMASTER + "@" + smtpHost.getDomain());
  }

  public static SmtpPostMaster create(SmtpHost smtpHost, String postMasterEmailConf) throws AddressException {
    return new SmtpPostMaster(smtpHost, postMasterEmailConf);
  }

  public boolean isPostMasterPath(String forwardPathString) {
    return forwardPathString.equalsIgnoreCase(POSTMASTER)
      || forwardPathString.equalsIgnoreCase(this.postMasterAtAddress.getAddress());
  }

  public BMailInternetAddress getPostmasterAddressInConfiguration() {
    return this.postMasterEmailInConfiguration;
  }

  public BMailInternetAddress getPostmasterAddress() {
    return this.postMasterAtAddress;
  }

  @Override
  public String toString() {
    return this.postMasterAtAddress + "( or" + postMasterEmailInConfiguration + ")";
  }
}
