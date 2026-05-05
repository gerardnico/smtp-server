package com.combostrap.smtp;

import java.time.Instant;

/**
 * A class to hold the delivery failure information
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class SmtpDeliveryFailure {

  private int counter = 0;
  private Instant instant;
  private Throwable e;

  public void inc(Throwable e) {
    this.instant = Instant.now();
    this.counter++;
    this.e = e;
  }

  public Instant getLastTentative() {
    return this.instant;
  }

}
