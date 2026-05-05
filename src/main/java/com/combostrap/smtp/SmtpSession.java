package com.combostrap.smtp;

import com.combostrap.email.BMailInternetAddress;
import com.combostrap.smtp.command.SmtpBdatCommandHandler;
import com.combostrap.smtp.command.SmtpDataCommandHandler;
import com.combostrap.smtp.command.SmtpRsetCommandHandler;
import com.combostrap.type.DnsName;
import io.vertx.core.Handler;
import io.vertx.core.net.NetSocket;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.combostrap.smtp.SmtpCommand.QUIT;


/**
 * The session manages the sequence of {@link SmtpInput} and {@link SmtpReply}
 * to create a {@link SmtpTransactionState}
 * This is the state machine engine. The state data is defined in each {@link SmtpCommand}.
 * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.3.2">Sequence</a>
 */
public class SmtpSession implements Handler<List<SmtpInput>> {

    private final SmtpSocket smtpSocket;
    private final SmtpService smtpService;
    private SmtpTransactionState transactionState;
    private final SmtpSessionInteractionHistory sessionInteractionHistory;
    private int resetCount = 0;
    /**
     * Number of exception by session
     */
    private int exceptionCount = 0;

    /**
     * The authenticated user
     */
    private SmtpUser authenticatedUser;

    /**
     * The greeting information
     */
    private SmtpGreeting smtpGreeting;
    private SmtpParser parser;


    public SmtpSession(SmtpService smtpService, SmtpSocket netSocket) {

        this.smtpSocket = netSocket;
        this.smtpService = smtpService;


        this.sessionInteractionHistory = new SmtpSessionInteractionHistory(this);
        this.transactionState = SmtpTransactionState.create(this);

    }


    /**
     * Greeting is done at initial connection
     * and after a {@link com.combostrap.smtp.command.SmtpStartTlsCommandHandler StartTls command}
     * That's why we have a function
     */
    public void greet() throws SmtpException {

        this.smtpGreeting = SmtpGreeting.create(this);
        SmtpReply greeting = smtpGreeting.greet();
        reply(greeting);

    }


    /**
     * @return the smtp protocol written in mime message headers and in the greeting
     */
    String getSmtpProtocol() {

        StringBuilder smtpType = new StringBuilder();
        /**
         * E means extension
         */
        smtpType.append("ESMTP");
        if (isSsl()) {
            smtpType.append("S");
        }
        if (isAuthenticated()) {
            smtpType.append("A");
        }
        return smtpType.toString();

    }


    public static SmtpSession create(SmtpService smtpService, SmtpSocket smtpSocket) {

        return new SmtpSession(smtpService, smtpSocket);

    }


    /**
     * Note that multiple messages can
     * be delivered on a single connection using the {@link SmtpRsetCommandHandler RSET command}
     */
    public void start() {

        try {
            this.smtpService.getSmtpServer().connectionRateLimiter(this);
        } catch (SmtpException e) {
            this.handleException(e);
            return;
        }

        try {
            this.greet();
        } catch (SmtpException e) {
            this.handleException(e);
            return;
        }

        this.parser = SmtpParser.conf(this, this.smtpSocket.getNetSocket())
                .setOutputHandler(this)
                .setMaxBufferSize(this.getSmtpService().getSmtpServer().getMaxMessageSizeInBytes())
                .setEndStreamHandler(v -> this.endSocketHandler())
                .exceptionHandler(this::handleException)
                .build();

    }

    boolean isAuthenticated() {
        return this.authenticatedUser != null;
    }

    public boolean isAuthenticatedOrIsLocalhost() {
        if (
                getSmtpSocket().isRemoteLocalhost()
                        && !getSmtpService().getSmtpServer().getLocalHostAuthenticationRequired()) {
            return true;
        }
        return isAuthenticated();
    }

    private void replies(List<SmtpReply> replies) {
        StringBuilder stringBuilder = new StringBuilder();
        for (SmtpReply smtpReply : replies) {
            if (smtpReply == null) {
                /**
                 * {@link DATA command (DATA, BDAT) may have not any reply
                 * See {@link SmtpCommand#hasImmediateReply}
                 */
                continue;
            }
            this.sessionInteractionHistory.addInteraction(smtpReply);
            String replyLines = smtpReply.getReplyLines();
            stringBuilder.append(replyLines);
        }
        if (stringBuilder.length() != 0) {
            this.smtpSocket.write(stringBuilder.toString());
        }
    }


