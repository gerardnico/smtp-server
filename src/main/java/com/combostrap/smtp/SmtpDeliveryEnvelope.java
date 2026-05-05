package com.combostrap.smtp;

import com.combostrap.email.BMailInternetAddress;
import com.combostrap.email.BMailMimeMessage;

import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An envelope is a sender, a recipient and the message
 * It's the delivery unit so that we don't need to duplicate the message
 */
public class SmtpDeliveryEnvelope {

    static Logger LOGGER = Logger.getLogger(SmtpDeliveryEnvelope.class.getName());

    /**
     * The sender (if there is any error, we should
     * send a message to the sender)
     */
    private final BMailInternetAddress sender;
    private final Set<SmtpRecipient> recipientsToDeliver;
    private final BMailMimeMessage mimeMessage;
    private final Map<SmtpRecipient, SmtpDeliveryFailure> deliveryFailuresForRecipient = new HashMap<>();

    public SmtpDeliveryEnvelope(BMailInternetAddress sender, Set<SmtpRecipient> recipientsToDeliver, BMailMimeMessage mimeMessage) {
        this.sender = sender;
        this.recipientsToDeliver = recipientsToDeliver;
        this.mimeMessage = mimeMessage;
    }

    public static SmtpDeliveryEnvelope create(BMailInternetAddress sender, Set<SmtpRecipient> recipients, BMailMimeMessage mimeMessage) {
        return new SmtpDeliveryEnvelope(sender, recipients, mimeMessage);
    }

    public Set<SmtpRecipient> getRecipientsToDeliver() {
        return this.recipientsToDeliver;
    }

    public BMailMimeMessage getMimeMessage() {
        return this.mimeMessage;
    }

    public BMailInternetAddress getSender() {
        return sender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SmtpDeliveryEnvelope that = (SmtpDeliveryEnvelope) o;
        return Objects.equals(sender, that.sender) && Objects.equals(recipientsToDeliver, that.recipientsToDeliver) && Objects.equals(mimeMessage, that.mimeMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, recipientsToDeliver, mimeMessage);
    }

    public boolean hasBeenDelivered() {
        return this.recipientsToDeliver.size() == 0;
    }

    public void hasBeenDeliveredToRecipient(SmtpRecipient recipient) {
        this.recipientsToDeliver.remove(recipient);
        LOGGER.info("Enveloppe with the message (" + this.mimeMessage.getSubject() + ") has been delivered to the recipient (" + recipient + ").");
    }

    /**
     * <a href="https://datatracker.ietf.org/doc/html/rfc5321#section-6.1">...</a>
     * If there is a delivery failure after acceptance of a message, the
     * receiver-SMTP MUST formulate and mail a notification message.  This
     * notification MUST be sent using a null ("<>") reverse-path in the
     * envelope.
     * <p>
     * To not become a backscatterer, we have taken most of the filtering steps
     * before accepting the emails
     * </p>
     * <p>
     * Delivered-To: gerardnico@gmail.com
     * Return-Path: <SRS0=155e=FQ=gmail.com=gerardnico@eraldy.com>
     * X-Original-To: support@combostrap.com
     * X-Complaints-To: abuse@xxxx.net
     * X-Report-Abuse-To: abuse@xxxx.net
     * X-Report-Abuse: abuse@xxx.net
     * X-Eraldy-Sender: rfc822; gerardnico@gmail.com, mail-yw1-f182.google.com, 209.85.128.182
     * X-Eraldy-Version: 10.0.0-alpha
     */
    public void deliveryFailureForRecipient(SmtpRecipient recipient, Throwable e) {

        SmtpDeliveryFailure deliveryFailure = this.deliveryFailuresForRecipient.computeIfAbsent(recipient, k -> new SmtpDeliveryFailure());
        deliveryFailure.inc(e);
        this.deliveryFailuresForRecipient.put(recipient, deliveryFailure);
        LOGGER.log(Level.SEVERE, "Unable to deliver the enveloppe (" + this + ") to the recipient (" + recipient + "). " + deliveryFailure + " times", e);

    }

    public Optional<Instant> getLastDeliveryTentative(SmtpRecipient smtpRecipient) {
        SmtpDeliveryFailure deliveryFailure = this.deliveryFailuresForRecipient.get(smtpRecipient);
        if (deliveryFailure == null) {
            return Optional.empty();
        }
        return Optional.of(deliveryFailure.getLastTentative());
    }
}
