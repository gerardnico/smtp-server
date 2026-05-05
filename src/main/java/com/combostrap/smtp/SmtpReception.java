package com.combostrap.smtp;

import com.combostrap.email.BMailMimeMessage;
import io.vertx.core.buffer.Buffer;
import jakarta.mail.MessagingException;

import java.io.IOException;
import java.util.Set;

public class SmtpReception {


  private final SmtpDelivery smtpDelivery;

  public SmtpReception(SmtpDelivery smtpDelivery) {
    this.smtpDelivery = smtpDelivery;
  }


  /**
   * A reception is when you store the SMTP transaction.
   * The {@link SmtpDelivery#deliver(SmtpDeliveryEnvelope)} is normally done later on schedule
   */
  public void reception(SmtpTransactionState transactionState) throws SmtpException {


    BMailMimeMessage mimeMessage;
    try {

      /**
       * Message size check
       */
      Buffer textMessage = transactionState.getMessage();
      Integer messageSizeFromMailCommand = transactionState.getMessageSizeFromMailCommand();
      if (messageSizeFromMailCommand != null) {
        int messageSize = textMessage.length();
        if (messageSize != messageSizeFromMailCommand) {
          throw SmtpException.create(SmtpReplyCode.ERROR_IN_PROCESSING_451, "The message size received (" + messageSize + ") is different that the MAIL command size (" + messageSizeFromMailCommand + ")");
        }
      }

      String eml = textMessage.toString();

      mimeMessage = BMailMimeMessage.createFromEml(eml);

    } catch (MessagingException | IOException e) {
      throw SmtpException.createForInternalException("Reception Error: ", e);
    }

    /**
     * Mail From Address Handling
     * <p>
     * During the transaction, the {@link SmtpFiltering}
     * checks have already run
     * * on Ip of the remote
     * * and Domain of the `From` email
     * * {@link SmtpSpf}
     */

    /**
     * Email Dkim
     */
    SmtpSession session = transactionState.getSession();
    SmtpDkim.check(session);

    /**
     * Add trace header upon reception
     */
    SmtpReceptionTracing.addTraceHeader(session, mimeMessage);

    /**
     * Create the envelope
     */
    Set<SmtpRecipient> recipients = transactionState.getRecipients();
    SmtpDeliveryEnvelope enveloppe = SmtpDeliveryEnvelope.create(
      transactionState.getSender(),
      recipients,
      mimeMessage
    );
    this.smtpDelivery.addEnvelopeToQueue(enveloppe);

  }



}
