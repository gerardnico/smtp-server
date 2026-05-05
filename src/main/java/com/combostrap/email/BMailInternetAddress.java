package com.combostrap.email;


import com.combostrap.exception.InternalException;
import com.combostrap.type.DnsName;
import com.combostrap.type.EmailAddress;
import com.combostrap.type.EmailCastException;
import com.sanctionco.jmail.InvalidEmailException;
import com.sanctionco.jmail.JMail;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

/**
 * An Internet address is an {@link EmailAddress email address} and a name
 * in different format specified in RFC 822
 * <p>
 * Example:
 * where an email address can be specified in a mailbox format
 * `<yolo yol@email.com>`
 * This class wraps a {@link jakarta.mail.internet.InternetAddress}
 * and a {@link EmailAddress}
 * <p>
 * Internet Address are Strings using the common formats for email with or without real name.
 * * Example of valid values
 * * `username@example.com`
 * * `username@example.com (Firstname Lastname)`
 * * `Firstname Lastname <username@example.com>`
 */
public class BMailInternetAddress {

  private final EmailAddress emailAddress;
  private final InternetAddress internetAddress;

  public static BMailInternetAddress of(String email, String name) throws AddressException {

    if (email == null) {
      throw new AddressException("The email given was null");
    }
    InternetAddress internetAddress = new InternetAddress(email);
    if (name != null) {
      try {
        internetAddress.setPersonal(name);
      } catch (UnsupportedEncodingException e) {
        throw new InternalException(e);
      }
    }
    return new BMailInternetAddress(internetAddress);
  }

  public static BMailInternetAddress of(EmailAddress email, String name) throws AddressException {
    if (email == null) {
      throw new AddressException("The email given was null");
    }
    InternetAddress internetAddress = new InternetAddress(email.toNormalizedString());
    if (name != null) {
      try {
        internetAddress.setPersonal(name);
      } catch (UnsupportedEncodingException e) {
        throw new InternalException(e);
      }
    }
    return new BMailInternetAddress(internetAddress);
  }

  @Override
  public String toString() {
    return this.getInternetAddress().toString();
  }

  /**
   * @param internetAddress - a rfc822 internet address
   */
  private BMailInternetAddress(InternetAddress internetAddress) throws AddressException {

    this.internetAddress = internetAddress;

    /**
     * A group is a list of email
     */
    if (internetAddress.isGroup()) {
      throw new RuntimeException("(" + internetAddress + ") is a group email and it's not yet supported");
    }

    try {
      this.emailAddress = EmailAddress.of(internetAddress.getAddress());
    } catch (EmailCastException e) {
      throw new AddressException("The email address is not valid (" + e.getMessage() + ")");
    }

    /**
     * Set by default the personal to the local part of the email
     */
    if (this.internetAddress.getPersonal() == null) {
      try {
        this.internetAddress.setPersonal(this.emailAddress.getLocalPart());
      } catch (UnsupportedEncodingException e) {
        throw new InternalException("We were unable to set the personal to the email local part", e);
      }
    }

    this.emailValidation();
  }

  public EmailAddress getEmailAddress() {
    return emailAddress;
  }

  /**
   * @deprecated use {@link #getEmailAddress()} to get the domain
   */
  @Deprecated
  public DnsName getDomainName() {
    return emailAddress.getDomainName();
  }

  /**
   * @return the part before the at/arobase character
   * @deprecated use {@link #getEmailAddress()}
   */
  @Deprecated
  public String getLocalPart() {
    return emailAddress.getLocalPart();
  }

  public static BMailInternetAddress of(String email) throws AddressException {

    return of(email, null);

  }

  public static BMailInternetAddress of(InternetAddress internetAddress) throws AddressException {
    return new BMailInternetAddress(internetAddress);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BMailInternetAddress BMailInternetAddress1 = (BMailInternetAddress) o;
    return Objects.equals(emailAddress, BMailInternetAddress1.emailAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(emailAddress);
  }

  /**
   * @throws AddressException - if the email is not a good one
   *                          <a href="https://stackoverflow.com/questions/624581/what-is-the-best-java-email-address-validation-method">...</a>
   *                          <p>See also
   *                          <a href=https://commons.apache.org/proper/">...</a>commons-validator/
   */
  private void emailValidation() throws AddressException {


    this.internetAddress.validate();

    /**
     * Not sure if this is needed
     */
    try {
      JMail.enforceValid(this.emailAddress.toString());
    } catch (InvalidEmailException e) {
      // Handle invalid email
      throw new AddressException("(" + this.emailAddress + ") is not a valid email: " + e.getMessage());
    }

  }


  public InternetAddress getInternetAddress() {
    return this.internetAddress;
  }

  public String getAddress() {
    return this.emailAddress.toString();
  }


  public String toNormalizedString() {
    return this.emailAddress.toNormalizedString();
  }
}
