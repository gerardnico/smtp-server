package com.combostrap.dns;

import com.combostrap.type.DnsName;

public interface DnsMxRecord {

  /**
   * The priority of the MX record.
   */
  int getPriority();

  /**
   * The target name of the MX record
   */
  DnsName getTarget();

}
