package com.combostrap.smtp.command;

import com.combostrap.smtp.*;

/**
 * A `Hello` command is started by a client that does not support {@link SmtpExtensionParameter SMTP service extensions}
 * <p>
 * See: <a href="https://www.ietf.org/rfc/rfc1869.txt">...</a> - Section 4 extensions
 * A client SMTP supporting {@link SmtpExtensionParameter SMTP service extensions}
 * should start an SMTP session by issuing the {@link SmtpEhloCommandHandler EHLO command} instead of the HELO command.
 */
public class SmtpHeloCommandHandler extends SmtpInputCommandDirectReplyHandler {


  public SmtpHeloCommandHandler(SmtpCommand smtpCommand) {
    super(smtpCommand);
  }

  @Override
  public SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException {

    return SmtpCommand.EHLO
      .getHandler(SmtpInputCommandDirectReplyHandler.class)
      .getReply(smtpInputContext);


  }

}
