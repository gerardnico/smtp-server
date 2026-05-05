package com.combostrap.smtp;

import com.combostrap.smtp.command.*;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

/**
 * The command with:
 * * the parameter syntax
 * * the state machine
 * * the handler
 * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.3.2">...</a>
 */
public enum SmtpCommand {

  DATA(
    "",
    Set.of(
      // first reply: acknowledge that data are coming
      SmtpReplyCode.START_MAIL_INPUT_354,
      // second reply: after data has been received
      SmtpReplyCode.OK_250
    ),
    Set.of(
      SmtpReplyCode.ERROR_IN_PROCESSING_451,
      SmtpReplyCode.TRANSACTION_FAILED_554,
      SmtpReplyCode.BAD_SEQUENCE_OF_COMMAND_503,
      // the below error are specific for data reception
      SmtpReplyCode.MESSAGE_SIZE_EXCEED_LIMIT_552,
      SmtpReplyCode.INSUFFISANT_STORAGE_452
    ),
    SmtpDataCommandHandler.class,
    false, // the first one but not the other
    false),
  STARTTLS(
    "",
    Set.of(
      SmtpReplyCode.GREETING_220
    ),
    Set.of(
      SmtpReplyCode.SYNTAX_ERROR_501,
      SmtpReplyCode.TLS_NOT_AVAILABLE_TEMPORARILY_454,
      SmtpReplyCode.AUTHENTICATION_REQUIRED_530
    ),
    SmtpStartTlsCommandHandler.class,
    false,
    false),
  RCPT(
    "TO:<recipient> [key=value ]*",
    Set.of(
      SmtpReplyCode.OK_250,
      SmtpReplyCode.CODE_251
    ),
    Set.of(
      SmtpReplyCode.NO_SUCH_USER_550,
      SmtpReplyCode.USER_NOT_LOCAL_551,
      SmtpReplyCode.MESSAGE_SIZE_EXCEED_LIMIT_552,
      SmtpReplyCode.USER_AMBIGUOUS_553,
      SmtpReplyCode.CODE_450,
      SmtpReplyCode.ERROR_IN_PROCESSING_451,
      SmtpReplyCode.INSUFFISANT_STORAGE_452,
      SmtpReplyCode.BAD_SEQUENCE_OF_COMMAND_503
    ),
    SmtpRcptCommandHandler.class,
    true, false),
  MAIL(
    "FROM:<sender> [key=value ]*",
    Set.of(SmtpReplyCode.OK_250),
    Set.of(
      SmtpReplyCode.MESSAGE_SIZE_EXCEED_LIMIT_552,
      SmtpReplyCode.ERROR_IN_PROCESSING_451,
      SmtpReplyCode.INSUFFISANT_STORAGE_452,
      SmtpReplyCode.NO_SUCH_USER_550,
      SmtpReplyCode.USER_AMBIGUOUS_553,
      SmtpReplyCode.BAD_SEQUENCE_OF_COMMAND_503
    ),
    SmtpMailCommandHandler.class,
    true, false),
  EHLO(
    // In EBNF: https://www.ietf.org/rfc/rfc1869.html#section-4.2
    // "EHLO" SP domain CR LF
    "<hostname>",
    Set.of(SmtpReplyCode.OK_250),
    Set.of(SmtpReplyCode.NO_SUCH_USER_550, SmtpReplyCode.NO_VALID_RECIPIENTS_504),
    SmtpEhloCommandHandler.class,
    // called normally at the beginning once but if you want to see it again, why not?
    true,
    true
  ),
  HELO(
    "<hostname>",
    Set.of(SmtpReplyCode.OK_250),
    Set.of(SmtpReplyCode.NO_SUCH_USER_550, SmtpReplyCode.NO_VALID_RECIPIENTS_504),
    SmtpHeloCommandHandler.class,
    // called normally at the beginning once but if you want to see it again, why not?
    true,
    true
  ),
  /**
   * Fake command to emulate the data reception state
   */

