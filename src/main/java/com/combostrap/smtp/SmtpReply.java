package com.combostrap.smtp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.2">Smtp Replies</a>
 */
public class SmtpReply implements SmtpSessionInteraction {
  private final SmtpReplyCode smtpReplyCode;

  /**
   * To be able to make a reply such as
   * <p>
   * 250-server.host.name
   * 250-PIPELINING
   * 250-SIZE 10240000
   * 250-ETRN
   * 250 8BITMIME
   * <p>
   * <a href="https://datatracker.ietf.org/doc/html/rfc2821#page-12">Lines</a>
   */
  private final List<String> humanTextLines = new ArrayList<>();

  public SmtpReply(SmtpReplyCode smtpReplyCode) {
    this.smtpReplyCode = smtpReplyCode;
  }

  public static SmtpReply create(SmtpReplyCode smtpReplyCode) {
    return new SmtpReply(smtpReplyCode);
  }

  public static SmtpReply create(SmtpReplyCode smtpReplyCode, String humanText) {
    return new SmtpReply(smtpReplyCode)
      .addHumanTextLine(humanText);
  }

  public static SmtpReply createOk() {
    return SmtpReply.create(SmtpReplyCode.OK_250);
  }

  public static SmtpReply createOk(String humanText) {
    return SmtpReply.create(SmtpReplyCode.OK_250, humanText);
  }

  /**
   * This is the entry point for internal exception, even
   * if we have a {@link SmtpException} because the exception
   * can be fired by the JVM (ie NPE for instance)
   */
  public static SmtpReply createForInternalException(String humanText) {
    return create(SmtpReplyCode.TRANSACTION_FAILED_554)
      .addHumanTextLine("Internal Error: "+humanText);
  }


  public SmtpReply addHumanTextLine(String humanText) {
    this.humanTextLines.add(humanText);
    return this;
  }

  public SmtpReplyCode getSmtpReplyCode() {
    return this.smtpReplyCode;
  }

  public List<String> getHumanTextLines() {
    return this.humanTextLines;
  }


  public String getReplyLines() {
    List<String> humanTextLines = getHumanTextLines();
    SmtpReplyCode replyCode = getSmtpReplyCode();
    if (humanTextLines == null || humanTextLines.size() == 0) {
      humanTextLines = Collections.singletonList(replyCode.getHumanText());
    }
    StringBuilder reply = new StringBuilder();
    if (humanTextLines.size() == 1) {
      reply
        .append(replyCode.getCode())
        .append(' ')
        .append(humanTextLines.get(0))
        .append(SmtpSyntax.LINE_DELIMITER);
    } else {
      /**
       * Multiline
       * <p>
       * 123-First line
       * 123-Second line
       * 123-234 text beginning with numbers
       * 123 The last line
       */
      for (int i = 0; i < humanTextLines.size(); i++) {
        reply.append(replyCode.getCode());
        if (i != humanTextLines.size() - 1) {
          reply.append('-');
        } else {
          reply.append(' ');
        }
        reply
          .append(humanTextLines.get(i))
          .append(SmtpSyntax.LINE_DELIMITER);
      }
    }
    return reply.toString();
  }

  @Override
  public String getSessionHistoryLine() {
    return getReplyLines();
  }

}
