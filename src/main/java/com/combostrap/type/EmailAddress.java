package com.combostrap.type;

import com.combostrap.exception.InternalException;
import com.sanctionco.jmail.EmailValidator;
import com.sanctionco.jmail.InvalidEmailException;
import com.sanctionco.jmail.JMail;

import java.util.Objects;

/**
 * An email address (not an Internet Address)
 * <p>
 * Usage: handle Email validation in DNS records.
 * <p>
 * If you use SMTP (Internet Address), you should use the BMailInternetAddress that represents
 * an Internet Address (ie Jakarta package representations of an email address and name)
 * <p>
 * Why, do we have extracted the email address.
 * It's difficult to not create a circular dependencies between DNS and the Smtp Package. ie
 * * There is an email in dmarc, we need it then in DNS to verify that this an email.
 * * And if the smtp package does need it also
 * We may also use a third email package such as Vertx in place of SimpleEmail
 */
public class EmailAddress {


    private final String mailAddress;
    private final DnsName domain;
    /**
     * The part before the arobase
     */
    private final String localPart;
    /**
     * The part before any plus in the local part
     * or the local part if none
     * <p>
     * For example,
     * if the email address is john+shopping@gmail.com,
     * or john+anythingyouwant@gmail.com
     * localPartBox is john
     * <p>
     * And you can reach john at john@gmail.com
     */
    private final String localPartBox;
    private final static EmailValidator validator;

    static {
        validator = JMail.strictValidator()
                .requireTopLevelDomain();
    }

    public EmailAddress(String mailAddress) throws EmailCastException {

        if (mailAddress == null) {
            throw new InternalException("The email value should not be null");
        }
        this.mailAddress = mailAddress;
        final String[] split = mailAddress.split("@");
        if (split.length != 2) {
            throw new EmailCastException("The email address does not have 2 parts (ie localname@domain)");
        }
        String absoluteName = split[1];
        try {
            this.domain = DnsName.create(absoluteName);
        } catch (CastException e) {
            throw new EmailCastException("The domain (" + absoluteName + ") is not valid (");
        }
        this.localPart = split[0];

        // LocalPartBox (email without the Alias ie + part)
        int indexOfPlus = this.localPart.indexOf("+");
        if (indexOfPlus != -1) {
            this.localPartBox = this.localPart.substring(0, indexOfPlus);
        } else {
            this.localPartBox = this.localPart;
        }
        /**
         * Basic validation
         * user@[10.9.8.7] and user@localhost are also valid,
         * but we don't accept them
         */
        if (this.domain.toStringWithoutRoot().equals("localhost")) {
            throw new EmailCastException("The domain should not be localhost");
        }
        if (this.domain.toStringWithoutRoot().startsWith("[")) {
            throw new EmailCastException("The domain should not start with a [");
        }
        if (this.domain.getLabels().size() == 1) {
            throw new EmailCastException("The email " + mailAddress + " has a domain without a tld");
        }
        try {
            validator.enforceValid(mailAddress);
        } catch (InvalidEmailException e) {
            throw new EmailCastException(e);
        }

    }

    public static EmailAddress of(String mail) throws EmailCastException {
        return new EmailAddress(mail);
    }

    /**
     * A method that does not throw because we write the email as literal, or
     * we retrieve in a database an email that we already have validated on write
     */
    public static EmailAddress ofFailSafe(String emailAddress) {
        try {
            return EmailAddress.of(emailAddress);
        } catch (EmailCastException e) {
            throw new RuntimeException("The email address is not valid (" + emailAddress + "). Error: " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return toNormalizedString();
    }

    public DnsName getDomainName() {
        return this.domain;
    }

    @SuppressWarnings("unused")
    public String getLocalPart() {
        return localPart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailAddress that = (EmailAddress) o;
        return Objects.equals(mailAddress, that.mailAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mailAddress);
    }

    public Integer getLocalPartDigitCount() {

        return Math.toIntExact(localPart.chars()
                .filter(i -> Character.isDigit((char) i))
                .count());
    }

    /**
     * @return the email in lowercase format without any specified alias functionality (ie no + in the local part)
     * ChatGpt: In practice, most mail servers and services treat both the local and domain parts of email addresses as case-insensitive.
     * Nonetheless, it's always recommended to follow the standard conventions and use lowercase characters
     * for the entire email address to avoid potential issues or confusion.
     */
    public String toNormalizedString() {

        String emailWithoutAlias = this.localPartBox + "@" + this.domain.toStringWithoutRoot();
        return emailWithoutAlias.toLowerCase();

    }


    /**
     * @return the local part without any alias part (ie
     * from john+alias@smith.com, it will be john
     */
    public String getLocalBox() {
        return this.localPartBox;
    }


}
