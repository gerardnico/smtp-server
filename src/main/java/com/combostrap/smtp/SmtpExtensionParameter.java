package com.combostrap.smtp;

import com.combostrap.smtp.command.SmtpBdatCommandHandler;
import com.combostrap.smtp.command.SmtpMailCommandHandler;

/**
 * <a href="https://en.wikipedia.org/wiki/Simple_Mail_Transfer_Protocol#Extensions">...</a>
 */
public enum SmtpExtensionParameter {

  /**
   * <a href="https://datatracker.ietf.org/doc/html/rfc1870">...</a>
   * SIZE is an option of the {@link SmtpMailCommandHandler MailCommand}
   * that gives the size of the message.
   * The size is in octet
   */
  SIZE("SIZE"),


  /**
   * Indicate the type of the data
   * in the {@link SmtpMailCommandHandler}
   */
  BODY("BODY"),


  AUTH("AUTH"),

  /**
   * Indicate that {@link SmtpBdatCommandHandler}
   * is supported
   * <p>
   * BDAT is the binary alternative to the DATA command.
   * ie it will accept the sending of messages in chunks
   * with the BDAT command
   */
  CHUNKING("CHUNKING"),

  /**
   * Enable the {@link SmtpCommand#STARTTLS StartTLS command}
   */
  STARTTLS("STARTTLS"),

  /**
   * Delivery Status Notifications (DSNs).
   * <a href="https://datatracker.ietf.org/doc/html/rfc3464">...</a>
   * Notifications include:
   * * failed delivery,
   * * delayed delivery,
   * * successful delivery,
   * * or the gatewaying of a message into an environment that may not support DSNs.
   * Not supported by google ?
   * <p>
   * Works with notify?
   * RCPT TO: <nico@example.com> NOTIFY=success,failure
   */
  DSN("DSN"),

  /**
   * PIPELINE enables the client SMTP to transmit
   * groups of SMTP commands in batches without waiting for a response to
   * each individual command.
   * <p>
   * Example: <a href="https://datatracker.ietf.org/doc/html/rfc1830#section-5.2">Pipelining Binarymime</a>
   * Rfc: <a href="https://datatracker.ietf.org/doc/html/rfc2920">Rfc</a>
   */
  PIPELINING("PIPELINING"),

  /**
   * An SMTP server augments its responses with the enhanced
   * mail system {@link SmtpReplyCode status codes} defined in RFC 1893
   * <a href="https://datatracker.ietf.org/doc/html/rfc2034">...</a>
   * <p>
   * The Status codes consist of three numerical fields separated by ".": ie
   * status-code = class "." subject "." detail
   * where:
   * * class = "2"/"4"/"5": success, permanent, and transient error
   * * subject = 1 digit
   * * detail = 1 digit
   * See a list here:
   * <a href="https://datatracker.ietf.org/doc/html/rfc1893#section-8">List</a>
   */
  ENHANCEDSTATUSCODES("ENHANCEDSTATUSCODES"),

  /**
   * Accept UTF-8 string
   *<a href="https://datatracker.ietf.org/doc/html/rfc6531#section-3.2">...</a>
   */
  SMTPUTF8("SMTPUTF8"),

  /**
   * The RET option to the {@link SmtpMailCommandHandler MAIL command}.
   * Either FULL or HDRS.
   * In a mail session, the property is `mail.smtp.dsn.ret`
   */
  RET("RET"),

  /**
   * Argument from the {@link com.combostrap.smtp.command.SmtpRcptCommandHandler RCP command}
   * to get a deliver notification
   */
  NOTIFY("NOTIFY");


  private final String key;


  /**
   * @param key - the parameter key value
   */
  SmtpExtensionParameter(String key) {
    this.key = key;
  }

  @Override
  public String toString() {
    return key;
  }

}
