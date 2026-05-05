package com.combostrap.dns;

import java.net.InetAddress;

public class DnsAddress {
  private final InetAddress address;

  public DnsAddress(InetAddress byAddress) {
    this.address = byAddress;
  }



  public InetAddress getAddress() {
    return this.address;
  }


}