    /**
     * Handle exception has its own function
     * because the exception handler of the record parser
     * does not catch NPE (null pointer exception)
     * We catch therefore exception on 2 places
     */
    public void handleException(Throwable t) {

        /**
         * Log the exception
         */
        SmtpExceptionHandler.logTheException(t);
        this.exceptionCount++;

        /**
         * Reply with or without Quit.
         */
        String message = t.getMessage();
        if (message == null) {
            message = t.getClass().getSimpleName();
        }
        SmtpReply reply = SmtpReply.createForInternalException(message);
        boolean shouldQuit = true; // default we quit
        boolean shouldBeSilentQuit = false; // default we are not
        /**
         * Our smtp exception
         */
        if (t instanceof SmtpException) {
            SmtpException smtpException = (SmtpException) t;
            shouldQuit = smtpException.getShouldQuit();
            reply = smtpException.getReply();
            shouldBeSilentQuit = smtpException.getShouldBeSilentQuit();
        }

        if (this.exceptionCount > this.getSmtpService().getSmtpServer().maximumExceptionBySession()) {
            shouldQuit = true;
        }

        if (shouldQuit) {
            if (!shouldBeSilentQuit) {
                closeSessionWithReply(reply);
            } else {
                closeSession();
            }
            return;
        }

        reply(reply);


    }


    /**
     * Run when the {@link #smtpSocket connection socket} is closed
     * * by the client
     * * or by us with the {@link #closeSessionWithReply(SmtpReply)}
     */
    public void endSocketHandler() {

        /**
         * Run when:
         * * the client close the connection
         * * the {@link NetSocket#end()}} is called due to a {@link SmtpQuitCommandHandler quit command}
         */
        if (this.transactionState.getState() != QUIT) {
            /**
             * The client has closed the connection
             */
            this.sessionInteractionHistory.addInteraction(SmtpReply.create(SmtpReplyCode.INTERNAL_CLIENT_QUIT_999));
        }
        this.sessionInteractionHistory.endAndReplay();
        this.getSmtpService().getSmtpServer().removeSession(this);

    }

    public SmtpService getSmtpService() {
        return this.smtpService;
    }


    public LocalDateTime getLastInteractiveTime() {
        return this.sessionInteractionHistory.getLastInteractiveTime();
    }

    public SmtpSocket getSmtpSocket() {
        return this.smtpSocket;
    }

    public SmtpTransactionState getTransactionState() {
        return this.transactionState;
    }

    public SmtpSessionInteractionHistory getSessionHistory() {
        return this.sessionInteractionHistory;
    }


    public void reset() throws SmtpException {
        if (this.resetCount > 5) {
            throw SmtpException
                    .createTooMuchReset("Too much reset commands")
                    .setShouldQuit(true);
        }
        this.resetCount += 1;
        this.resetTransactionState();
        /**
         * The session state is not reset
         * ie {@link SmtpGreeting}
         * and {@link #isAuthenticated()}
         */
    }

    private void resetTransactionState() {
        this.transactionState = SmtpTransactionState.create(this);
        /**
         * The parser mode is the only state not in the transaction state object
         */
        this.parser.resetState();
    }

    /**
     * <a href="https://www.ietf.org/rfc/rfc821#section-4.1.1">section 4.1.1 of RFC 821</a>
     * The section 4.1.1 of RFC 821 states that disconnection
     * should only occur after a QUIT command is issued.
     * <p>
     * In the case of any error response,
     * the client SMTP should issue either the HELO or QUIT command.
     * <p>
     * See also: <a href="https://www.ietf.org/rfc/rfc1869.html#section-4.7">Responses from improperly implemented servers</a>
     */

    public void closeSessionWithReply(SmtpReply smtpReply) {
        this.reply(smtpReply);
        this.closeSession();
    }

    void closeSession() {
        /**
         * The close of the socket will call {@link #endSocketHandler()}
         */
        this.smtpSocket.close();
    }

    /**
     * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.2">Reply</a>
     * An SMTP reply consists of a three digit number (transmitted as three
     * numeric characters) followed by some text unless specified otherwise
     * in this document.  The number is for use by automata to determine
     * what state to enter next; the text is for the human user.
     * <p>
     * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.2.1">Multelines</a>
     * The format for multiline replies requires that every line, except the
     * last, begin with the reply code, followed immediately by a hyphen,
     * "-" (also known as minus), followed by text.  The last line will
     * begin with the reply code, followed immediately by <SP>, optionally
     * some text, and <CRLF>.  As noted above, servers SHOULD send the <SP>
     * if subsequent text is not sent, but clients MUST be prepared for it
     * to be omitted.
     * <p>
     * For example:
     * <p>
     * 123-First line
     * 123-Second line
     * 123-234 text beginning with numbers
     * 123 The last line
     */
    void reply(SmtpReply smtpReply) {
        replies(Collections.singletonList(smtpReply));
    }


    /**
     * Triggered by the DATA commands
     * {@link SmtpDataCommandHandler}
     * or {@link SmtpBdatCommandHandler}
     * when there is no data to receive anymore
     */
    public void messageReception() throws SmtpException {

        /**
         * Reception
         */
        this.smtpService.getSmtpServer().getSmtpReception().reception(this.transactionState);
        /**
         * Reset the transaction state
         */
        this.resetTransactionState();

    }


