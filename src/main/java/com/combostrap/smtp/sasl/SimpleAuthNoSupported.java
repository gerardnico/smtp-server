package com.combostrap.smtp.sasl;

import com.combostrap.smtp.SmtpDomain;
import com.combostrap.smtp.SmtpUser;

public class SimpleAuthNoSupported extends SimpleAuth {
  public SimpleAuthNoSupported(SimpleAuthMechanism simpleAuthMechanism) {
    super(simpleAuthMechanism);
  }

  @Override
  public SmtpUser authenticate(SmtpDomain smtpDomain, String credential) throws SimpleAuthException {
    throw new SimpleAuthException("Not supported");
  }

  @Override
  public boolean isImplemented() {
    return false;
  }

}
