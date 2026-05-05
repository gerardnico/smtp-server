package com.combostrap.email;

public enum BMailMimeMessageHeader {

  /**
   * Trace info: The date when the message was received by the SMTP server
   * and by who.
   * <a href="https://www.rfc-editor.org/rfc/rfc822.html#section-4.3.2">...</a>
   */
  RECEIVED("Received"),
  /**
   * Trace info: Where to send back the email in case of problem.
   * Email header that indicates where and how bounced emails will be processed.
   * <a href="https://www.rfc-editor.org/rfc/rfc1123">...</a>
   * This header is also referred to as a bounce address or reverse path
   * <p>
   * Diff with Reply-To:
   * The "Reply-To" field is added  by  the  originator  and
   * serves  to  direct  replies,  whereas the "Return-Path"
   * field is used to identify a path back to  the  originator
   * in case of bounce
   * <a href="https://www.rfc-editor.org/rfc/rfc822.html#section-4.3.1">...</a>
   */
  RETURN_PATH("Return-Path"),
  /**
   * The "Reply-To" field is added  by  the  originator  and
   * serves  to  direct  replies,  whereas the "Return-Path"
   * field is used to identify a path back to  the  originator
   * in case of bounce
   */
  REPLY_TO("Reply-To"),
  /**
   * Errors-To: Address to which notifications
   * Not internet standard, Non-standard, discouraged
   * <a href="https://www.rfc-editor.org/rfc/rfc2076.html#section-3.4">...</a>
   */
  ERRORS_TO("Errors-To"),
  /**
   * see <a href="https://policy.hubspot.com/abuse-complaints">...</a>
   */
  X_REPORT_ABUSE_TO("X-Report-Abuse-To"),
  /**
   * Example
   * X-Mailer: Customer.io (dgTQ1wUDAN7xBd3xBQGGMVPhnYtKqXGPZwNilOc=; +https://xxxx.com)
   */
  @SuppressWarnings("JavadocLinkAsPlainText")
  X_MAILER("X-Mailer");


  private final String name;

  BMailMimeMessageHeader(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
