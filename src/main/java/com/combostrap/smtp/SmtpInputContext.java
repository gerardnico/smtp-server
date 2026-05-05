package com.combostrap.smtp;

/**
 * An execution unit and context to {@link #execute()}
 * It permits to reduce the number of method in the run
 * to be easier in the implementation
 * It's used in the {@link SmtpInputHandler} to send an {@link SmtpInputHandler#handle(SmtpInputContext)} event
 */
public class SmtpInputContext {


  private final SmtpSession session;
  private final SmtpInput smtpInput;

  private SmtpReply reply;




  public SmtpInputContext(SmtpSession session, SmtpInput smtpInput) {
    this.session = session;
    this.smtpInput = smtpInput;
  }

  public static SmtpInputContext create(SmtpSession smtpSession, SmtpInput smtpInput) {
    return new SmtpInputContext(smtpSession, smtpInput);
  }


  public SmtpReply execute() throws SmtpException {

    /**
     * Check command sequence - Order of Commands
     */
    this.getSessionState().checkStateMachine(smtpInput);

    /**
     * Get command
     */
    SmtpCommand requestCommand = smtpInput.getSmtpRequestCommand().getCommand();

    /**
     * Handle
     */
    requestCommand.getHandler().handle(this);

    SmtpReply smtpReply = this.getReply();

    /**
     * Event handler may return a reply
     * The {@link com.combostrap.smtp.command.DataReceptionHandler}
     * returns a reply only when the data reception is finished
     */
    if (smtpReply == null) {

      if (requestCommand.hasImmediateReply()) {

        throw SmtpException.createForInternalException("The handler for the command (" + requestCommand + ") did not return a SMTP reply");

      }

    } else {

      /**
       * Did we get a correct reply?
       */
      SmtpReplyCode smtpReplyCode = smtpReply.getSmtpReplyCode();
      if (!requestCommand.getAllowedReplyCodes().contains(smtpReplyCode)) {
        throw SmtpException.createForInternalException("The reply code (" + smtpReplyCode + ") is not allowed for the command (" + requestCommand + ")");
      }

    }

    /**
     * Update the state
     */
    smtpInput.getSession().getTransactionState().setNewState(requestCommand);


    return reply;
  }

  SmtpReply getReply() {
    return this.reply;
  }



  public SmtpTransactionState getSessionState() {
    return session.getTransactionState();
  }

  public void reply(SmtpReply smtpReply) {
    this.reply = smtpReply;
  }

  public SmtpService getSmtpService() {
    return this.session.getSmtpService();
  }

  public SmtpSession getSession() {
    return this.session;
  }


  public void endMessage() throws SmtpException {
    session.messageReception();
  }


  public boolean isSsl() {
    return this.session.isSsl();
  }

  public SmtpInput getSmtpInput() {
    return this.smtpInput;
  }


}
