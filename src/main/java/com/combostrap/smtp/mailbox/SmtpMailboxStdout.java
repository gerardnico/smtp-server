package com.combostrap.smtp.mailbox;

import com.combostrap.smtp.SmtpMessage;
import com.combostrap.smtp.SmtpUser;
import com.combostrap.smtp.milter.SmtpMilter;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.util.List;
import java.util.Map;

public class SmtpMailboxStdout extends SmtpMailbox {


  /**
   * @param vertx          - the vertx in case an async should be run
   * @param props - the configuration
   */
  public SmtpMailboxStdout(SmtpUser smtpUser, Vertx vertx, List<SmtpMilter> milters, Map<String,Object> props) {
    super(smtpUser, vertx, milters, props);
  }

  @Override
  public Future<Void> deliver(SmtpMessage smtpMessage) {

    System.out.println("Delivery on Stdout to " + this.getSmtpUser() + ":");
    System.out.println(new String(smtpMessage.getBytes()));
    return Future.succeededFuture();

  }


}
