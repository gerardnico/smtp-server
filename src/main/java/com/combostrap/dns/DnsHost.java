package com.combostrap.dns;

import com.combostrap.type.CastException;
import com.combostrap.type.DnsName;

/**
 * Represents a host with a name and its 2 ip address
 */
public class DnsHost {

  private final DnsHostConfig config;

  public DnsHost(DnsHostConfig config) {
    this.config = config;
  }



  public String getName() {
    return this.config.dnsName.toStringWithoutRoot();
  }

  public DnsIp getIpv4() {
    return this.config.ipv4;
  }

  public DnsIp getIpv6() {
    return this.config.ipv6;
  }

  public DnsName getDnsName() {
    return this.config.dnsName;
  }


  public static class DnsHostConfig {
    private final DnsName dnsName;
    private DnsIp ipv4;
    private DnsIp ipv6;

    public DnsHostConfig(String name) throws DnsIllegalArgumentException {

        try {
            this.dnsName = DnsName.create(name);
        } catch (CastException e) {
            throw new DnsIllegalArgumentException(e);
        }

    }

    public DnsHostConfig setIpv4(String ipv4) throws DnsException {

      this.ipv4 = DnsIp.createFromIpv4String(ipv4);
      return this;
    }

    public DnsHostConfig setIpv6(String ipv6) throws DnsException {
      this.ipv6 = DnsIp.createFromIpv6String(ipv6);
      return this;
    }

    public DnsHost build() {
      return new DnsHost(this);
    }
  }

  @Override
  public String toString() {
    return config.dnsName.toString();
  }

}
