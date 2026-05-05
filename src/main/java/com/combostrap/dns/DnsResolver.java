package com.combostrap.dns;

import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.SimpleResolver;

import java.net.UnknownHostException;

/**
 * Resolver transforms a user request into a query
 * <p>
 * <a href="https://datatracker.ietf.org/doc/html/rfc1035#autoid-64">...</a>
 */
public class DnsResolver {



  private static final Resolver DEFAULT;
  private static final Resolver CLOUDFLARE_RESOLVER;
  private static final Resolver GOOGLE_RESOLVER;

  static {
    try {
      DEFAULT = new SimpleResolver(ResolverConfig.getCurrentConfig().server());

      //https://developers.google.com/speed/public-dns
      //GOOGLE_RESOLVER = new SimpleResolver("8.8.8.8");
      //https://developers.cloudflare.com/1.1.1.1/privacy/public-dns-resolver/
      CLOUDFLARE_RESOLVER = new SimpleResolver("1.1.1.1");
      GOOGLE_RESOLVER = new SimpleResolver("8.8.8.8");
    } catch (UnknownHostException e) {
      throw new RuntimeException("Error while initializing the dns resolver");
    }
  }
  @SuppressWarnings("unused")
  public static Resolver getGoogle() {
    return GOOGLE_RESOLVER;
  }

  @SuppressWarnings("unused")
  public static Resolver getCloudflare() {
    return CLOUDFLARE_RESOLVER;
  }

  public static Resolver getLocal() {
    return DEFAULT;
  }

}
