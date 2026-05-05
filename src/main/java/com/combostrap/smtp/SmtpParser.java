package com.combostrap.smtp;


import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.Arguments;
import io.vertx.core.net.NetSocket;

import java.util.ArrayList;
import java.util.List;

/**
 * SMTP parsing that allows {@link SmtpExtensionParameter#PIPELINING}
 * the data is returned when the buffer is empty.
 * You got:
 * * a group of command
 * * or:
 * * the data lines of the DATA command
 * * the BDAT command followed by a random bytes (the buffer may)
 * <p>
 * {@link Handler} defines the handler that is called on the socket stream in the constructor
 * <p>
 */
public class SmtpParser implements Handler<Buffer> {

  private final SmtpParserConf conf;


  private final byte[] lineDelim = latin1StringToBytes(SmtpSyntax.LINE_DELIMITER).getBytes();


  private boolean isParsingRunning;
  /**
   * Got true when the socket stream has ended
   */
  private boolean streamEnded;
  private Long fetchFixedSize;
  private SmtpParserMode parserMode = SmtpParserMode.LINE;

  /**
   * The internal buffer
   * With the {@link com.combostrap.smtp.command.SmtpBdatCommandHandler}
   * the buffer data received by the {@link #handle(Buffer)}
   * function may not be of fixe length
   * We need to keep data over
   */
  private Buffer bufferToParse = Buffer.buffer();

  /**
   * In pipeline mode, commands can be received in batch
   * <p>
   * The pipeline mode (ie batch command) is disallowed:
   * * if not enabled
   * * if already seen once (only one pipeline is allowed by transaction)
   */
  private boolean pipelineBatchEnabled;


  private SmtpParser(SmtpParserConf smtpParserConf) {

    this.conf = smtpParserConf;

    resetState();

    /**
     * Socket stream handling functions
     */
    conf.netSocket
      .endHandler((v) -> {
        this.streamEnded = true;
        conf.endSocketHandler.handle(v);
      })
      .exceptionHandler((err) -> {
        if (conf.exceptionHandler != null) {
          conf.exceptionHandler.handle(err);
        }
      })
      .handler(this);

  }


  public static Buffer latin1StringToBytes(String str) {
    byte[] bytes = new byte[str.length()];

    for (int i = 0; i < str.length(); ++i) {
      char c = str.charAt(i);
      bytes[i] = (byte) (c & 255);
    }

    return Buffer.buffer(bytes);
  }


  public static SmtpParserConf conf(SmtpSession smtpSession, NetSocket netSocket) {
    return new SmtpParserConf(smtpSession, netSocket);
  }


  private void handleParsing() {

    if (!this.isParsingRunning) {
      this.isParsingRunning = true;
      try {

        /**
         * The buffer may be:
         * * A command line ended by {@link SmtpSyntax#LINE_DELIMITER}
         * * A list of command lines (Pipelining) separated by {@link SmtpSyntax#LINE_DELIMITER}
         * * The eml content after the {@link com.combostrap.smtp.command.SmtpDataCommandHandler DATA command}
         * * A BDAT command line and its data (maybe partial)
         */
        List<SmtpInput> parsedSmtpInput = new ArrayList<>();

        /**
         * We don't detect the line in data
         */
        SmtpSession smtpSession = this.conf.smtpSession;
        SmtpInputType inputType = smtpSession.getTransactionState().getInputType();

        /**
         * Parse the input buffer into
         * * command end with a line
         * * the message as {@link com.combostrap.smtp.command.SmtpDataCommandHandler DATA format} end with a line with 1 point
         * * or fixed bytes size record
         */
        Integer pointer = -1;

        /**
         * Buffer parsing
         */
        boolean shouldBreakBufferParsing = false;
        while (pointer < this.bufferToParse.length() - 1) {
          /**
           * Record parsing
           */
          boolean shouldBreakRecordParsing = false;
          while (pointer < this.bufferToParse.length() - 1) {

            Buffer parsedBuffer = null;
            if (this.parserMode == SmtpParserMode.LINE) {
              try {
                parsedBuffer = Buffer.buffer();
                pointer = this.advancePointerAndReturnLine(parsedBuffer, pointer);
              } catch (SmtpParserEndBuffer e) {
                shouldBreakBufferParsing = true;
                break;
              }
              if (!this.pipelineBatchEnabled) {
                /**
                 * One record by line
                 */
                shouldBreakRecordParsing = true;
              }
            } else {

              /**
               * Fixed bytes size record
               */
              if (this.bufferToParse.length() - (pointer + 1) >= this.fetchFixedSize) {
                int fixedSizeStart = pointer + 1;
                int fixedSizeEnd = fixedSizeStart + this.fetchFixedSize.intValue();
                parsedBuffer = this.bufferToParse.getBuffer(fixedSizeStart, fixedSizeEnd);
                pointer = fixedSizeEnd;
              }

              /**
               * Fixed Size Parsing
               * Returns always only one record
               */
              shouldBreakRecordParsing = true;
              shouldBreakBufferParsing = true;

            }

            /**
             * Handle the input
             * Note that the buffer may be the empty line
             * Parsed buffer null means that the parsing is not yet final
             */
            if (parsedBuffer != null) {
              SmtpInput smtpInput = SmtpInput.create(smtpSession, parsedBuffer, inputType);
              smtpSession.getSessionHistory().addInteraction(smtpInput);
              parsedSmtpInput.add(smtpInput);
            }

            /**
             * Break/Stop the loop
             */
            if (shouldBreakRecordParsing || this.streamEnded) {
              break;
            }

          }

          /**
           * May be empty if the fixed requested amount size is not
           * yet arrived
           */
          if (parsedSmtpInput.size() >= 1) {

            /**
             * Enable/Disable pipeline mode and
             * check the structure if necessary
             * {@link SmtpPipelining}
             */
            if (pipelineBatchEnabled) {
              if (parsedSmtpInput.size() >= 2) {
                SmtpPipelining.checkStateIfPipeline(parsedSmtpInput);
                pipelineBatchEnabled = false;
              } else {
                /**
                 * When the client does not support pipeline,
                 * it sends the command one by one
                 * The pipeline mode should be disabled before receiving
                 * the data (BDAT is followed by some byte and not by another line)
                 */
                for (SmtpInput smtpInput : parsedSmtpInput) {
                  if (smtpInput.getSmtpRequestCommand().getCommand() == SmtpCommand.RCPT) {
                    pipelineBatchEnabled = false;
                    break;
                  }
                }
              }
            }

            /**
             * Handle the parse
             */
            conf.smtpHandler.handle(parsedSmtpInput);

            /**
             * Re-initialize
             */
            parsedSmtpInput = new ArrayList<>();

          }

          if (shouldBreakBufferParsing) {
            break;
          }

        }

        /**
         * Keep the rest of the buffer not parsed or reset
         */
        if (pointer < this.bufferToParse.length() - 1) {
          pointer = pointer + 1; // pointer starts at -1
          this.bufferToParse = this.bufferToParse.getBuffer(pointer, this.bufferToParse.length());
        } else {
          this.bufferToParse = Buffer.buffer();
        }


        /**
         * Get the next buffer
         */
        conf.netSocket.resume();

      } catch (Exception e) {

        conf.exceptionHandler.handle(e);

      } finally {

        this.isParsingRunning = false;

      }

    }
  }


