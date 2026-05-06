package com.combostrap.smtp.command;


import com.combostrap.smtp.*;
import com.combostrap.smtp.sasl.SimpleAuthException;
import com.combostrap.smtp.sasl.SimpleAuthMechanism;
import com.combostrap.type.Base64Utility;
import com.combostrap.type.CastException;
import com.combostrap.type.Casts;
import com.combostrap.type.Enums;

import java.util.List;

/**
 * Auth command
 * <a href="https://datatracker.ietf.org/doc/html/rfc4954">Auth</a>
 */
public class SmtpAuthCommandHandler extends SmtpInputCommandDirectReplyHandler {


    public SmtpAuthCommandHandler(SmtpCommand smtpCommand) {
        super(smtpCommand);
    }

    /**
     * <a href="https://datatracker.ietf.org/doc/html/rfc4954#section-4">Syntax</a>
     * `AUTH mechanism [initial-response]`
     * where mechanism is a <a href="https://datatracker.ietf.org/doc/html/rfc4422">SASL</a>
     * List: <a href="https://www.iana.org/assignments/sasl-mechanisms/sasl-mechanisms.xhtml">SASL List</a>
     * <p>
     * Reply code can be seen here: <a href="https://datatracker.ietf.org/doc/html/rfc4954#section-6"></a>
     */
    public SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException {

        if (!smtpInputContext.getSmtpService().isAuthEnabled()) {
            throw SmtpException.createNotSupportedImplemented("Auth is not enabled");
        }

        if (!smtpInputContext.isSsl()) {
            throw SmtpException.create(SmtpReplyCode.SSL_REQUIRED_538);
        }

        SmtpSession session = smtpInputContext.getSession();
        List<String> arguments = smtpInputContext.getSmtpInput().getSmtpRequestCommand().getArguments();

        /**
         * Credentials reception mode?
         * Example from https://datatracker.ietf.org/doc/html/rfc4954#section-4.1
         * C: AUTH PLAIN
         * S: 334  <-- there is a single space following the 334
         * C: dGVzdAB0ZXN0ADEyMzQ=
         */
        SmtpTransactionState sessionState = smtpInputContext.getSessionState();
        if (sessionState.isDataReceptionMode()) {
            sessionState.setReceptionModeToCommand();
            String token = smtpInputContext.getSmtpInput().getBuffer().toString();
            return this.authenticate(session, sessionState.getAuthMechanism(), token);
        }

        if (arguments.isEmpty()) {
            throw SmtpException.createBadSyntax("The command (" + SmtpCommand.AUTH + ") has at minimum one argument the Auth mechanism. The syntax is: " + SmtpCommand.AUTH.getCommandSyntax());
        }

        SimpleAuthMechanism simpleAuthMechanism;
        String mechanismString = arguments.get(0).toUpperCase();
        try {
            simpleAuthMechanism = Casts.cast(mechanismString, SimpleAuthMechanism.class);
        } catch (CastException e) {
            throw SmtpException.createNotSupportedImplemented("The auth mechanism (" + mechanismString + ") is not supported. The supported mechanism are " + Enums.toConstantAsStringCommaSeparated(SimpleAuthMechanism.class));
        }

        if (arguments.size() == 1) {
            sessionState.setAuthMechanism(simpleAuthMechanism);
            sessionState.setReceptionModeToData();
            return SmtpReply.create(SmtpReplyCode.READY_FOR_CREDENTIALS_334, " ");
        }

        return this.authenticate(session, simpleAuthMechanism, arguments.get(1));

    }

    /**
     * Example for plain from <a href="https://datatracker.ietf.org/doc/html/rfc4954#section-4.1">...</a>
     * Example 1:
     * AUTH PLAIN dGVzdAB0ZXN0ADEyMzQ=
     */
    private SmtpReply authenticate(SmtpSession smtpSession, SimpleAuthMechanism simpleAuthMechanism, String token) throws SmtpException {
        try {
            /**
             * The credentials data is always Base 64
             * as seen in the AUTH EBNF syntax
             * https://datatracker.ietf.org/doc/html/rfc4954#section-8
             */
            String credential;
            try {
                credential = Base64Utility.base64UrlStringToString(token);
            } catch (CastException e) {
                SmtpReply smtpReply = SmtpReply.create(SmtpReplyCode.CREDENTIAL_INVALID_535, "Credentials Invalid: They should be encoded in base 64. " + e.getMessage());
                throw SmtpException.create(smtpReply, e);
            }
            SmtpMxHost requestedHost = smtpSession.getGreeting().getRequestedHost().orElse(null);
            if (requestedHost == null) {
                // It should not happen as this check beforehand
                throw SmtpException.create(SmtpReplyCode.SSL_REQUIRED_538, "To login, TLS is required. Use the port 587 or the command STARTTLS");
            }
            SmtpDomain domain = requestedHost.getDomain();

            SmtpUser smtpUser = simpleAuthMechanism.getHandler().authenticate(domain, credential);
            smtpSession.setAuthenticatedUser(smtpUser);
            return SmtpReply.create(SmtpReplyCode.SUCCESSFUL_AUTHENTICATION_235);
        } catch (SimpleAuthException e) {
            SmtpReply smtpReply = SmtpReply.create(SmtpReplyCode.CREDENTIAL_INVALID_535, "Credentials Invalid: " + e.getMessage());
            throw SmtpException.create(smtpReply, e);
        }
    }

}
