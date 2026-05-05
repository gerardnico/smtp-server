package com.combostrap.smtp.command;

import com.combostrap.smtp.*;
import com.combostrap.type.CastException;
import com.combostrap.type.Casts;


import java.util.List;
import java.util.logging.Logger;

public class SmtpRcptCommandHandler extends SmtpInputCommandDirectReplyHandler {

  private static final Logger LOGGER = Logger.getLogger(SmtpRcptCommandHandler.class.getName());
  private static final String TO_WORD = "TO:";

  public SmtpRcptCommandHandler(SmtpCommand smtpCommand) {
    super(smtpCommand);
  }

  /**
   * <a href="https://www.rfc-editor.org/rfc/rfc5321.html#section-4.1.1.3">...</a>
   * <p>
   * EBNF = "RCPT TO:" ( "<Postmaster@" Domain ">" / "<Postmaster>" /
   * Forward-path ) [SP Rcpt-parameters] CRLF
   */
  public SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException {
    /**
     * Minimum 1 argument: `TO:email`
     */
    SmtpInputCommand smtpRequestCommand = smtpInputContext.getSmtpInput().getSmtpRequestCommand();
    List<String> arguments = smtpRequestCommand.getArguments();
    if (arguments.isEmpty()) {
      throw SmtpException.createBadSyntax("The " + this.getSmtpCommand() + " command has at minima 1 argument: " + this.getSmtpCommand().getCommandSyntax());
    }

    String to = arguments.get(0);
    if (to.equals(TO_WORD)) {
      throw SmtpException.createBadSyntax("The " + this.getSmtpCommand() + " command syntax has a `TO:` in second position: " + this.getSmtpCommand().getCommandSyntax());
    }

    /**
     * <forward-path> is normally a mailbox and domain, always surrounded by "<" and ">"
     *    brackets) identifying one recipient.
     * https://www.rfc-editor.org/rfc/rfc5321.html#section-3.3
     * <p>
     * Note implemented: The <forward-path> can contain more than just a mailbox.
     * Historically, the <forward-path> was permitted to contain a source
     * routing list of hosts and the destination mailbox; however,
     * contemporary SMTP clients SHOULD NOT utilize source routes (see
     * Appendix C).  Servers MUST be prepared to encounter a list of source
     * routes in the forward-path, but they SHOULD ignore the routes or MAY
     * decline to support the relaying they imply
     */
    String forwardPathString = to.substring(TO_WORD.length());
    SmtpRecipient smtpRecipient = SmtpRecipient.createFrom(smtpInputContext.getSession(),forwardPathString);

    smtpInputContext.getSessionState().addRecipient(smtpRecipient);
    LOGGER.fine("A RCP TO forward path was added: " + smtpRecipient);

    /**
     * There may be also {@link  SmtpExtensionParameter parameter}
     * if we need to implement them,
     * the code is already available in the {@link SmtpMailCommandHandler}
     */
    if (arguments.size() >= 2) {

      List<String> eSmtpParameters = arguments.subList(1, arguments.size());
      for (String esmtpParameter : eSmtpParameters) {
        String[] parameterWords = esmtpParameter.split("=", 2);
        if (parameterWords.length != 2) {
          throw SmtpException.createBadSyntax("The extension parameter (" + esmtpParameter + ") should be in the form key=value");
        }
        String key = parameterWords[0].toUpperCase();
        SmtpExtensionParameter extensionParameter;
        try {
          extensionParameter = Casts.cast(key, SmtpExtensionParameter.class);
        } catch (CastException e) {
          throw SmtpException.createBadSyntax("The extension parameter (" + key + ") is not supported");
        }
        // String value = parameterWords[1];
        switch (extensionParameter) {
          case NOTIFY:
            /**
             * NOTIFY=success,failure
             */
            throw SmtpException.createBadSyntax("The extension parameter (" + key + ") is not yet supported");
          default:
            throw SmtpException.createBadSyntax("The extension parameter (" + key + ") is not supported");
        }

      }

    }
    return SmtpReply.createOk(smtpRecipient + "... Recipient Ok");

  }

}
