package com.combostrap.smtp;

public abstract class SmtpInputHandlerAbstract implements SmtpInputHandler {

  private final SmtpCommand smtpCommand;

  public SmtpInputHandlerAbstract(SmtpCommand smtpCommand) {
    this.smtpCommand = smtpCommand;
  }

  public SmtpCommand getSmtpCommand() {
    return smtpCommand;
  }


}
