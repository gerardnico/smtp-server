package com.combostrap.smtp.mailbox;

import com.combostrap.s3.AwsBucket;
import com.combostrap.s3.AwsObject;
import com.combostrap.smtp.SmtpMessage;
import com.combostrap.smtp.SmtpUser;
import com.combostrap.smtp.milter.SmtpMilter;
import com.combostrap.vertx.ConfigAccessor;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.util.List;

public class SmtpMailboxS3 extends SmtpMailbox {

  private final AwsBucket awsBucket;

  public SmtpMailboxS3(SmtpUser smtpUser, Vertx vertx, List<SmtpMilter> milters, ConfigAccessor configAccessor) {
    super(smtpUser, vertx, milters, configAccessor);
    this.awsBucket = AwsBucket.init(vertx, configAccessor);
  }


  @Override
  public Future<Void> deliver(SmtpMessage smtpMessage) {


    AwsObject awsObject = AwsObject
      .create(smtpMessage.getPath())
      .setContent(smtpMessage.getBytes())
      .setMediaType(smtpMessage.getMediaType());
    return this.awsBucket.putObject(awsObject);

  }

}
