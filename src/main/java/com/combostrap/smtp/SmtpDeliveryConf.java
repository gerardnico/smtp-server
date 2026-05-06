package com.combostrap.smtp;

public class SmtpDeliveryConf {

    /**
     * Immediate delivery after reception or in the queue
     */
    public boolean immediateDelivery = false;
    /**
     * Deliver run in minutes
     */
    public Integer runInterval = 5;
    /**
     * Retry Interval Between Failures
     */
    public Integer retryInterval = 15;
}
