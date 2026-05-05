package com.combostrap.smtp;

import java.util.List;
import java.util.Set;

/**
 * {@link SmtpExtensionParameter#PIPELINING}
 */
public class SmtpPipelining {

  /**
   * The commands that should be put aside
   * to be executed later in a pipeline stack.
   * In particular, the commands RSET, MAIL FROM,
   * SEND FROM, SOML FROM, SAML FROM, and RCPT TO can all appear anywhere
   * in a pipelined command group.
   * <a href="https://datatracker.ietf.org/doc/html/rfc2920#section-3.1">...</a>
   */
  public static final Set<SmtpCommand> COMMANDS_PIPELINED = Set.of(
    SmtpCommand.RSET,
    SmtpCommand.MAIL,
    SmtpCommand.RCPT,
    SmtpCommand.SEND
  );

  /**
   * The commands that start the pipeline stack
   * and executes each input
   * <p>
   * The EHLO, DATA, VRFY, EXPN, TURN, QUIT, and NOOP commands can
   * only appear as the last command in a group
   * <a href="https://datatracker.ietf.org/doc/html/rfc2920#section-3.1">...</a>
   */
  public static final Set<SmtpCommand> END_PIPELINED_COMMAND = Set.of(
    SmtpCommand.DATA,
    SmtpCommand.BDAT,
    SmtpCommand.EHLO,
    SmtpCommand.VRFY,
    SmtpCommand.EXPN,
    SmtpCommand.TURN,
    SmtpCommand.QUIT,
    SmtpCommand.NOOP
  );



  public static void checkStateIfPipeline(List<SmtpInput> smtpInputs) throws SmtpException {

    for (int i = 0; i < smtpInputs.size(); i++) {
      SmtpCommand requestCommand = smtpInputs.get(i).getSmtpRequestCommand().getCommand();
      boolean isLastCommand = i != smtpInputs.size() - 1;
      if (!isLastCommand && END_PIPELINED_COMMAND.contains(requestCommand)) {
        throw SmtpException.create(SmtpReplyCode.BAD_SEQUENCE_OF_COMMAND_503, "The command " + requestCommand + " may appear only at the end of a pipelined group");
      }
      if (!(
        END_PIPELINED_COMMAND.contains(requestCommand)
          || COMMANDS_PIPELINED.contains(requestCommand)
      )) {
        throw SmtpException.create(SmtpReplyCode.BAD_SEQUENCE_OF_COMMAND_503, "The command " + requestCommand + " is not a command that can be pipelined");
      }
    }
  }
}
