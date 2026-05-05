package com.combostrap.smtp;


import io.vertx.core.buffer.Buffer;

/**
 * An input is a wrapper around data (ie a buffer) received on the Socket.
 * <p>
 * It may be:
 * * a command line or data
 * * in {@link #smtpInputType binary mime or text format}
 */
public class SmtpInput implements SmtpSessionInteraction {

  private final SmtpSession smtpSession;
  private final SmtpInputType smtpInputType;
  private final Buffer buffer;
  private SmtpInputCommand smtpCommand;

  public SmtpInput(SmtpSession smtpSession, Buffer buffer, SmtpInputType smtpInputType) {

    this.buffer = buffer;
    this.smtpInputType = smtpInputType;
    this.smtpSession = smtpSession;

  }

  public static SmtpInput create(SmtpSession smtpSession, Buffer buffer, SmtpInputType inputType) {

    return new SmtpInput(smtpSession, buffer, inputType);
  }

  public SmtpInputCommand getSmtpRequestCommand() throws SmtpException {
    /**
     * We capture the command in a field variable
     * because
     * the type of input (ie command or data is driven by the command)
     * <p>
     * If the command change it back to
     * {@link SmtpTransactionState#setReceptionModeToData()}
     * and we recreate the command, there is an error.
     */
    if(this.smtpCommand == null){
      this.smtpCommand = new SmtpInputCommand(this);
    }
    return this.smtpCommand;
  }

  public String getLine() {
    if (isBinary()) {
      return this.smtpInputType.toString();
    }
    return this.buffer.toString();
  }

  @Override
  public String toString() {
    if (isBinary()) {
      return this.smtpInputType.toString();
    }
    return getLine();
  }

  public SmtpSession getSession() {
    return this.smtpSession;
  }

  public boolean isBinary() {
    return this.smtpInputType.getIsBinary();
  }

  public Buffer getBuffer() {
    return this.buffer;
  }

  public SmtpInputType getInputType() {
    return this.smtpInputType;
  }

  @Override
  public String getSessionHistoryLine() {

    return getLine() + SmtpSyntax.LINE_DELIMITER;

  }
}
