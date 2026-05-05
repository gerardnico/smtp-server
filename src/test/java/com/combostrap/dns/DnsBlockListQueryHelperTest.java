package com.combostrap.dns;

import com.combostrap.type.DnsCastException;
import com.combostrap.type.DnsName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class DnsBlockListQueryHelperTest {


  @Test
  void ipBlockListTest() throws IllegalStructure, DnsException, DnsNotFoundException {

    List<DnsBlockListQueryHelper> responses = DnsBlockListQueryHelper
      .forIp("192.99.55.226")
      .build();
    this.assertNotBlocked(responses);

  }

  private void assertNotBlocked(List<DnsBlockListQueryHelper> blockListQueryHelpers) throws DnsException, DnsNotFoundException {
    XBillDnsClient dnsClient = XBillDnsClient.builder().build();
    for (DnsBlockListQueryHelper dnsBlockListQueryHelper: blockListQueryHelpers) {
      Set<DnsIp> dnsIp = dnsClient.resolveA(dnsBlockListQueryHelper.getDnsNameToQuery());
      if(dnsIp.isEmpty()){
        System.out.println("Not blocked");
        continue;
      }
      DnsBlockListResponseCode responseCode = dnsBlockListQueryHelper.createResponseCode(dnsIp.iterator().next());
      if(responseCode.getBlocked()) {
        System.out.println("blocked");
      } else {
        System.out.println("not blocked");
      }
      Assertions.assertFalse(responseCode.getBlocked());
    }
  }

  @Test
  void domainBlockTest() throws DnsException, DnsNotFoundException, DnsCastException {

    List<DnsBlockListQueryHelper> queryHelpers = DnsBlockListQueryHelper
      .forDomain(DnsName.create("eraldy.com"))
      .build();
    this.assertNotBlocked(queryHelpers);


  }


}
