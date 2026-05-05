package com.combostrap.smtp.command;

import com.combostrap.smtp.*;

public class NotImplementedCommandHandler extends SmtpInputCommandDirectReplyHandler {



  public NotImplementedCommandHandler(SmtpCommand smtpCommand) {
    super(smtpCommand);
  }

  public SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException {
    throw SmtpException.createNotSupportedImplemented("The command ("+this.getSmtpCommand()+") is not yet implemented");
  }

}