  private Integer advancePointerAndReturnLine(Buffer lineBuffer, Integer actualBufferPosition) throws SmtpParserEndBuffer {

    Buffer delimiterBuffer = Buffer.buffer();
    while (true) {
      /**
       * Advance the pointer, the start is minus 1,
       * We get the first one at 0 then
       */
      actualBufferPosition++;
      if (actualBufferPosition > bufferToParse.length() - 1) {
        break;
      }
      byte actualByte = bufferToParse.getByte(actualBufferPosition);
      if (actualByte == this.lineDelim[delimiterBuffer.length()]) {
        delimiterBuffer.appendByte(actualByte);
        if (delimiterBuffer.length() == this.lineDelim.length) {
          return actualBufferPosition;
        }
        continue;
      }
      if (delimiterBuffer.length() != 0) {
        lineBuffer.appendBuffer(delimiterBuffer);
        delimiterBuffer = Buffer.buffer();
      }
      lineBuffer.appendByte(actualByte);

    }

    /**
     * With a big data body, the message is split by MTU
     * We may be in the middle of one
     */
    throw new SmtpParserEndBuffer();

  }


  /**
   * This function handles the input of the socket
   */
  @Override
  public void handle(Buffer buffer) {

    if (this.bufferToParse.length() == 0) {
      bufferToParse = buffer;
    } else {
      bufferToParse.appendBuffer(buffer);
    }

    if (buffer != null && conf.maxBufferSizeInBytes > 0 && buffer.length() > conf.maxBufferSizeInBytes) {
      IllegalStateException ex = new IllegalStateException("The current buffer data is too long");
      if (conf.exceptionHandler == null) {
        throw ex;
      }
      conf.exceptionHandler.handle(ex);
    }

    this.handleParsing();

  }

  public void setParserModeToFix(Long fixedSized) {
    this.parserMode = SmtpParserMode.FIXED;
    this.fetchFixedSize = fixedSized;
  }


  public void setParserModeToLine() {
    this.parserMode = SmtpParserMode.LINE;
  }


  public void resetPipelineBatchAllowed() {
    this.pipelineBatchEnabled = this.conf.smtpSession.getSmtpService().isPipeliningEnabled();
  }

  /**
   * The commands are the drivers of the parser,
   * they tell the mode
   * For instance, the BDAT command tell the parser the amount of data to fetch
   **/
  public void resetState() {
    setParserModeToLine();
    resetPipelineBatchAllowed();
  }


  public static class SmtpParserConf {
    private final NetSocket netSocket;
    private final SmtpSession smtpSession;
    private Handler<List<SmtpInput>> smtpHandler;
    private int maxBufferSizeInBytes;
    private Handler<Void> endSocketHandler;
    private Handler<Throwable> exceptionHandler;

    public SmtpParserConf(SmtpSession smtpSession, NetSocket netSocket) {
      this.netSocket = netSocket;
      this.smtpSession = smtpSession;
    }

    /**
     * @param smtpHandler - the class that will output the output
     */
    public SmtpParserConf setOutputHandler(Handler<List<SmtpInput>> smtpHandler) {
      this.smtpHandler = smtpHandler;
      return this;
    }

    /**
     * @param maxMessageSizeInBytes - the max size that can be sent by the client
     */
    public SmtpParserConf setMaxBufferSize(int maxMessageSizeInBytes) {
      Arguments.require(maxMessageSizeInBytes > 0, "Size must be > 0");
      this.maxBufferSizeInBytes = maxMessageSizeInBytes;
      return this;
    }

    public SmtpParserConf setEndStreamHandler(Handler<Void> endSocketHandler) {
      this.endSocketHandler = endSocketHandler;
      return this;
    }

    public SmtpParserConf exceptionHandler(Handler<Throwable> handleException) {
      this.exceptionHandler = handleException;
      return this;
    }

    public SmtpParser build() {
      return new SmtpParser(this);
    }
  }
}
