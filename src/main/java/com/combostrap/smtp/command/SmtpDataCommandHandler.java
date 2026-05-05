package com.combostrap.smtp.command;

import com.combostrap.smtp.*;

public class SmtpDataCommandHandler extends SmtpInputCommandDirectReplyHandler {


  private static final String END_OF_BODY = ".";

  public SmtpDataCommandHandler(SmtpCommand smtpCommand) {
    super(smtpCommand);
  }


  @Override
  public SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException {
    /**
     * DATA is two state stages
     */
    SmtpTransactionState sessionState = smtpInputContext.getSessionState();

    if (sessionState.isDataReceptionMode()) {

      /**
       * Error can be:
       * {@link SmtpReplyCode.MESSAGE_SIZE_EXCEED_LIMIT_552}
       * {@link SmtpReplyCode.TRANSACTION_FAILED_554}
       * {@link SmtpReplyCode.ERROR_IN_PROCESSING_451}
       * {@link SmtpReplyCode.CODE_452}
       */
      SmtpInput smtpInput = smtpInputContext.getSmtpInput();

      SmtpInputType inputType = smtpInput.getInputType();
      if (!inputType.equals(SmtpInputType.TEXT_BIT8)) {
        throw SmtpException.createBadSequence("The body type with a DATA command should be (" + SmtpInputType.TEXT_BIT8 + "), not " + inputType + ".");
      }

      String line = smtpInput.getLine();

      /**
       * End of data sequence
       */
      if (line.equals(END_OF_BODY)) {

        smtpInputContext.endMessage();
        return SmtpReply.createOk();

      }

      sessionState.addLineToBodyData(line);
      return null;

    }


    sessionState.setReceptionModeToData();
    return SmtpReply.create(SmtpReplyCode.START_MAIL_INPUT_354);
  }

}
