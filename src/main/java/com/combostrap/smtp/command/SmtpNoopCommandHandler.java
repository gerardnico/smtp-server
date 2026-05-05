package com.combostrap.smtp.command;


import com.combostrap.smtp.*;
import org.eclipse.angus.mail.smtp.SMTPTransport;

/**
 * No operation: NOOP
 * {@link SMTPTransport#isConnected()} sends a NOOP command.
 * to verify the connection status
 */
public class SmtpNoopCommandHandler extends SmtpInputCommandDirectReplyHandler {


    public SmtpNoopCommandHandler(SmtpCommand smtpCommand) {
        super(smtpCommand);
    }

    public SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException {
        return SmtpReply.createOk();
    }


}