    public boolean isSsl() {
        return this.smtpSocket.getNetSocket().isSsl();
    }

    public void setAuthenticatedUser(SmtpUser smtpUser) {
        this.authenticatedUser = smtpUser;
    }

    public String getId() {
        return LocalDateTime.now() + SmtpSyntax.PART_SEP + this.getSmtpSocket().toString();
    }

    /**
     * First party cannot be determined at the {@link SmtpService service level}
     * because we need to know the requested host, and we get only with TLS
     */
    public boolean isFirstPartyEmail(BMailInternetAddress mailFromSender) {

        return this.getFirstPartyDomains().stream()
                .map(SmtpDomain::toString)
                .collect(Collectors.toList())
                .contains(mailFromSender.getDomainName().toStringWithoutRoot());

    }

    /**
     * First party cannot be determined at the {@link SmtpService service level}
     * because we need to know the requested host, and we get only with TLS
     */
    public Set<SmtpDomain> getFirstPartyDomains() {

        /**
         * Connected with ssl?
         */
        SmtpHost requestedHost = this.smtpGreeting.getRequestedHost().orElse(null);
        if (requestedHost != null) {
            return Collections.singleton(requestedHost.getDomain());
        }

        /**
         * All domains then
         */
        return this.getSmtpService().getSmtpServer().getHostedHosts()
                .values()
                .stream()
                .map(SmtpHost::getDomain)
                .collect(Collectors.toSet());

    }

    public SmtpGreeting getGreeting() {
        return this.smtpGreeting;
    }

    public SmtpUser checkUserExists(BMailInternetAddress address) throws SmtpException {
        String domain = address.getEmailAddress().getDomainName().toStringWithoutRoot();
        SmtpDomain smtpDomain = this.getFirstPartyDomain(domain).orElse(null);
        if(smtpDomain == null) {
            throw SmtpException.create(SmtpReplyCode.NO_SUCH_USER_550, address + " not found. Domain is not managed.");
        }
        SmtpUser user = smtpDomain.getUser(address.getEmailAddress().getLocalPart()).orElse(null);
        if (user != null) {
            return user;
        }
        throw SmtpException.create(SmtpReplyCode.NO_SUCH_USER_550, address + " not found");


    }

    private Optional<SmtpDomain> getFirstPartyDomain(String domain) {
        SmtpDomain value = this.getFirstPartyDomains()
                .stream()
                .filter(d -> {
                    DnsName dnsDomain = d.getDnsDomain();
                    return dnsDomain.toStringWithoutRoot().equals(domain);
                })
                .findFirst()
                .orElse(null);
        if (value == null) return Optional.empty();
        return Optional.of(value);
    }

    /**
     * @param smtpInputs the input (ie command line or data) returned by the {@link SmtpParser}
     */
    @Override
    public void handle(List<SmtpInput> smtpInputs) {

        try {


            /**
             * Process
             */
            List<SmtpReply> smtpReplies = new ArrayList<>();
            for (SmtpInput smtpInput : smtpInputs) {

                SmtpCommand requestCommand = smtpInput.getSmtpRequestCommand().getCommand();


                /**
                 * Non-public commands
                 */
                if (!requestCommand.isPublic()) {

                    /**
                     * Auth required ?
                     */
                    if (this.getSmtpService().isAuthRequired()
                            && !this.isAuthenticated()) {
                        this.reply(SmtpReply.create(SmtpReplyCode.AUTHENTICATION_REQUIRED_530));
                        return;
                    }

                    /**
                     * StartTls required ?
                     */
                    if (this.getSmtpService().isStartTlsRequired()
                            && !this.isSsl()) {
                        this.reply(SmtpReply.create(SmtpReplyCode.SSL_REQUIRED_538));
                        return;
                    }

                }

                /**
                 * The execution unit
                 */
                SmtpInputContext smtpInputContext = SmtpInputContext.create(this, smtpInput);

                /**
                 * Run/Execute command
                 */
                SmtpReply reply = smtpInputContext.execute();
                if (reply != null) {
                    /**
                     * {@link DATA command (DATA, BDAT) may have not any reply
                     * See {@link SmtpCommand#hasImmediateReply}
                     */
                    smtpReplies.add(reply);
                }

            }
            this.replies(smtpReplies);

            /**
             * Quit send a reply before closing the connection
             * That's why it's at the end.
             * The reply was already send above.
             * <p>
             * It is after the state management
             * because we need to be in the QUIT state
             */
            SmtpCommand lastCommand = smtpInputs.get(smtpInputs.size() - 1).getSmtpRequestCommand().getCommand();
            if (lastCommand.equals(QUIT)) {
                this.closeSession();
            }

        } catch (SmtpException e) {
            this.handleException(e);
        } catch (Exception e) {
            /**
             * NPE exception comes here
             */
            this.handleException(e);
        }
    }


    public SmtpParser getParser() {
        return this.parser;
    }
}
