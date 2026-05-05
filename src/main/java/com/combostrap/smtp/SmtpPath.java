package com.combostrap.smtp;

import com.combostrap.email.BMailInternetAddress;
import jakarta.mail.internet.AddressException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Implementation of Path (a mailbox address format)
 * <p>
 * A mail message may pass through a number of intermediate
 * relay or gateway hosts on its path from sender to ultimate recipient.
 * <p>
 * <a href="https://datatracker.ietf.org/doc/html/rfc5321#section-4.1.2">Path EBNF</a>
 * <p>
 * Path           = "<" [ A-d-l ":" ] Mailbox ">"
 * <p>
 * Reverse-path   = Path / "<>" (originator, arg from the mail from command)
 * <p>
 * Forward-path   = Path    (It's the email of the recipient given in the rcpt command)
 * <p>
 * <p>
 * Reverse-path value `<>` is in case of delivery failure when returning a bounce
 * <p>
 * Example:
 * RCPT TO:<@hosta.int,@jkl.org:userc@d.bar.org>
 * <p>
 * Historically,
 * mailbox might optionally have been preceded by a list of hosts, but
 * that behavior is now deprecated.
 * <p>
 * The <forward-path> can contain more than just a mailbox.
 * Historically, the <forward-path> was permitted to contain a source
 * routing list of hosts and the destination mailbox; however,
 * contemporary SMTP clients SHOULD NOT utilize source routes (see
 * Appendix C).
 * Servers MUST be prepared to encounter a list of source
 * routes in the forward-path, but they SHOULD ignore the routes or MAY
 * decline to support the relaying they imply
 */
public class SmtpPath {

  private static final String EMPTY = "<>";

  /**
   * <a href="https://datatracker.ietf.org/doc/html/rfc5321#section-4.5.3.1.3">Maximum length</a>
   */
  private static final int MAXIMUM_LENGTH = 256;
  private String route;
  private String mailbox;
  private BMailInternetAddress mailboxInternetAddress;
  private String path;


  public static SmtpPath of(String stringPath) throws SmtpException {

    if (stringPath == null || stringPath.isEmpty()) {
      throw SmtpException.createBadSyntax("A smtp path may not be empty or null");
    }
    if (stringPath.length() < 2) {
      throw SmtpException.createBadSyntax("A smtp path should have at minimum 2 characters (ie `<` and `>`)");
    }
    int totalOctets = stringPath.getBytes(StandardCharsets.UTF_8).length;
    if (totalOctets > MAXIMUM_LENGTH) {
      throw SmtpException.createBadSyntax("The smtp path is too long (" + totalOctets + "). The maximum is " + MAXIMUM_LENGTH);
    }

    /**
     * Enclosing
     */
    if (stringPath.charAt(0) != '<' && stringPath.charAt(stringPath.length() - 1) != '>') {
      throw SmtpException.createBadSyntax("A smtp path should be enclosed by the `less` and `greater than` characters. Example: `<xxx>`");
    }
    String unenclosedPath = stringPath.substring(1, stringPath.length() - 1);

    /**
     * Processing
     */

    String[] routeAndMailBox = unenclosedPath.split(":", 2);
    String mailBox;
    String route = null;
    switch (routeAndMailBox.length) {
      case 1:
        mailBox = routeAndMailBox[0];
        break;
      case 2:
        route = routeAndMailBox[0]; // is deprecated
        mailBox = routeAndMailBox[1];
        break;
      default:
        throw SmtpException.createForInternalException("Internal error while parsing the path, there should be maximum 2 parts");
    }
    SmtpPath smtpPath = new SmtpPath();
    smtpPath.route = route;
    smtpPath.mailbox = mailBox;
    smtpPath.path = stringPath;
    smtpPath.buildAndValidate();
    return smtpPath;
  }


  public static SmtpPath empty() throws SmtpException {
    try {
      return SmtpPath.of(EMPTY);
    } catch (SmtpException e) {
      throw SmtpException.createForInternalException("Empty Smtp Path should not throw", e);
    }
  }

  public String getMailBox() {
    return this.mailbox;
  }

  public String getRoute() {
    return this.route;
  }

  private SmtpPath buildAndValidate() throws SmtpException {
    if (this.mailbox.isEmpty()) {
      /**
       * Internet Address reject empty string
       * but this is a valid path in case of bounce
       */
      this.mailboxInternetAddress = null;
      return this;
    }
    if (this.mailbox.equals(SmtpPostMaster.POSTMASTER)) {
      /**
       * Internet Address reject internet address without any domain
       * such as `postmaster`
       */
      this.mailboxInternetAddress = null;
      return this;
    }
    try {
      this.mailboxInternetAddress = BMailInternetAddress.of(this.mailbox);
    } catch (AddressException e) {
      throw SmtpException.createBadSequence("The mailbox (" + this.mailbox + ") is not valid", e);
    }
    return this;
  }

  /**
   * @return the mailbox in internet address format
   */
  public Optional<BMailInternetAddress> getInternetAddress(SmtpPostMaster smtpPostMaster)  {

    if (this.mailbox == null) {
      return Optional.empty();
    }
    if (smtpPostMaster != null && smtpPostMaster.isPostMasterPath(this.path)) {
      return Optional.of(smtpPostMaster.getPostmasterAddress());
    }
    return Optional.of(this.mailboxInternetAddress);
  }

  @Override
  public String toString() {
    return path;
  }

}
