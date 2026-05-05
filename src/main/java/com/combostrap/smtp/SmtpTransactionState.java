package com.combostrap.smtp;

import com.combostrap.email.BMailInternetAddress;
import com.combostrap.smtp.command.SmtpBdatCommandHandler;
import com.combostrap.smtp.command.SmtpDataCommandHandler;
import com.combostrap.smtp.command.SmtpRcptCommandHandler;
import com.combostrap.smtp.command.SmtpRsetCommandHandler;
import com.combostrap.smtp.sasl.SimpleAuthMechanism;
import com.combostrap.type.DnsName;
import io.vertx.core.buffer.Buffer;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An object to hold the whole transaction (ie state) and
 * create the final Email message
 * in the {@link SmtpSession SMTP sequence}
 * When a {@link SmtpRsetCommandHandler} is executed,
 * the while object is recreated
 */
public class SmtpTransactionState {

  private SmtpCommand actualCommandState;

  private BMailInternetAddress sender;
  private Integer messageSizeFromMailCommand;
  private final SmtpSession smtpSession;
  private final Set<SmtpRecipient> recipients = new HashSet<>();

  /**
   * The type of the data (ie body)
   */
  private SmtpInputType smtpBodyInputType = SmtpInputType.TEXT_BIT8;


  /**
   * Buffer data received with the {@link SmtpBdatCommandHandler bdat command}
   * may be {@link SmtpInputType#BINARYMIME} binary
   * or {@link SmtpInputType#TEXT_BIT8} text
   */
  private Buffer textMessage = Buffer.buffer();

  /**
   * Indicate if the last {@link SmtpBdatCommandHandler BDAT}
   * segment was received
   */
  private boolean lastBdat = false;

  private SmtpInputReceptionMode smtpInputReceptionMode = SmtpInputReceptionMode.COMMAND;
  private SmtpInputType smtpInputType = SmtpInputType.TEXT_BIT8;

  /**
   * The asked auth mechanism waiting for credentials
   */
  private SimpleAuthMechanism authMechanism;
  private DnsName ehloClientHostname;
  /**
   * The chunk size given in the bdat command
   */
  private Long bdatChunkSize;


  public SmtpTransactionState(SmtpSession smtpSession) {
    this.smtpSession = smtpSession;
  }

  public static SmtpTransactionState create(SmtpSession smtpSession) {
    return new SmtpTransactionState(smtpSession);
  }


  public void setSender(BMailInternetAddress mailFromAddress) {
    this.sender = mailFromAddress;
  }

  public void setMessageSize(int size) {
    this.messageSizeFromMailCommand = size;
  }


  public void setNewState(SmtpCommand smtpCommand) {
    this.actualCommandState = smtpCommand;
  }


  /**
   * A state machine
   * We can't create it in the {@link SmtpCommand}
   * because command such as {@link SmtpRcptCommandHandler} refer to themselves
   * <p>
   * Note that the state has also a mode {@link SmtpInputReceptionMode}
   * used by the command to make the difference between a:
   * * input command
   * * input data
   * This is used by data command such as {@link SmtpDataCommandHandler}
   * and {@link SmtpBdatCommandHandler} to handle their two stages
   * processing
   * <p>
   * ie the first {@link SmtpCommand} may occur after one of the others command {@link SmtpCommand}
   */
  private static final Map<SmtpCommand, Set<SmtpCommand>> stateMachine = new HashMap<>();

  static {
    /**
     * Check command sequence - Order of Commands
     * <a href="https://datatracker.ietf.org/doc/html/rfc2821#page-39">...</a>
     * <a href="https://datatracker.ietf.org/doc/html/rfc2821#appendix-D">...</a> - Scenario
     * <p>
     * In any event, a client MUST issue HELO or EHLO before starting a mail transaction.
     * https://datatracker.ietf.org/doc/html/rfc2821#section-4.1.1.1
     * <p>
     * A session to start with either EHLO or HELO.
     * https://www.ietf.org/rfc/rfc1869.html#section-4.1.1 - First Command
     */
    stateMachine.put(SmtpCommand.STARTTLS, Set.of(SmtpCommand.EHLO, SmtpCommand.HELO));
    stateMachine.put(SmtpCommand.AUTH, Set.of(SmtpCommand.EHLO, SmtpCommand.HELO, SmtpCommand.STARTTLS));
    stateMachine.put(SmtpCommand.MAIL, Set.of(SmtpCommand.EHLO, SmtpCommand.HELO, SmtpCommand.STARTTLS, SmtpCommand.AUTH));
    stateMachine.put(SmtpCommand.RCPT, Set.of(SmtpCommand.MAIL, SmtpCommand.RCPT));
    stateMachine.put(SmtpCommand.DATA, Set.of(SmtpCommand.DATA, SmtpCommand.RCPT));
    stateMachine.put(SmtpCommand.BDAT, Set.of(SmtpCommand.BDAT, SmtpCommand.RCPT));

  }


