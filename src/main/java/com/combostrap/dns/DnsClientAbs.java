package com.combostrap.dns;


import com.combostrap.type.CastException;
import com.combostrap.type.DnsName;

import java.util.List;
import java.util.Set;

public abstract class DnsClientAbs implements DnsClient {



  @Override
  public DnsIp lookupA(DnsName dnsName) throws DnsException, DnsNotFoundException {
    Set<DnsIp> dnsIps = this.resolveA(dnsName);
    if (dnsIps.isEmpty()) {
      throw new DnsNotFoundException("There is no A record for the name (" + dnsName + ")");
    }
    return dnsIps.iterator().next();
  }

  @Override
  public DnsIp lookupIpv4(DnsName dnsName) throws DnsException, DnsNotFoundException {
    return lookupA(dnsName);
  }

  @Override
  public DnsIp lookupAAAA(DnsName dnsName) throws DnsNotFoundException, DnsException {
    Set<DnsIp> dnsIps = this.resolveAAAA(dnsName);
    if (dnsIps.isEmpty()) {
      throw new DnsNotFoundException("There is no AAAA record for the name (" + dnsName + ")");
    }
    /**
     * Due to load balancer, we may get more than one
     */
    return dnsIps.iterator().next();
  }

  @Override
  public String lookupTxt(DnsName dnsName) throws DnsException, DnsNotFoundException {
    List<String> txtRecords = this.resolveTxt(dnsName);
    if (txtRecords.isEmpty()) {
      throw new DnsNotFoundException();
    }
    return txtRecords.get(0);
  }

  @Override
  public String lookupDkimRecord(DnsName dnsName, String dkimSelector) throws DnsException, DnsNotFoundException {

    try {
      DnsName dkimSelectorName = DnsName.create(dkimSelector + "._domainkey." + dnsName.toString());
      return getTextRecordThatStartsWith(dkimSelectorName, "v=DKIM1");
    } catch (CastException e) {
      throw new RuntimeException("We create the name, it should be good",e);
    }

  }

  /**
   * @param startsWith - the prefix
   */
  String getTextRecordThatStartsWith(DnsName dnsName, String startsWith) throws DnsNotFoundException, DnsException {

    return this.resolveTxt(dnsName)
      .stream()
      .filter(record -> record.startsWith(startsWith))
      .findFirst()
      .orElseThrow(() -> new DnsNotFoundException("The (" + startsWith + ") text record for the name (" + dnsName + ") was not found"));

  }

  @Override
  public String lookupSpf(DnsName dnsName) throws DnsException, DnsNotFoundException {

    return getTextRecordThatStartsWith(dnsName, "v=spf1");

  }

  @Override
  public String lookupDmarc(DnsName dnsName) throws DnsException, DnsNotFoundException {
    try {
      return getTextRecordThatStartsWith(dnsName.getSubdomain("_dmarc"), "v=DMARC1");
    } catch (CastException e) {
      throw new DnsInternalException("_dmarc is a valid name", e);
    }
  }

  public void printRecords(DnsName dnsName) {

    System.out.println("DNS Records for " + dnsName);
    String tabLevel1 = "  - ";
    String tabLevel2 = "     - ";
    try {
      System.out.println(tabLevel1 + "Ipv4 (A records)");
      Set<DnsIp> aRecords = this.resolveA(dnsName);
      for (DnsIp dnsIp : aRecords) {
        System.out.println(tabLevel2 + dnsIp);
      }
    } catch (DnsException e) {
      throw new RuntimeException(e);
    } catch (DnsNotFoundException e) {
      System.out.println(tabLevel2 + "No A record");
    }


    try {
      System.out.println(tabLevel1 + "Ipv6 (AAAA records)");
      Set<DnsIp> dnsIpv6s = this.resolveAAAA(dnsName);
      if (dnsIpv6s.isEmpty()) {
        System.out.println(tabLevel2 + "none");
      }
      for (DnsIp dnsIp : dnsIpv6s) {
        System.out.println(tabLevel2 + dnsIp);
      }
    } catch (DnsException e) {
      throw new RuntimeException(e);
    } catch (DnsNotFoundException e) {
      System.out.println(tabLevel2 + "No AAAA record");
    }

    try {
      System.out.println(tabLevel1 + "Cname for " + this);
      Set<DnsName> cnameRecords = resolveCName(dnsName);
      if (cnameRecords.isEmpty()) {
        System.out.println(tabLevel2 + "none");
      }
      for (DnsName cnameRecord : cnameRecords) {
        System.out.println(tabLevel2 + cnameRecord.toString());
      }
    } catch (DnsException e) {
      throw new RuntimeException(e);
    } catch (DnsNotFoundException e) {
      System.out.println(tabLevel2 + "No domain record");
    }
  }

  public void printRecords(String domainName) throws CastException {
    printRecords(DnsName.create(domainName));
  }

  public DnsIp lookupA(String domainName) throws CastException, DnsException, DnsNotFoundException {
    return this.lookupA(DnsName.create(domainName));
  }

  @Override
  public DnsIp lookupIpv6(DnsName dnsName) throws DnsException, DnsNotFoundException {
    return lookupAAAA(dnsName);
  }

  @Override
  public DnsName lookupReverse(DnsIp dnsIp) throws DnsNotFoundException, DnsException {
    return lookupPtr(dnsIp);
  }
}
