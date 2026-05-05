package com.combostrap.smtp;

import com.combostrap.email.BMailMimeMessage;
import com.combostrap.smtp.mailbox.SmtpMailbox;
import com.combostrap.smtp.milter.SmtpMilter;
import com.combostrap.type.MediaType;
import io.vertx.core.Future;

import java.nio.charset.StandardCharsets;

/**
 * A user
 */
public class SmtpUser {


  private final String name;
  private final SmtpDomain smtpDomain;
  private SmtpMailbox mailBox;
  private String password;

  public SmtpUser(SmtpDomain smtpDomain, String name) {
    this.smtpDomain = smtpDomain;
    this.name = name.trim().toLowerCase();
  }

  public static SmtpUser createFrom(SmtpDomain smtpDomain, String userName, String password) {

    SmtpUser smtpUser = new SmtpUser(smtpDomain, userName);
    smtpUser.password = password;
    return smtpUser;

  }


  public SmtpDomain getDomain() {
    return this.smtpDomain;
  }

  public String getName() {
    return this.name;
  }

  public String getPassword() {
    return this.password;
  }

  public Future<Void> deliver(SmtpDeliveryEnvelope smtpDeliveryEnvelope) {
    BMailMimeMessage mimeMessage = smtpDeliveryEnvelope.getMimeMessage();
    SmtpMessage smtpMessage = new SmtpMessage() {

      @Override
      public Object getObject() {
        return mimeMessage;
      }

      @Override
      public byte[] getBytes() {
        return mimeMessage.toEml().getBytes(StandardCharsets.UTF_8);
      }

      @Override
      public String getPath() {
        return mimeMessage.getMessageId() + "." + SmtpMediaType.TEXT_EML.getSubType();
      }

      @Override
      public MediaType getMediaType() {
        return SmtpMediaType.TEXT_EML;
      }
    };

    /**
     * Milters
     */
    for(SmtpMilter milter: this.mailBox.getMilters()){
      smtpMessage = milter.apply(smtpMessage);
    }

    return this.mailBox.deliver(smtpMessage);

  }

  @Override
  public String toString() {
    return name + "@" + this.smtpDomain;
  }

  public void setMailBox(SmtpMailbox smtpMailbox) {
    this.mailBox = smtpMailbox;
  }

  public SmtpMailbox getMailbox() {
    return this.mailBox;
  }

}
