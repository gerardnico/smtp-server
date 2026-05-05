package com.combostrap.dns;

import com.combostrap.type.DnsName;

/**
 *
 * Represent a Service-Record (SRV)
 *
 */
@SuppressWarnings("unused")
public interface DnsSrvRecord {

  /**
   * Returns the priority for this service record.
   */
  int getPriority();

  /**
   * Returns the weight of this service record.
   */
  int getWeight();

  /**
   * Returns the port the service is running on.
   */
  int getPort();

  /**
   * Returns the name for the server being queried.
   */
  DnsName getName();

  /**
   * Returns the protocol for the service being queried (i.e. "_tcp").
   */
  String getProtocol();

  /**
   * Returns the service's name (i.e. "_http").
   */
  String getService();

  /**
   * Returns the name of the host for the service.
   */
  DnsName getTarget();

}
