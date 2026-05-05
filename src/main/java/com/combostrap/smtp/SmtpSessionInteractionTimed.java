package com.combostrap.smtp;

import java.time.LocalDateTime;

/**
 * A wrapper of {@link SmtpSessionInteraction} to add the time
 */
public class SmtpSessionInteractionTimed {


  @SuppressWarnings("FieldCanBeLocal")
  private final LocalDateTime interactionTime;
  private final SmtpSessionInteraction interaction;

  public SmtpSessionInteractionTimed(LocalDateTime interactionTime, SmtpSessionInteraction smtpSessionInteraction) {
    this.interactionTime = interactionTime;
    this.interaction = smtpSessionInteraction;
  }

  public static SmtpSessionInteractionTimed create(LocalDateTime interactionTime, SmtpSessionInteraction smtpSessionInteraction) {
    return new SmtpSessionInteractionTimed(interactionTime, smtpSessionInteraction);
  }

  public SmtpSessionInteraction getInteraction() {
    return this.interaction;
  }

  @SuppressWarnings("unused")
  public LocalDateTime getInteractionTime() {
    return interactionTime;
  }


}
