package com.combostrap.smtp.command;

import com.combostrap.smtp.*;

/**
 * SMTP provides commands to verify a user name or obtain the content of
 * a mailing list.  This is done with the {@link SmtpVrfyCommandHandler} and EXPN commands, which
 * have character string arguments.
 * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-3.5">...</a>
 */
public class SmtpExpnCommandHandler extends SmtpInputCommandDirectReplyHandler {


  public SmtpExpnCommandHandler(SmtpCommand smtpCommand) {
    super(smtpCommand);
  }

  @Override
  public SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException {
    /**
     * When a name that is the argument to VRFY could identify more than one
     *    mailbox, the server MAY either note the ambiguity or identify the
     *    alternatives.  In other words, any of the following are legitimate
     *    response to VRFY:
     * <p>
     *       553 User ambiguous
     * <p>
     *    or
     * <p>
     *       553- Ambiguous;  Possibilities are
     *       553-Joe Smith <jsmith@foo.com>
     *       553-Harry Smith <hsmith@foo.com>
     *       553 Melvin Smith <dweep@foo.com>
     * <p>
     *    or
     * <p>
     *       553-Ambiguous;  Possibilities
     *       553- <jsmith@foo.com>
     *       553- <hsmith@foo.com>
     *       553 <dweep@foo.com>
     */
    throw SmtpException.createNotSupportedImplemented( "The command (" + this.getSmtpCommand() + ") is not yet implemented");
  }


}
