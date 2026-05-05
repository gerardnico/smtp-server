package com.combostrap.smtp;

import com.combostrap.email.BMailInternetAddress;
import com.combostrap.type.DnsName;

/**
 * SPF
 * It should be applied before the message body is transmitted, saving:
 * * bandwidth cost of downloading the message
 * * and the CPU cost of filtering it.
 * <a href="https://datatracker.ietf.org/doc/html/rfc4408">...</a>
 * <p>
 * See also: Mechanism:
 * <a href="https://github.com/apache/james-jspf/blob/master/resolver/src/main/java/org/apache/james/jspf/terms/AMechanism.java">...</a>
 * Parser:
 * <a href="https://github.com/apache/james-jspf/blob/master/resolver/src/main/java/org/apache/james/jspf/parser/RFC4408SPF1Parser.java">...</a>
 */
public class SmtpSpf {

  /**
   *
   * https://linux.die.net/man/1/spfquery from https://www.libspf2.org/
   * https://github.com/shevek/libspf2/
   * 15:30:04.33: ----------------------------------------------------------------
   * 15:30:04.33: SPFcheck_host called:
   * 15:30:04.33:       source ip = 192.168.1.3
   * 15:30:04.33:          domain = 11.spf1-test.siroe.com
   * 15:30:04.33:          sender = postmaster@11.spf1-test.siroe.com
   * 15:30:04.33:      local_part = postmaster
   * 15:30:04.33:     helo_domain = 11.spf1-test.siroe.com
   * 15:30:04.33:
   * 15:30:04.33:   Looking up "v=spf1" records for 11.spf1-test.siroe.com
   * 15:30:04.35:     DNS query status: Pass
   * 15:30:04.35:       "v=spf1 mx:spf1-test.siroe.com                  -all"
   * 15:30:04.35:
   * 15:30:04.35:   Parsing mechanism: " mx : spf1-test.siroe.com"
   * 15:30:04.35:     Assuming a Pass prefix
   * 15:30:04.35:     Processing macros in spf1-test.siroe.com
   * 15:30:04.35:     Comparing against 192.168.1.3
   * 15:30:04.35:     Looking for MX records for spf1-test.siroe.com
   * 15:30:04.41:       mx02.spf1-test.siroe.com:
   * 15:30:04.41:         192.0.2.22 - No match
   * 15:30:04.41:         192.0.2.21 - No match
   * 15:30:04.41:         192.0.2.20 - No match
   * 15:30:04.41:         192.0.2.23 - No match
   * 15:30:04.41:       mx01.spf1-test.siroe.com:
   * 15:30:04.42:         192.0.2.13 - No match
   * 15:30:04.42:         192.0.2.11 - No match
   * 15:30:04.42:         192.0.2.12 - No match
   * 15:30:04.42:         192.0.2.10 - No match
   * 15:30:04.42:       mx03.spf1-test.siroe.com:
   * 15:30:04.42:         192.0.2.32 - No match
   * 15:30:04.42:         192.0.2.30 - No match
   * 15:30:04.42:         192.0.2.31 - No match
   * 15:30:04.42:         192.168.1.3 - Matched
   * 15:30:04.42:   Mechanism matched; returning Pass
   * 15:30:04.42:
   * 15:30:04.42:   Parsing mechanism: "- all : " (not evaluated)
   */


  /**
   * <a href="https://datatracker.ietf.org/doc/html/rfc4408#section-2.4">...</a>
   * At least the "MAIL FROM" identity MUST be checked, but it
   * is RECOMMENDED that the "HELO" identity also be checked beforehand.
   * <p>
   * The check_host() function fetches SPF records, parses them, and
   * interprets them to determine whether a particular host is or is not
   * permitted to send mail with a given identity.
   */
  @SuppressWarnings("unused")
  public static void checkSenderHost(SmtpSession smtpSession, DnsName hostname, BMailInternetAddress sender) throws SmtpException {

    /**
     * The check is based on three parameters
     * @param smtpSocket - the IP address of the SMTP client that is emitting the mail, either IPv4 or IPv6
     * @param hostName - the domain that provides the sought-after authorization information; initially, the domain portion of the "MAIL FROM" or "HELO" identity
     * @param sender - the "MAIL FROM" or "HELO" identity
     */
    SmtpSocket smtpSocket = smtpSession.getSmtpSocket();

    /**
     * Check basic
     * If the <domain> is malformed (label longer than 63 characters, zero-length label not at the end, etc.)
     * or is not a fully qualified domain name, or if the DNS lookup returns "domain does not exist" (RCODE 3)
     * check_host() immediately returns the result "None".
     * <p>
     * If the <sender> has no localpart, substitute the string "postmaster" for the localpart.
     */

    /**
     * Localhost
     */
    if (smtpSocket.isRemoteLocalhost()) {
      /**
       * SPF don't work for localhost or loopback addresses (127.0.0.*, ::1, etc.)
       */
      return;
    }

    /**
     * James-jspf is not working see test
     * In case of failed, a why link should be given
     * http://www.open-spf.org/Why/API/
     */
    if(smtpSession.getSmtpService().isSpfEnabled()){
      throw SmtpException.createForInternalException("Not yet enabeld");
    }

    /**
     * In case of failed, a why link should be given to the Why
     * http://www.open-spf.org/Why/API/
     */

  }
}
