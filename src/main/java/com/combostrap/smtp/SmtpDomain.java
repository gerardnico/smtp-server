package com.combostrap.smtp;

import com.combostrap.dns.DnsIllegalArgumentException;
import com.combostrap.exception.InternalException;
import com.combostrap.type.CastException;
import com.combostrap.type.DnsName;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A class that wraps a domain name
 * for the SMTP context
 */
public class SmtpDomain {


  private final DnsName domainName;
  private final Map<String, SmtpUser> users = new HashMap<>();

  public SmtpDomain(String name) throws DnsIllegalArgumentException {

      try {
          this.domainName = DnsName.create(name);
      } catch (CastException e) {
          throw new DnsIllegalArgumentException(e);
      }

  }

  public static SmtpDomain createFromName(String name) throws DnsIllegalArgumentException {
    return new SmtpDomain(name);
  }


  public DnsName getDnsDomain() {
    return domainName;
  }

  public Optional<SmtpUser> getUser(String userName)  {
    SmtpUser user = this.users.get(userName);
    if (user == null) {
      return Optional.empty();
    }
    return Optional.of(user);
  }

  @Override
  public String toString() {
    return getDnsDomain().toStringWithoutRoot();
  }

  public SmtpDomain addUser(SmtpUser smtpUser) {
    if (!smtpUser.getDomain().equals(this)) {
      throw new InternalException("The  domain (" + this + ") does not accept users of another domain (" + smtpUser + ")");
    }
    this.users.put(smtpUser.getName(), smtpUser);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SmtpDomain that = (SmtpDomain) o;
    return Objects.equals(domainName, that.domainName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domainName.toString());
  }

}