  public void checkStateMachine(SmtpInput smtpInput) throws SmtpException {

    SmtpCommand requestedCommand = smtpInput.getSmtpRequestCommand().getCommand();

    Set<SmtpCommand> previousStates = stateMachine.get(requestedCommand);
    if (previousStates == null) {
      /**
       * Stateless command
       */
      return;
    }

    /**
     * Non-Stateless command
     */
    if (previousStates.contains(actualCommandState)) {
      return;
    }

    String previousCommands = previousStates.stream().map(SmtpCommand::toString).collect(Collectors.joining(", "));
    throw SmtpException.createBadSequence("The " + requestedCommand + " command may not occur after the command (" + actualCommandState + "). Only this commands may be send: " + previousCommands);

  }

  public void addRecipient(SmtpRecipient smtpRecipient) throws SmtpException {
    /**
     * Quota
     */
    SmtpService smtpService = this.smtpSession.getSmtpService();
    int maxRecipients = smtpService.getSmtpServer().getMaxRecipientsByEmail();
    if (recipients.size() > maxRecipients) {
      throw SmtpException.create(SmtpReplyCode.INSUFFISANT_STORAGE_452, "Too many recipients (max:" + maxRecipients + ")");
    }
    this.recipients.add(smtpRecipient);
  }

  public SmtpCommand getState() {
    return this.actualCommandState;
  }

  /**
   * In textual mode, we gather the data with this
   * function
   */
  public void addLineToBodyData(String line) throws SmtpException {
    /**
     * The last line of data is a line delimiter with a point  {@link SmtpDataCommandHandler#END_OF_BODY}
     * We should not add the last end of line delimiter
     */
    if (this.textMessage.length() == 0) {
      this.textMessage.appendString(line);
    } else {
      this.textMessage.appendString(SmtpSyntax.LINE_DELIMITER + line);
    }
    this.checkBodySize();

  }

  private void checkBodySize() throws SmtpException {
    int messageSize = this.textMessage.length();
    int maxMessageSizeInBytes = this.smtpSession.getSmtpService().getSmtpServer().getMaxMessageSizeInBytes();
    if (messageSize > maxMessageSizeInBytes) {
      throw SmtpException
        .create(SmtpReplyCode.MESSAGE_SIZE_EXCEED_LIMIT_552, "Message Size (" + messageSize + ") exceeds max size (" + maxMessageSizeInBytes + ")")
        .setShouldQuit(true);
    }
  }

  public void addTextualBufferToBodyData(Buffer buffer) throws SmtpException {
    if (!this.smtpBodyInputType.equals(SmtpInputType.TEXT_BIT8)) {
      throw SmtpException.createNotSupportedImplemented("The body type (" + this.smtpBodyInputType + ") is not supported");
    }
    if (this.textMessage == null) {
      this.textMessage = buffer;
      return;
    }
    this.textMessage.appendBuffer(buffer);
    this.checkBodySize();
  }

  public void setBdatLast(boolean last) {
    this.lastBdat = last;
  }

  public boolean getLastBdat() {
    return this.lastBdat;
  }

  /**
   * The type of the {@link SmtpExtensionParameter#BODY}
   */
  public SmtpInputType getBodyInputType() {
    return this.smtpBodyInputType;
  }

  public void setBodyType(SmtpInputType smtpInputType) {
    this.smtpBodyInputType = smtpInputType;
  }

  public Buffer getBinaryDataReceived() {
    return this.textMessage;
  }


  /**
   * A command may set that the next input is data
   */
  public boolean isDataReceptionMode() {
    return this.smtpInputReceptionMode.equals(SmtpInputReceptionMode.DATA);
  }

  /**
   * A command may set that the next input is a command
   */
  public SmtpTransactionState setReceptionModeToCommand() {
    this.smtpInputReceptionMode = SmtpInputReceptionMode.COMMAND;
    return this;
  }

  public SmtpTransactionState setReceptionModeToData() {
    this.smtpInputReceptionMode = SmtpInputReceptionMode.DATA;
    return this;
  }

  public SmtpInputType getInputType() {
    return this.smtpInputType;
  }

  public void setInputType(SmtpInputType smtpInputType) {
    this.smtpInputType = smtpInputType;
  }


  public SmtpTransactionState setAuthMechanism(SimpleAuthMechanism simpleAuthMechanism) {
    this.authMechanism = simpleAuthMechanism;
    return this;
  }

  public SimpleAuthMechanism getAuthMechanism() {
    return this.authMechanism;
  }


  public Integer getMessageSizeFromMailCommand() {
    return this.messageSizeFromMailCommand;
  }

  public Buffer getMessage() {
    return this.textMessage;
  }

  public Set<SmtpRecipient> getRecipients() {
    return this.recipients;
  }

  public BMailInternetAddress getSender() {
    return this.sender;
  }

  public DnsName getEhloClientHostName() {
    return this.ehloClientHostname;
  }

  public void setEhloClientHostName(DnsName ehloDomainName) {
    this.ehloClientHostname = ehloDomainName;
  }

  public SmtpSession getSession() {
    return this.smtpSession;
  }

  public void setBdatChunkSize(Long chunkSize) {
    this.bdatChunkSize = chunkSize;
  }

  public void setParserModeFixedBytes(Long chunkSize) {
    this.smtpSession.getParser().setParserModeToFix(chunkSize);
  }

  public void setParserModeFixedToLine() {
    this.smtpSession.getParser().setParserModeToLine();
  }

  public Long getExpectedBdatChunkSize() {
    return this.bdatChunkSize;
  }
}
