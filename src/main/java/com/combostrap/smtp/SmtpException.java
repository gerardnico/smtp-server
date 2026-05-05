package com.combostrap.smtp;

public class SmtpException extends Exception {


  private final SmtpReplyCode smtpReplyCode;

  /**
   * The exception can impose a quit
   * We don't quit by default
   */
  private boolean shouldQuit = false;
  /**
   * When the user try to connect directly with an ip
   * or with a hostname that we don't host
   * we just close the connection without any reply
   */
  private boolean shouldBeSilentQuit;
  /**
   * A flag to see the bad behavior
   */
  private boolean badBehavior = false;

  public SmtpException(String message, Throwable cause, SmtpReplyCode smtpReplyCode) {
    super(message, cause);
    this.smtpReplyCode = smtpReplyCode;
  }


  public static SmtpException create(SmtpReplyCode smtpReplyCode, String humanText) {
    return new SmtpException(humanText, null, smtpReplyCode);
  }

  public static SmtpException create(SmtpReply smtpReply) {
    return new SmtpException(smtpReply.getReplyLines(), null, smtpReply.getSmtpReplyCode());
  }

  public static SmtpException create(SmtpReply smtpReply, Throwable e) {
    return new SmtpException(smtpReply.getReplyLines(), e, smtpReply.getSmtpReplyCode());
  }

  public static SmtpException create(SmtpReplyCode smtpReplyCode) {
    return new SmtpException(smtpReplyCode.getHumanText(), null, smtpReplyCode);
  }

  public static SmtpException createBadSyntax(String s) {
    return create(SmtpReplyCode.SYNTAX_ERROR_501, s);
  }

  public static SmtpException createBadSequence(String humanText) {
    return createBadSequence(humanText, null);
  }

  public static SmtpException createForInternalException(String humanText) {
    return createForInternalException(humanText, null);
  }

  public static SmtpException createForInternalException(String humanText, Throwable e) {
    return create(SmtpReply.createForInternalException(humanText), e);
  }

  public static SmtpException createNotSupportedImplemented(String humanText) {
    return create(
      SmtpReply.create(SmtpReplyCode.NOT_IMPLEMENTED_502, humanText)
    );
  }

  public static SmtpException createTooMuchReset(String tooMuchResetCommands) {
    return create(SmtpReply.create(SmtpReplyCode.TRANSACTION_FAILED_554, tooMuchResetCommands));
  }

  public static SmtpException create(String humanText, Throwable throwable, SmtpReplyCode smtpReplyCode) {
    return new SmtpException(humanText, throwable, smtpReplyCode);
  }

  public static SmtpException createBadSequence(String humanText, Throwable exception) {
    return create(
      SmtpReply.create(SmtpReplyCode.BAD_SEQUENCE_OF_COMMAND_503)
        .addHumanTextLine(humanText),
      exception
    );
  }

  public SmtpReply getReply() {
    return SmtpReply.create(this.smtpReplyCode, this.getMessage());
  }

  /**
   * @return true if the transaction should be terminated
   */
  public boolean getShouldQuit() {
    if (this.badBehavior) {
      return true;
    }
    if(this.smtpReplyCode == SmtpReplyCode.TRANSACTION_FAILED_554){
      // Internal error
      return true;
    }
    return this.shouldQuit;
  }

  public SmtpException setShouldQuit(boolean shouldQuit) {
    this.shouldQuit = shouldQuit;
    return this;
  }


  public SmtpException setShouldBeSilentQuit(boolean silent) {
    this.shouldBeSilentQuit = silent;
    return this;
  }

  public boolean getShouldBeSilentQuit() {
    return this.shouldBeSilentQuit;
  }

  public SmtpException setBadBehaviorFlag() {
    this.badBehavior = true;
    return this;
  }
}
