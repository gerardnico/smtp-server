package com.combostrap.smtp;

import com.combostrap.vertx.ConfigAccessor;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

public class SmtpDelivery implements Handler<Long> {

    public static final String DELIVERY_RUN_AFTER_RECEPTION_CONF = "delivery.run.after.reception";
    private static final String DELIVERY_RETRY_INTERVAL_KEY = "delivery.retry.interval";
    private static final Integer DEFAULT_INTERVAL = 5;
    public static final String DELIVERY_RUN_INTERVAL_KEY = "delivery.run.interval";
    static Logger LOGGER = Logger.getLogger(SmtpDelivery.class.getName());
    private static final int DEFAULT_RETRY_INTERVAL = 15;
    private final Map<Integer, SmtpDeliveryEnvelope> deliveryQueue = new HashMap<>();
    private final Integer retryIntervalBetweenFailures;
    private final Boolean immediateDelivery;
    private boolean isRunning = false;


    public SmtpDelivery(Vertx vertx, ConfigAccessor configAccessor) {

        this.immediateDelivery = configAccessor.getBoolean(DELIVERY_RUN_AFTER_RECEPTION_CONF, false);
        LOGGER.info("Delivery does " + (this.immediateDelivery ? "" : "not") + " run after reception");

        Integer deliveryInterval = configAccessor.getInteger(DELIVERY_RUN_INTERVAL_KEY, DEFAULT_INTERVAL);
        if (deliveryInterval > 0) {
            LOGGER.info("Delivery run interval (" + DELIVERY_RETRY_INTERVAL_KEY + ") set to " + deliveryInterval + " minutes");
            vertx.setPeriodic(deliveryInterval * 60 * 1000, this);
        } else {
            LOGGER.info("Delivery run at interval disabled. The interval value (" + deliveryInterval + ") of the configuration (" + DELIVERY_RETRY_INTERVAL_KEY + ") is negative or null.");
        }

        this.retryIntervalBetweenFailures = configAccessor.getInteger(DELIVERY_RETRY_INTERVAL_KEY, DEFAULT_RETRY_INTERVAL);
        LOGGER.info("Delivery retry interval (" + DELIVERY_RETRY_INTERVAL_KEY + ") between failures set to " + this.retryIntervalBetweenFailures + " minutes");

    }

    @Override
    public void handle(Long event) {

        this.run();

    }

    Future<Void> run() {

        int size = deliveryQueue.size();
        if (size == 0) {
            LOGGER.info("Delivery: Nothing to deliver");
            return Future.succeededFuture();
        }
        if (this.isRunning) {
            LOGGER.info("Delivery already running");
            return Future.succeededFuture();
        }
        this.isRunning = true;

        LOGGER.info("Delivery started for " + size + " enveloppes");


        List<Future<Void>> envelopeDeliveries = new ArrayList<>();

        for (Integer enveloppeId : deliveryQueue.keySet()) {
            SmtpDeliveryEnvelope envelope = deliveryQueue.get(enveloppeId);
            Future<Void> envelopeDelivery = this.deliver(envelope)
                    .compose(v -> {
                        if (envelope.hasBeenDelivered()) {
                            deliveryQueue.remove(enveloppeId);
                        }
                        return Future.succeededFuture();
                    });
            envelopeDeliveries.add(envelopeDelivery);
        }

        return Future.join(envelopeDeliveries)
                .compose(ar -> {
                    this.isRunning = false;
                    LOGGER.info("Delivery done. Still to deliver next time: " + this.deliveryQueue.size());
                    return Future.succeededFuture();
                });

    }

    public void addEnvelopeToQueue(SmtpDeliveryEnvelope enveloppe) {
        int key = enveloppe.hashCode();
        LOGGER.info("Reception of the enveloppe (" + key + "), Message Id: " + enveloppe.getMimeMessage().getMessageId());
        this.deliveryQueue.put(key, enveloppe);
        if (this.immediateDelivery) {
            this.run();
        }
    }

    /**
     * 2 choices:
     * * local delivery: storing the email in a local mailbox (disk, http endpoint)
     * * remote delivery: transmitting it to remote mailbox with or without forwarding (alias): SRS
     */
    public Future<Void> deliver(SmtpDeliveryEnvelope smtpDeliveryEnvelope) {


        /**
         * Recipients
         */
        Set<SmtpRecipient> recipientsPaths = smtpDeliveryEnvelope.getRecipientsToDeliver();
        List<Future<Void>> delivery = new ArrayList<>();
        for (SmtpRecipient recipient : recipientsPaths) {

            delivery.add(this.deliverToRecipient(recipient, smtpDeliveryEnvelope));

        }
        return Future.join(delivery)
                .compose(res -> Future.succeededFuture());


    }

    private Future<Void> deliverToRecipient(SmtpRecipient recipient, SmtpDeliveryEnvelope smtpDeliveryEnvelope) {

        /**
         * Retry Handling
         * TODO: Retry for X more hours and notified if the delivery fails permanently. Gmail retries for 46 more hours.
         */
        Instant lastDeliveringTentative = smtpDeliveryEnvelope.getLastDeliveryTentative(recipient).orElse(null);
        if (lastDeliveringTentative != null) {
            long duration = Duration.between(lastDeliveringTentative, Instant.now()).toMinutes();
            if (duration < retryIntervalBetweenFailures) {
                return Future.succeededFuture();
            }
        }

        /**
         * Delivery
         */
        Future<Void> deliveryFuture;
        SmtpDeliveryType deliveryType = recipient.getDeliveryType();
        switch (deliveryType) {
            case LOCAL:
                deliveryFuture = recipient.getLocalUser().deliver(smtpDeliveryEnvelope);
                break;
            case REMOTE:
                /**
                 * Gmail's Bulk Senders Guidelines
                 * https://support.google.com/mail/answer/81126
                 * DMARC/Postmaster tool
                 * https://support.google.com/mail/answer/2451690
                 */
                deliveryFuture = Future.failedFuture("Remote delivery is not yet supported");
                break;
            default:
                deliveryFuture = Future.failedFuture("Internal Error: Delivery Type (" + deliveryType + ") is unknown");
        }
        return deliveryFuture.compose(ar -> {
                    smtpDeliveryEnvelope.hasBeenDeliveredToRecipient(recipient);
                    return Future.succeededFuture();
                }, err -> {
                    smtpDeliveryEnvelope.deliveryFailureForRecipient(recipient, err);
                    return Future.failedFuture(err);
                }
        );
    }


}
