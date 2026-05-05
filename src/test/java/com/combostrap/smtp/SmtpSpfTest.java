package com.combostrap.smtp;

import org.apache.james.jspf.core.DNSServiceEnabled;
import org.apache.james.jspf.core.MacroExpand;
import org.apache.james.jspf.core.MacroExpandEnabled;
import org.apache.james.jspf.core.SPFCheckEnabled;
import org.apache.james.jspf.executor.AsynchronousSPFExecutor;
import org.apache.james.jspf.executor.SPFResult;
import org.apache.james.jspf.impl.DNSServiceXBillImpl;
import org.apache.james.jspf.impl.DefaultTermsFactory;
import org.apache.james.jspf.impl.SPF;
import org.apache.james.jspf.parser.RFC4408SPF1Parser;
import org.apache.james.jspf.wiring.WiringServiceTable;
import org.junit.jupiter.api.Test;

import static org.apache.james.jspf.core.exceptions.SPFErrorConstants.FAIL_CONV;
import static org.apache.james.jspf.core.exceptions.SPFErrorConstants.PASS_CONV;

class SmtpSpfTest {

  @Test
  void spfJamesTest() {

    /**
     * Code found here:
     * https://github.com/apache/james-jspf/tree/master
     * <p>
     * Does not pass this basic test
     */
    DNSServiceXBillImpl dnsService = new DNSServiceXBillImpl();
    WiringServiceTable wiringService = new WiringServiceTable();
    wiringService.put(DNSServiceEnabled.class, dnsService);
    MacroExpand macroExpand = new MacroExpand(dnsService);
    wiringService.put(MacroExpandEnabled.class, macroExpand);
    RFC4408SPF1Parser parser = new RFC4408SPF1Parser(new DefaultTermsFactory(wiringService));
    wiringService.put(SPFCheckEnabled.class, this);
    AsynchronousSPFExecutor executor = new AsynchronousSPFExecutor(dnsService);
    SPF spf = new SPF(dnsService,parser,macroExpand,executor);
    //DefaultSPF spf = new DefaultSPF();
    // spfquery -ip=11.22.33.44 -sender=user@aol.com -helo=spammer.tld
    SPFResult res = spf.checkSPF("11.22.33.44", "user@aol.com", "spammer.tld");
    if (res.getResult().equals(FAIL_CONV)) {
      System.out.println("Failed");
    } else if ( res.getResult().equals(PASS_CONV)) {
      System.out.println("Passed");
    } else {
      //      Further results are:
      //
      //      PERM_ERROR_CONV = "error";
      //      NONE_CONV = "none";
      //      TEMP_ERROR_CONV = "temperror";
      //      PASS_CONV = "pass";
      //      NEUTRAL_CONV = "neutral";
      //      FAIL_CONV = "fail";
      //      SOFTFAIL_CONV = "softfail";
    }

  }
}
