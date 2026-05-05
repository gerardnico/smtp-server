package com.combostrap.smtp;


import com.combostrap.email.BMailMimeMessage;
import com.combostrap.email.BMailMimeMessageHeader;

import static com.combostrap.smtp.SmtpSyntax.FWS;
import static com.combostrap.smtp.SmtpSyntax.getCurrentDateInMailFormat;

/**
 * Trace info is:
 * * the return path
 * * the received header
 * The doc is <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.4">Trace information section: Syntax and info</a>
 */
public class SmtpReceptionTracing {



  /**
   * Received header:
   * The doc is <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.4">Trace information section: Syntax and info</a>
   * Every SMTP server that receives a message MUST insert a "Received:" header field
   * at the beginning of the message content to trace the timeline.
   * An Internet mail program MUST NOT change an actual Received line that was previously added.
   * <p>
   * Value:
   * * use explicit offsets in the dates (e.g., -0800), rather than time zone names of any type.
   * * Local time (with an offset) is preferred to UT (Universal Time) when feasible.
   * * If it is desired to supply a time zone name, it SHOULD be included in a comment.
   * <p>
   * Auth and received: <a href="https://datatracker.ietf.org/doc/html/rfc4954#section-7">...</a>
   * Upon successful authentication,
   * a server SHOULD use the "ESMTPA" or the "ESMTPSA" [SMTP-TT] (when
   * appropriate) keyword in the "with" clause of the Received header
   * field.
   * Example in the same email 2 received:
   * Received: by 2002:a05:7010:ee05:b0:37c:efaf:9fbb with SMTP id pc5csp3700801mdb;
   * Thu, 28 Sep 2023 21:34:38 -0700 (PDT)
   * .......
   * Received: from a13-34.smtp-out.amazonses.com (a13-34.smtp-out.amazonses.com. [54.240.13.34])
   * by mx.google.com with ESMTPS id v2-20020a0cdd82000000b0065d1380dd1fsi822550qvk.29.2023.09.28.21.34.37
   * for <gerardnico@gmail.com>
   * (version=TLS1_2 cipher=ECDHE-ECDSA-AES128-GCM-SHA256 bits=128/128);
   * Thu, 28 Sep 2023 21:34:37 -0700 (PDT)
   */
  private static void addReceivedHeader(SmtpSession smtpSession, BMailMimeMessage mimeMessage) {

    /**
     * For the EBNF, see the end of
     * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.4">Trace information section: Syntax and info</a>
     * where FWS = whitespace
     * Only the FROM, BY and time clause ar mandatory
     */
    String received = "FROM" +
      FWS +
      smtpSession.getSmtpSocket().getSourceExtendedDomain() +
      FWS +
      "BY" +
      FWS +
      smtpSession.getGreeting().getRequestedHostOrDefault().getHostedHostname() +
      FWS +
      "with" +
      FWS +
      smtpSession.getSmtpProtocol() +
      FWS +
      "id" +
      FWS +
      smtpSession.getId() +
      ";" +
      getCurrentDateInMailFormat();

    mimeMessage.addHeader(BMailMimeMessageHeader.RECEIVED, received);
  }


  public static void addTraceHeader(SmtpSession smtpSession, BMailMimeMessage mimeMessage) {
    addReceivedHeader(smtpSession,  mimeMessage);
    addReturnPathHeader();
  }

  /**
   * Trace info:
   * The doc is <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.4">Trace information section: Syntax and info</a>
   */
  private static void addReturnPathHeader() {
    //
  }

}
