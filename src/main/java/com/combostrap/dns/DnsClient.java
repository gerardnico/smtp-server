package com.combostrap.dns;

import com.combostrap.type.DnsName;

import java.util.List;
import java.util.Set;

/**
 * A sync Dns Client interface
 * * lookup methods return the first record
 * * resolve methods return a list of record
 */
public interface DnsClient {


  /**
   * @param dnsName - the DNS name
   * @return all Txt
   */
  List<String> resolveTxt(DnsName dnsName) throws DnsException, DnsNotFoundException;

  /**
   *
   * @param dnsName - the DNS Name
   * @return the mx records
   */
  List<DnsMxRecord> resolveMx(DnsName dnsName) throws DnsNotFoundException, DnsException;

  /**
   * @param dnsName - the DNS name
   * @return the first A record
   * behaves the same way as you may be used
   * from when using "nslookup" on your operating system.
   * <p>
   * For a name, you may get multiple record with the same name
   * but two differents address
   * For instance:
   * * when the address is proxied by cloudflare
   * * for a list of host (mailers)
   * Lookup return the first one
   */
  @SuppressWarnings("unused")
  DnsIp lookupA(DnsName dnsName) throws DnsException, DnsNotFoundException;

  /**
   *
   * Alias to {@link #lookupA(DnsName)}
   */
  DnsIp lookupIpv4(DnsName dnsName) throws DnsException, DnsNotFoundException;

  /**
   * @param dnsName - the DNS name
   * @return the first AAAA record
   * behaves the same way as you may be used
   * from when using "nslookup" on your operating system.
   */
  @SuppressWarnings("unused")
  DnsIp lookupAAAA(DnsName dnsName) throws DnsNotFoundException, DnsException;


  String lookupTxt(DnsName dnsName) throws DnsException, DnsNotFoundException;

  /**
   * Due to load balancer, we may get more than one
   */
  Set<DnsIp> resolveAAAA(DnsName dnsName) throws DnsNotFoundException, DnsException;


  Set<DnsIp> resolveA(DnsName dnsName) throws DnsNotFoundException, DnsException;

  Set<DnsName> resolveCName(DnsName dnsName) throws DnsNotFoundException, DnsException;

  DnsName lookupPtr(DnsIp dnsIp) throws DnsNotFoundException, DnsException;

  /**
   * An alias to {@link #lookupPtr(DnsIp)}
   */
  @SuppressWarnings("unused")
  DnsName lookupReverse(DnsIp dnsIp) throws DnsNotFoundException, DnsException;

  String lookupDkimRecord(DnsName dnsName, String dkimSelector) throws DnsException, DnsNotFoundException;

  String lookupSpf(DnsName dnsName) throws DnsException, DnsNotFoundException;

  String lookupDmarc(DnsName dnsName) throws DnsException, DnsNotFoundException;

  /**
   *
   * Alias to {@link #lookupAAAA(DnsName)}
   */
  DnsIp lookupIpv6(DnsName dnsName) throws DnsException, DnsNotFoundException;
}
