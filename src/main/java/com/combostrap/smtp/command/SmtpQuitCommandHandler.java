package com.combostrap.smtp.command;

import com.combostrap.smtp.*;

public class SmtpQuitCommandHandler extends SmtpInputCommandDirectReplyHandler {


  public SmtpQuitCommandHandler(SmtpCommand smtpCommand) {
    super(smtpCommand);
  }

  public SmtpReply getReply(SmtpInputContext smtpInputContext) {

    String domainOrHostname = smtpInputContext.getSession().getGreeting().getRequestedHostOrDefault().getDomain().toString();
    return SmtpReply.create(SmtpReplyCode.CLOSING_QUITING_221, domainOrHostname + " Service closing transmission channel");

  }


}
