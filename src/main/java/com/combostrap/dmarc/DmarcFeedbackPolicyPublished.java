package com.combostrap.dmarc;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class DmarcFeedbackPolicyPublished {

  /**
   * The domain at which the DMARC record was found
   */
  @JsonProperty("domain")
  String domain;
  /**
   * The DKIM alignment mode
   */
  @JsonProperty("adkim")
  String adkim;
  /**
   * The SPF alignment mode
   */
  @JsonProperty("aspf")
  String aspf;
  /**
   * The policy to apply to messages from the domain
   */
  @JsonProperty("p")
  String p;
  /**
   * The policy to apply to messages from subdomains.
   */
  @JsonProperty("sp")
  String sp;
  /**
   * The percent of messages to which policy applies.
   */
  @JsonProperty("pct")
  Integer pct;
  /**
   * Failure reporting options in effect
   */
  @JsonProperty("np")
  String np;

  public String getDomain() {
    return this.domain;
  }

}
