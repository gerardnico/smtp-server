package com.combostrap.smtp;


import com.combostrap.type.Timestamp;

/**
 * A collection of constant used in
 * SMTP output
 */
public class SmtpSyntax {

  public static final String LOG_TAB = "  * ";

  /**
   * WS = White-space
   * FWS is used in all EBNF in SMTP
   */
  public static final String FWS = " ";
  /**
   * The end of a line in SMTP is the windows EOL
   */
  protected static final String LINE_DELIMITER = "\r\n";

  /**
   * A separator in name for parts
   * Used in id and file name
   * (Nothing to do with SMTP but with our syntax)
   */
  static final String PART_SEP = "!";

  /**
   * Local time with an offset used in the headers must have this format
   * ie Thu, 28 Sep 2023 21:34:37 -0700 (PDT)
   */
  static final String MAIL_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z (z)";

  public static String getCurrentDateInMailFormat() {
    return Timestamp.createFromNowLocalSystem().toString(MAIL_DATE_FORMAT);
  }
}
