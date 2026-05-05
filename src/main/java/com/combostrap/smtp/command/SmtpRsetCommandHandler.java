package com.combostrap.smtp.command;

import com.combostrap.smtp.*;

public class SmtpRsetCommandHandler extends SmtpInputCommandDirectReplyHandler {


  public SmtpRsetCommandHandler(SmtpCommand smtpCommand) {
    super(smtpCommand);
  }

  public SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException {
    smtpInputContext.getSession().reset();
    return SmtpReply.createOk();
  }


}
