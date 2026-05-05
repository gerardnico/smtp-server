package com.combostrap.smtp;


import com.combostrap.smtp.command.SmtpEhloCommandHandler;
import com.combostrap.smtp.command.SmtpHeloCommandHandler;

public abstract class SmtpInputCommandDirectReplyHandler extends SmtpInputHandlerAbstract {


  public SmtpInputCommandDirectReplyHandler(SmtpCommand smtpCommand) {
    super(smtpCommand);
  }

  /**
   * This function allows cross reply.
   * It's mostly use to get the reply of {@link SmtpEhloCommandHandler}
   * for {@link SmtpHeloCommandHandler}
   */
  public abstract SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException;

  @Override
  public void handle(SmtpInputContext smtpInputContext) throws SmtpException {
    smtpInputContext.reply(getReply(smtpInputContext));
  }

}