  RSET(
    "",
    Set.of(
      SmtpReplyCode.OK_250
    ),
    Set.of(),
    SmtpRsetCommandHandler.class,
    true,
    true
  ),
  VRFY(
    "",
    Set.of(
      SmtpReplyCode.OK_250,
      SmtpReplyCode.CODE_251,
      SmtpReplyCode.CODE_252
    ),
    Set.of(
      SmtpReplyCode.NO_SUCH_USER_550,
      SmtpReplyCode.USER_NOT_LOCAL_551,
      SmtpReplyCode.USER_AMBIGUOUS_553,
      SmtpReplyCode.NOT_IMPLEMENTED_502,
      SmtpReplyCode.NO_VALID_RECIPIENTS_504
    ),
    NotImplementedCommandHandler.class,
    true,
    false),
  EXPN(
    "",
    Set.of(
      SmtpReplyCode.OK_250,
      SmtpReplyCode.CODE_252
    ),
    Set.of(
      SmtpReplyCode.NO_SUCH_USER_550,
      SmtpReplyCode.NOT_RECOGNIZED_500,
      SmtpReplyCode.NOT_IMPLEMENTED_502,
      SmtpReplyCode.NO_VALID_RECIPIENTS_504
    ),
    SmtpExpnCommandHandler.class,
    true, false),
  HELP(
    "",
    Set.of(SmtpReplyCode.CODE_211, SmtpReplyCode.CODE_214),
    Set.of(SmtpReplyCode.NOT_IMPLEMENTED_502, SmtpReplyCode.NO_VALID_RECIPIENTS_504),
    NotImplementedCommandHandler.class,
    true,
    false),
  NOOP(
    "",
    Set.of(SmtpReplyCode.OK_250),
    Set.of(),
    SmtpNoopCommandHandler.class,
    true,
    true
  ),
  QUIT(
    "",
    Set.of(SmtpReplyCode.CLOSING_QUITING_221),
    Set.of(),
    SmtpQuitCommandHandler.class,
    true,
    true
  ),
  /**
   * BDAT = Binary data
   * alternate DATA command "BDAT" for efficiently sending large MIME messages.
   */
  BDAT(
    "",
    Set.of(SmtpReplyCode.OK_250),
    Set.of(
      SmtpReplyCode.ERROR_IN_PROCESSING_451,
      SmtpReplyCode.TRANSACTION_FAILED_554,
      SmtpReplyCode.BAD_SEQUENCE_OF_COMMAND_503,
      // the below error are specific for data reception
      SmtpReplyCode.MESSAGE_SIZE_EXCEED_LIMIT_552,
      SmtpReplyCode.INSUFFISANT_STORAGE_452
    ),
    SmtpBdatCommandHandler.class,
    // not as it must gather the binary data before giving a reply
    false,
    false),
  TURN(
    "",
    Set.of(),
    Set.of(),
    NotImplementedCommandHandler.class,
    true,
    false),
  SEND(
    "FROM",
    Set.of(),
    Set.of(),
    NotImplementedCommandHandler.class,
    true,
    false),
  AUTH(
    "mechanism [initial-response]",
    // https://datatracker.ietf.org/doc/html/rfc4954#section-6
    Set.of(
      SmtpReplyCode.SUCCESSFUL_AUTHENTICATION_235,
      SmtpReplyCode.READY_FOR_CREDENTIALS_334
    ),
    Set.of(
      SmtpReplyCode.CREDENTIAL_INVALID_535,
      SmtpReplyCode.SSL_REQUIRED_538,
      SmtpReplyCode.SYNTAX_ERROR_501,
      SmtpReplyCode.NOT_IMPLEMENTED_502
    ),
    SmtpAuthCommandHandler.class,
    true,
    true
  );
  private final Set<SmtpReplyCode> allAllowedReplyCodes;
  private final String syntaxArgumentDescription;

  private final SmtpInputHandler commandHandler;

  /**
   * All command gives an immediate reply except
   * for the data and bdata command
   * We cheat a little bit.
   */
  private final boolean hasImmediateReply;
  /**
   * A public command is a command
   * that does not require any authentication
   */
  private final boolean isPublicCommand;


  SmtpCommand(String syntaxArgumentDescription, Set<SmtpReplyCode> successReplyCodes, Set<SmtpReplyCode> errorReplyCodes, Class<? extends SmtpInputHandlerAbstract> smtpCommandHandler, boolean hasImmediateReply, boolean isPublicCommand) {

    this.allAllowedReplyCodes = new HashSet<>();
    this.allAllowedReplyCodes.addAll(successReplyCodes);
    this.allAllowedReplyCodes.addAll(errorReplyCodes);
    this.syntaxArgumentDescription = syntaxArgumentDescription;
    try {
      this.commandHandler = smtpCommandHandler.getDeclaredConstructor(SmtpCommand.class).newInstance(this);
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    this.hasImmediateReply = hasImmediateReply;
    this.isPublicCommand = isPublicCommand;

  }

  public Set<SmtpReplyCode> getAllowedReplyCodes() {
    return this.allAllowedReplyCodes;
  }


  public String getCommandSyntax() {
    return this.name() + " " + this.syntaxArgumentDescription;
  }

  public <T extends SmtpInputHandler> T getHandler(Class<T> clazz) {
    return clazz.cast(this.commandHandler);
  }

  public SmtpInputHandler getHandler() {
    return getHandler(SmtpInputHandler.class);
  }


  public boolean hasImmediateReply() {
    return this.hasImmediateReply;
  }


  public boolean isPublic() {
    return this.isPublicCommand;
  }

}
