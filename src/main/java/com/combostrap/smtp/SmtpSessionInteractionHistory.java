package com.combostrap.smtp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Record the {@link SmtpInput} and {@link SmtpReply}
 * for a {@link SmtpSocket}
 */
public class SmtpSessionInteractionHistory {

  private final List<SmtpSessionInteractionTimed> interactions = new ArrayList<>();
  private final SmtpSession smtpSession;

  private LocalDateTime lastInteractiveTime = LocalDateTime.now();


  public SmtpSessionInteractionHistory(SmtpSession smtpSession) {
    this.smtpSession = smtpSession;
  }


  public void addInteraction(SmtpSessionInteraction smtpSessionInteraction) {

    this.lastInteractiveTime = LocalDateTime.now();
    SmtpSessionInteractionTimed smtpSessionInteractionTimed = SmtpSessionInteractionTimed
      .create(this.lastInteractiveTime, smtpSessionInteraction);
    this.interactions.add(smtpSessionInteractionTimed);

  }

  public void endAndReplay() {

    if(this.smtpSession.getSmtpService().getSmtpServer().isSessionReplayEnabled()) {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("Session Replay:" + SmtpSyntax.LINE_DELIMITER);
      for (SmtpSessionInteractionTimed interactionTimed : this.interactions) {

        SmtpSessionInteraction interaction = interactionTimed.getInteraction();
        String sessionHistoryLine = interaction.getSessionHistoryLine();
        if (sessionHistoryLine.trim().startsWith(SmtpCommand.AUTH.toString())) {
          String[] parts = sessionHistoryLine.split(" ");
          if (parts.length > 2) {
            sessionHistoryLine = parts[0] + " " + parts[1] + " " + "x".repeat(parts[2].length()) + SmtpSyntax.LINE_DELIMITER;
          }
        }
        stringBuilder.append(sessionHistoryLine);
      }
      System.out.println(stringBuilder);
    }

  }

  public LocalDateTime getLastInteractiveTime() {
    return this.lastInteractiveTime;
  }


}
