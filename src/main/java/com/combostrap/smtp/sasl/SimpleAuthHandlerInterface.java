package com.combostrap.smtp.sasl;

import com.combostrap.smtp.SmtpDomain;
import com.combostrap.smtp.SmtpUser;

public interface SimpleAuthHandlerInterface {

  SmtpUser authenticate(SmtpDomain smtpDomain, String credential) throws SimpleAuthException;

  boolean isImplemented();

}
