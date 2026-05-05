package com.combostrap.smtp;

/**
 * A class to group
 * together {@link SmtpReply}, {@link SmtpInput} {@link SmtpGreeting}
 * in a {@link SmtpSessionInteractionHistory}
 *
 * Note on the relation between a reply and an input:
 * a reply is not a 1 on 1 relationship
 * <p>
 * Reply: We may send a reply without request
 * (ie when we quit for timeout reason for instance)
 * We may also send replies in batch due to {@link SmtpPipelining}
 * Request; Interaction may be created from only request
 * when we receive only data with {@link com.combostrap.smtp.command.SmtpDataCommandHandler}
 * and {@link com.combostrap.smtp.command.SmtpBdatCommandHandler}
 */
public interface SmtpSessionInteraction {

  /**
   * @return a text output on a line (iethat should end with {@link SmtpSyntax#LINE_DELIMITER})
   * because consoles print only lines
   */
  String getSessionHistoryLine();

}
