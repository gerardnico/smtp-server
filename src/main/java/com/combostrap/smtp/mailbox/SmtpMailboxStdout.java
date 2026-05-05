package com.combostrap.smtp.mailbox;

import com.combostrap.smtp.SmtpMessage;
import com.combostrap.smtp.SmtpUser;
import com.combostrap.smtp.milter.SmtpMilter;
import com.combostrap.vertx.ConfigAccessor;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.util.List;

public class SmtpMailboxStdout extends SmtpMailbox {


  /**
   * @param vertx          - the vertx in case an async should be run
   * @param configAccessor - the configuration
   */
  public SmtpMailboxStdout(SmtpUser smtpUser, Vertx vertx, List<SmtpMilter> milters, ConfigAccessor configAccessor) {
    super(smtpUser, vertx, milters, configAccessor);
  }

  @Override
  public Future<Void> deliver(SmtpMessage smtpMessage) {

    System.out.println("Delivery on Stdout to " + this.getSmtpUser() + ":");
    System.out.println(new String(smtpMessage.getBytes()));
    return Future.succeededFuture();

  }


}
