package com.combostrap.smtp.sasl;

import java.lang.reflect.InvocationTargetException;

/**
 * List of authentication mechanism
 * Simple Authentication and Security Layer (SASL)
 *<a href="https://www.iana.org/assignments/sasl-mechanisms/sasl-mechanisms.xhtml">...</a>
 */
public enum SimpleAuthMechanism {

  /**
   * LOGIN Obsolete:
   * <a href="https://datatracker.ietf.org/doc/html/draft-murchison-sasl-login-00">...</a>
   * Clients SHOULD implement the PLAIN SASL mechanism and use it whenever
   * offered by a server. The LOGIN SASL mechanism SHOULD NOT be used by
   * a client when other plaintext mechanisms are offered by a server
   * <p>
   * Funny enough: The java mail client supports the following and choose LOGIN as first one:
   *  * DEBUG SMTP: Attempt to authenticate using mechanisms: LOGIN PLAIN DIGEST-MD5 NTLM XOAUTH2
   *  * DEBUG SMTP: mechanism LOGIN not supported by server
   *  * DEBUG SMTP: Using mechanism PLAIN
   */
  LOGIN(SimpleAuthNoSupported.class),
  /**
   *<a href="https://www.rfc-editor.org/rfc/rfc4616.html">...</a>
   */
  PLAIN(SimpleAuthPlain.class);

  private final SimpleAuth authHandler;

  SimpleAuthMechanism(Class<? extends SimpleAuth> authHandler) {

    try {
      this.authHandler = authHandler.getDeclaredConstructor(SimpleAuthMechanism.class).newInstance(this);
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public SimpleAuth getHandler() {
    return this.authHandler;
  }


  public boolean isImplemented() {
    return this.authHandler.isImplemented();
  }

}
