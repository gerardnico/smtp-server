package com.combostrap.dmarc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The time range in UTC covered by messages in this report,
 *  specified in seconds since epoch
 */
public class DmarcFeedbackDateRange {

  private Long begin;
  private  Long end;

  /**
   * @return the beginning time specified in seconds since epoch in UTC
   */
  @JsonProperty("begin")
  public Long getBegin() {
    return begin;
  }

  @SuppressWarnings("unused")
  public void setBegin(Long begin) {
    this.begin = begin;
  }

  /**
   * @return the end time specified in seconds since epoch in UTC
   */
  @JsonProperty("end")
  public Long getEnd() {
    return end;
  }

  @SuppressWarnings("unused")
  public void setEnd(Long end) {
    this.end = end;
  }

}
