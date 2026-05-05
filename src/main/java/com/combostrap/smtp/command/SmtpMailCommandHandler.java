package com.combostrap.smtp.command;

import com.combostrap.email.BMailInternetAddress;

import com.combostrap.smtp.*;
import com.combostrap.type.CastException;
import com.combostrap.type.Casts;
import com.combostrap.type.Enums;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This command is used to initiate a mail transaction in which the mail
 * data is delivered to an SMTP server which may, in turn, deliver it to
 * one or more mailboxes or pass it on to another system (possibly using
 * SMTP).
 * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.1.1.2">...</a>
 */
public class SmtpMailCommandHandler extends SmtpInputCommandDirectReplyHandler {

    private static final Logger LOGGER = Logger.getLogger(SmtpMailCommandHandler.class.getName());

    private static final String FROM_WORD = "FROM:";

    public SmtpMailCommandHandler(SmtpCommand smtpCommand) {
        super(smtpCommand);
    }

    /**
     * Example: `MAIL FROM:<Smith@bar.com>`
     * <p>
     * Defined in <a href="https://www.rfc-editor.org/rfc/rfc1425.html#section-6">Section 6</a>
     * Syntax: MAIL FROM:<" reverse-path "> [esmtp-keyword=esmtp-value]* CRLF"
     */
    @Override
    public SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException {

        SmtpInputCommand smtpRequestCommand = smtpInputContext.getSmtpInput().getSmtpRequestCommand();

        /**
         * Minimum 1 argument: FROM:email`
         */
        List<String> arguments = smtpRequestCommand.getArguments();
        if (!(arguments.size() >= 1)) {
            throw SmtpException.createBadSyntax("The MAIL command has at minimum 1 arguments: " + this.getSmtpCommand().getCommandSyntax());
        }

        String from = arguments.get(0);
        if (from.equals(FROM_WORD)) {
            throw SmtpException.createBadSyntax("The MAIL command syntax has a `FROM:` in second position: " + this.getSmtpCommand().getCommandSyntax());
        }
        String reversePathEmail = from.substring(FROM_WORD.length());

        SmtpReply ok;

        SmtpPath email = SmtpPath.of(reversePathEmail);
        BMailInternetAddress sender = email.getInternetAddress(null).orElse(null);
        if (sender == null) {
            throw SmtpException.createForInternalException("Reception Error: with the mail from address (" + reversePathEmail + ")");
        }

        /**
         * Checks
         */
        SmtpSession smtpSession = smtpInputContext.getSession();
        SmtpFiltering.checkSender(smtpSession, sender);
        if (smtpSession.isFirstPartyEmail(sender)) {
            /**
             * Do we have the user
             */
            smtpSession.checkUserExists(sender);
        }


        /**
         * All good
         */
        smtpInputContext.getSessionState().setSender(sender);
        LOGGER.fine("The MAIl FROM forward path was added: " + sender);
        ok = SmtpReply.createOk(sender + "... Sender Ok");

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
                String value = parameterWords[1];
                switch (extensionParameter) {
                    case SIZE:
                        /**
                         * {@link SmtpExtensionParameter#SIZE}
                         * MAIL FROM:<ned@thor.innosoft.com> SIZE=500000
                         */
                        int size;
                        try {
                            size = Casts.cast(value, Integer.class);
                        } catch (CastException e) {
                            throw SmtpException.createBadSyntax("The size value (" + value + ") is not a valid integer");
                        }

                        int maxMessageSizeInBytes = smtpInputContext.getSmtpService().getSmtpServer().getMaxMessageSizeInBytes();
                        if (size > maxMessageSizeInBytes) {
                            throw SmtpException.create(SmtpReplyCode.MESSAGE_SIZE_EXCEED_LIMIT_552, "Message Size (" + size + ") exceeds max size (" + maxMessageSizeInBytes + ")");
                        }
                        smtpInputContext.getSessionState().setMessageSize(size);
                        LOGGER.fine("The message size was found: " + size);
                        break;
                    case BODY:
                        /**
                         * This parameter tells if the MIME message
                         * is completely in {@link SmtpInputType#BINARYMIME binary} or not
                         * <p>
                         * Example:
                         * MAIL FROM:<ned@ymir.claremont.edu> BODY=BINARYMIME
                         */
                        try {
                            SmtpInputType smtpInputType = Casts.cast(value, SmtpInputType.class);
                            if (smtpInputType.equals(SmtpInputType.TEXT_BIT7)) {
                                /**
                                 * We use the {@link io.vertx.core.parsetools.RecordParser}
                                 * that requires a 1-1 byte-char mapping
                                 */
                                throw SmtpException.createNotSupportedImplemented("We don't support " + SmtpInputType.TEXT_BIT7);
                            }
                            smtpInputContext.getSessionState().setBodyType(smtpInputType);
                        } catch (CastException e) {
                            throw SmtpException.createBadSyntax("The body value (" + value + ") is not a valid body value. " + Enums.toConstantAsStringCommaSeparated(SmtpInputType.class));
                        }
                        break;
                    case RET:
                        /**
                         * Not supported but we have some information here
                         */
                        if (Set.of("FULL", "HDRS").contains(value.toUpperCase())) {
                            throw SmtpException.createBadSyntax("The extension parameter value (" + value + ") is not valid");
                        }
                        throw SmtpException.createBadSyntax("The extension parameter (" + key + ") is not supported");
                    default:
                        throw SmtpException.createBadSyntax("The extension parameter (" + key + ") is not supported");
                }

            }

        }

        return ok;

    }


}
