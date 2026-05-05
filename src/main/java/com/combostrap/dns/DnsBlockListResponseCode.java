package com.combostrap.dns;

public enum DnsBlockListResponseCode {


  /**
   * Spamhaus Domain response with error of our parts
   * <a href="https://www.spamhaus.org/faq/section/Spamhaus%20DBL#291">...</a>
   */
  R_127_255_255_252("127.255.255.252", "Typing error in DNSBL name", false),
  /**
   * With a public cloud, the DNS is public.
   * From home, this is not the case if you use a router.
   * A Bind DNS server should be used ?
   */
  R_127_255_255_254("127.255.255.254", "Anonymous query through public resolver", false),
  R_127_255_255_255("127.255.255.255", "Excessive number of queries", false),

  /**
   * Domain response as blocked
   * 127.0.1.0/24	Spamhaus Domain Blocklists
   * <a href="http://www.spamhaus.org/faq/section/Spamhaus%20DBL#291">...</a>
   */
  R_127_0_1_2("127.0.1.2", "spam domain", true),
  R_127_0_1_4("127.0.1.4", "Phish domain", true),
  R_127_0_1_5("127_0_1_5", "malware domain", true),

  R_127_0_1_6("127.0.1.6", "botnet C&C domain", true),
  R_127_0_1_102("127.0.1.102", "Abused legit spam", true),
  R_127_0_1_103("127.0.1.103", "Abused spammed redirector domain", true),
  R_127_0_1_104("127.0.1.104", "Abused legit phish", true),
  R_127_0_1_105("127.0.1.105", "Abused legit malware", true),
  R_127_0_1_106("127.0.1.106", "Abused legit botnet C&C", true),
  R_127_0_1_255("127.0.1.255", "IP queries prohibited!", true),

  /**
   * IP blocking response
   * ie 127.0.0.0/24	Spamhaus IP Blocklists
   * <a href="https://www.spamhaus.org/zen/">...</a>
   */
  // Direct UBE sources, spam operations & spam services
  R_127_0_0_2("127.0.0.2", "Spamhaus SBL Data", true),
  // Direct snowshoe spam sources detected via automation
  R_127_0_0_3("127.0.0.3", "Spamhaus SBL CSS Data", true),
  /**
   * 127.0.0.4-7 -> CBL (3rd party exploits such as proxies, trojans, etc.)
   */
  R_127_0_0_4("127.0.0.4", "CBL Data", true),
  R_127_0_0_5("127.0.0.5", "CBL Data", true),
  R_127_0_0_6("127.0.0.6", "CBL Data", true),
  R_127_0_0_7("127.0.0.7", "CBL Data", true),
  R_127_0_0_9("127.0.0.9", "Spamhaus DROP/EDROP Data (in addition to 127.0.0.2, since 01-Jun-2016)", true),
  /**
   * 127.0.0.10-11 -> End-user Non-MTA IP addresses set by ISP outbound mail policy
   */
  R_127_0_0_10("127.0.0.10", "ISP Maintained", true),
  R_127_0_0_11("127.0.0.11", "Spamhaus Maintained", true),
  /**
   * Internal response code
   */
  R_INTERNAL_RESPONSE_NOT_IN_LIST("", "Response code not in list", false),
  R_INTERNAL_HOST_NOT_BLOCKED("", "Host not blocked (not in list)", false),
  R_INTERNAL_HOST_BLOCKED("", "Host found in list", false);


  private final String responseCode;
  private final String description;
  /**
   * Wne false, the codes indicate an error condition
   * and must not be taken to imply that the object of the query is "listed"
   */
  private final boolean block;

  DnsBlockListResponseCode(String responseCode, String description, boolean block) {
    this.responseCode = responseCode;
    this.description = description;
    this.block = block;
  }

  public String getCode() {
    return this.responseCode;
  }

  public boolean getBlocked() {
    return this.block;
  }

  public String getDescription() {
    return this.description;
  }

}
