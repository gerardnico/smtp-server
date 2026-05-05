package com.combostrap.smtp;

import com.combostrap.smtp.mailbox.SmtpMailbox;
import com.combostrap.smtp.milter.SmtpMilter;
import com.combostrap.vertx.ConfigAccessor;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.util.ArrayList;
import java.util.List;

public class SmtpMailboxMemory extends SmtpMailbox {

  private final List<SmtpMessage> messages = new ArrayList<>();

  /**
   * @param vertx          - the vertx in case an async should be run
   * @param configAccessor - the configuration
   */
  public SmtpMailboxMemory(SmtpUser smtpUser, Vertx vertx, List<SmtpMilter> milters, ConfigAccessor configAccessor) {
    super(smtpUser, vertx, milters, configAccessor);
  }

  @Override
  public Future<Void> deliver(SmtpMessage smtpMessage) {
    messages.add(smtpMessage);
    return Future.succeededFuture();
  }

  public List<SmtpMessage> pumpMessages() {
    List<SmtpMessage> actualMessages = new ArrayList<>(this.messages);
    this.messages.clear();
    return actualMessages;
  }

}
