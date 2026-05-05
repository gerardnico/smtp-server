package com.combostrap.smtp;

/**
 * A class to check the email authentication
 * of incoming email if the user is not authenticated
 */
public class SmtpDkim {

  public static void check(SmtpSession smtpSession) throws SmtpException {

    /**
     * No need to check the DKIM if already authenticated
     */
    if (smtpSession.isAuthenticatedOrIsLocalhost()) {
      return;
    }
    if (smtpSession.getSmtpService().isDkimEnabled()) {
      throw SmtpException.create(SmtpReplyCode.TRANSACTION_FAILED_554, "Dkim failed");
    }

  }

}
