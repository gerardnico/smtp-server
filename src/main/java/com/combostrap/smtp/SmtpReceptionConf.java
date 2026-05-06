package com.combostrap.smtp;

public class SmtpReceptionConf {

    // false is the default because:
    // * on a cloud hosting, the DNS is public, and it's not supported by SpamHaus because they can't rate limit
    // * the filtering happens on ipv4, not ipv6
    public boolean enableDnsBlockList = false;

}
