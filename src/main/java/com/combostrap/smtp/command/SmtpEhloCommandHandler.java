package com.combostrap.smtp.command;

import com.combostrap.smtp.*;
import com.combostrap.smtp.sasl.SimpleAuthMechanism;
import com.combostrap.type.CastException;
import com.combostrap.type.DnsName;

import java.util.ArrayList;
import java.util.List;

/**
 * These commands are used to identify the SMTP client to the SMTP server.
 * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.1.1.1">...</a>
 * The SMTP server identifies itself to the SMTP
 * client in the connection greeting reply and in the response to this
 * command.
 * <p>
 * Example: <a href="https://datatracker.ietf.org/doc/html/rfc2821#appendix-D.1">Typical SMTP Transaction Scenario</a>
 * 250-foo.com greets bar.com
 * S: 250-8BITMIME
 * S: 250-SIZE 35882577  # limit of 35 MB. See <a href="https://www.rfc-editor.org/rfc/rfc1427.html">...</a>
 * S: 250-DSN
 * S: 250-VRFY
 * S: 250 HELP
 * <p>
 * For Gmail:
 * 250-mx.google.com at your service, [2607:5300:201:3100::85b]
 * 250-SIZE 157286400
 * 250-8BITMIME
 * 250-PIPELINING
 * 250-CHUNKING
 * 250-STARTTLS
 * 250-ENHANCEDSTATUSCODES
 * 250 SMTPUTF8
 * <p>
 * <a href="https://www.ietf.org/rfc/rfc1869.txt">...</a> - Section 4 extensions
 * A client SMTP supporting {@link SmtpExtensionParameter SMTP service extensions} should start an SMTP
 * session by issuing the EHLO command instead of the HELO command. If
 * the SMTP server supports the SMTP service extensions it will give a
 * successful response (see section 4.3), a failure response (see 4.4),
 * or an error response (4.5). If the SMTP server does not support any
 * SMTP service extensions it will generate an error response (see
 * section 4.5).
 */
public final class SmtpEhloCommandHandler extends SmtpInputCommandDirectReplyHandler {

  public SmtpEhloCommandHandler(SmtpCommand smtpCommand) {
    super(smtpCommand);
  }


  @Override
  public SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException {

    SmtpInputCommand smtpRequest = smtpInputContext.getSmtpInput().getSmtpRequestCommand();
    /**
     * The argument field contains the fully-qualified domain name
     * of the SMTP client if one is available
     * <p>
     * Example: EHLO [IPv6:::1]
     */
    List<String> arguments = smtpRequest.getArguments();
    if (arguments.isEmpty()) {
      throw SmtpException.create(SmtpReplyCode.SYNTAX_ERROR_501, "Expected Syntax is `" + SmtpCommand.EHLO.getCommandSyntax() + "`");
    }

    String hostName = arguments.get(0);
    DnsName clientHostname;
    try {
      clientHostname = DnsName.create(hostName);
    } catch (CastException e) {
      throw SmtpException.create(SmtpReplyCode.SYNTAX_ERROR_501, "The hostname value (" + hostName + ") is not valid");
    }

    /**
     * Domain Check
     */
    SmtpSession session = smtpInputContext.getSession();
    SmtpFiltering.ehloCheckHost(session, clientHostname);

    /**
     * Set the hostname advertised
     */
    session.getTransactionState().setEhloClientHostName(clientHostname);

    /**
     * Greetings
     */
    SmtpReply smtpReply = SmtpReply.create(SmtpReplyCode.OK_250)
      .addHumanTextLine(session.getGreeting().getRequestedHostOrDefault() + " at your service");

    /**
     * An EHLO command MAY be issued by a client later in the session.  If
     * it is issued after the session begins, the SMTP server MUST clear all
     * buffers and reset the state exactly as if a RSET command had been
     * issued.  In other words, the sequence of RSET followed immediately by
     * EHLO is redundant, but not harmful other than in the performance cost
     * of executing unnecessary commands.
     * https://datatracker.ietf.org/doc/html/rfc2821#section-4.1.4
     */
    SmtpCommand.RSET.getHandler().handle(smtpInputContext);

    /**
     * We advertise the {@link SmtpExtensionParameter}
     * that we support below
     */

    /**
     * 8-bit character body in plain text mode
     * There was an old one {@link SmtpInputType TEXT_BIT7}
     * on 7 bit that nobody supports anymore.
     */
    smtpReply.addHumanTextLine(SmtpInputType.TEXT_BIT8.toString());

    /**
     * Advertise the max size
     */
    SmtpService service = smtpInputContext.getSmtpService();
    int maxSize = service.getSmtpServer().getMaxMessageSizeInBytes();
    if (maxSize > 0) {
      smtpReply.addHumanTextLine(SmtpExtensionParameter.SIZE + " " + maxSize);
    }

    /**
     * Pipelining
     */
    if (service.isPipeliningEnabled()) {
      smtpReply.addHumanTextLine(SmtpExtensionParameter.PIPELINING.toString());
    }

    /**
     * Enabling / Hiding TLS
     */
    if (service.isStartTlsEnabled()) {
      smtpReply.addHumanTextLine(SmtpExtensionParameter.STARTTLS.toString());
    }

    /**
     * {@link SmtpBdatCommandHandler} Chunking Support
     */
    if (smtpInputContext.getSmtpService().isChunkingEnabled()) {
      smtpReply.addHumanTextLine(SmtpExtensionParameter.CHUNKING.toString());
    }
    if (smtpInputContext.getSmtpService().isBinaryMimeEnabled()) {
      smtpReply.addHumanTextLine(SmtpInputType.BINARYMIME.toString());
    }


    /**
     * Authentication support
     */
    if (smtpInputContext.getSmtpService().isAuthRequired()) {
      List<String> authResponse = new ArrayList<>();
      authResponse.add(SmtpExtensionParameter.AUTH.toString());
      for (SimpleAuthMechanism simpleAuthMechanism : SimpleAuthMechanism.values()) {
        if (simpleAuthMechanism.isImplemented()) {
          authResponse.add(simpleAuthMechanism.toString().toUpperCase());
        }
      }
      smtpReply.addHumanTextLine(String.join(" ", authResponse));
    }

    /**
     * Other SMTP capabilities, not supported and therefore not added
     * - ETRN
     */

    /**
     * We end with a `ok`
     */
    String ok = SmtpReplyCode.OK_250.getHumanText();
    return smtpReply.addHumanTextLine(ok);

  }
}
