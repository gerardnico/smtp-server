package com.combostrap.smtp.mailbox;

import com.combostrap.smtp.SmtpMessage;
import io.vertx.core.Future;

public interface SmtpMailboxInterface {


  Future<Void> deliver(SmtpMessage smtpMessage);


}
