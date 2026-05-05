package com.combostrap.dns;

import org.xbill.DNS.ResolverConfig;

import java.net.InetSocketAddress;

/**
 * See the Dns local nameserver or DNS search path limitations
 * <a href="https://github.com/dnsjava/dnsjava#limitations">limitations</a>
 */
public class DnsNameServer {


  private static final InetSocketAddress LOCALE_NAME_SERVER;

  static {
    LOCALE_NAME_SERVER = ResolverConfig.getCurrentConfig().server();
  }

  public static InetSocketAddress getLocale(){
    return LOCALE_NAME_SERVER;
  }

}
