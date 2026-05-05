package com.combostrap.smtp.command;

import com.combostrap.smtp.*;
import com.combostrap.type.CastException;
import com.combostrap.type.Casts;
import io.vertx.core.buffer.Buffer;

import java.util.List;

/**
 * BDAT = Binary Data
 * alternate DATA command "BDAT" for efficiently sending large MIME messages.
 * <a href="https://datatracker.ietf.org/doc/html/rfc1830">...</a>
 * <p>
 * Example:
 * <a href="https://datatracker.ietf.org/doc/html/rfc1830#section-5">...</a>
 * The need is to avoid the overhead of
 * base64 and quoted-printable encoding of binary objects sent using the
 * MIME message format over SMTP between hosts which support binary
 * message processing.
 */
public class SmtpBdatCommandHandler extends SmtpInputCommandDirectReplyHandler {


  public SmtpBdatCommandHandler(SmtpCommand smtpCommand) {
    super(smtpCommand);
  }

  /**
   * How it works?
   * Request:
   * The BDAT command takes one argument, the exact length of the binary data segment in octets.
   * The message data is sent with the BDAT command.
   * Once the receiver-SMTP receives the specified number of octets, it will return a 250 reply code.
   * The LAST parameter on the BDAT command indicates that this is the last chunk of message data to be sent.
   * Response:
   * A 250 response should be sent to each BDAT data block.  If a 5XX code
   * is sent in response to a BDAT chunk the message should be considered failed and,
   * the sender SMTP must not send any additional BDAT segments.
   * Error: Any BDAT command sent after the BDAT LAST is illegal and must be replied to with a 503 "Bad sequence of commands" reply code.
   * The state resulting from this error is indeterminate. A RSET command must be sent to clear the transaction before continuing.
   * <p>
   * Example:
   * Sending one chunk of 69 octets:
   * ```
   * Input: BDAT 69 LAST
   * Reply: 250 Message OK, 69 octets received
   * ```
   * Sending 2 chunk:
   * ```
   * Input: BDAT 100000
   * Reply: 250 100000 bytes received
   * Input: BDAT 324 LAST
   * Reply: 250 Message OK, 100324 octets received
   * ```
   * <p>
   * Syntax:
   * bdat-cmd   ::= "BDAT" SP chunk-size [ SP end-marker ] CR LF
   * chunk-size ::= 1*DIGIT
   * end-marker ::= "LAST"
   * <p>
   * This extension can be used for any message, whether 7 bit, 8BITMIME or BINARYMIME.
   */
  @Override
  public SmtpReply getReply(SmtpInputContext smtpInputContext) throws SmtpException {


    SmtpInput smtpInput = smtpInputContext.getSmtpInput();
    SmtpTransactionState sessionState = smtpInputContext.getSessionState();

    if (sessionState.isDataReceptionMode()) {

      Buffer buffer = smtpInput.getBuffer();
      Long bdatChunkSize = sessionState.getExpectedBdatChunkSize();
      int bufferLength = buffer.length();
      if (bufferLength != bdatChunkSize) {
        throw SmtpException.create(SmtpReplyCode.ERROR_IN_PROCESSING_451, "The size of the data (" + bufferLength + ") is not the expected size of the bdat command (" + bdatChunkSize + ") ");
      }
      sessionState.addTextualBufferToBodyData(buffer);

      boolean lastBdat = sessionState.getLastBdat();
      if (lastBdat) {
        /**
         * End of the message
         */
        smtpInputContext.endMessage();
        return getOkReply(sessionState);
      }

      /**
       * Next input should be a BDAT
       */
      sessionState
        .setReceptionModeToCommand()
        .setParserModeFixedToLine();
      return getOkReply(sessionState);

    }

    /**
     * BDAT command
     */
    boolean lastBdat = sessionState.getLastBdat();

    if (lastBdat) {
      throw SmtpException.createBadSequence("The last BDAT segment was already seen");
    }
    SmtpInputCommand smtpRequestCommand = smtpInput.getSmtpRequestCommand();
    List<String> args = smtpRequestCommand.getArguments();

    /**
     * Parameters
     */
    Long chunkSize;
    boolean last;
    int argSize = args.size();
    switch (argSize) {
      case 1:
        chunkSize = getChunkSize(args.get(0));
        last = false;
        break;
      case 2:
        chunkSize = getChunkSize(args.get(0));
        last = true;
        break;
      default:
        throw SmtpException.createBadSyntax("BDAT should have between 1 and 2 arguments, not " + argSize);
    }
    sessionState.setBdatLast(last);
    sessionState.setBdatChunkSize(chunkSize);

    /**
     * By default, the data received is in text format,
     * but it may be also in binary
     */
    if (sessionState.getBodyInputType().equals(SmtpInputType.BINARYMIME)) {
      // binary chunking
      sessionState.setInputType(SmtpInputType.BINARYMIME);
    }

    /**
     * Next State is BDAT but in data mode
     */
    sessionState.setReceptionModeToData();
    sessionState.setParserModeFixedBytes(chunkSize);
    return null;

  }

  /**
   * A common reply given on intermediate reception
   * and on the last one
   */
  private static SmtpReply getOkReply(SmtpTransactionState sessionState) {
    return SmtpReply.createOk("Message Ok, " + sessionState.getBinaryDataReceived().length() + " bytes received");
  }

  private Long getChunkSize(String chunkSizeString) throws SmtpException {
    try {
      return Casts.cast(chunkSizeString, Long.class);
    } catch (CastException e) {
      throw SmtpException.createBadSyntax("The size (" + chunkSizeString + ") is not an integer");
    }
  }

}
