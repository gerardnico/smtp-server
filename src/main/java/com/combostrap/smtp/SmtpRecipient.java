package com.combostrap.smtp;

import com.combostrap.email.BMailInternetAddress;

import java.util.Objects;

/**
 * The envelope recipient
 * <p>
 * Note on the diff between `recipients` from the rcpt command and `to` from the message
 * <p>
 * The message may be sent to:
 * * multiple recipient that are not in our domain
 * * a list
 * Therefore the recipient from the RCP command and the `to` address from the message
 * may not be the same at all
 * Example for Google Group:
 * To: Abridged recipients <vertx@googlegroups.com>
 * From: vertx@googlegroups.com
 */
public class SmtpRecipient {
    private final SmtpDeliveryType deliveryType;
    private final BMailInternetAddress internetAddress;
    private final SmtpUser smtpUser;

    private SmtpRecipient(BMailInternetAddress internetAddress, SmtpDeliveryType deliveryType, SmtpUser smtpUser) {
        this.internetAddress = internetAddress;
        this.deliveryType = deliveryType;
        this.smtpUser = smtpUser;
    }

    public static SmtpRecipient createFrom(SmtpSession smtpSession, String forwardPathString) throws SmtpException {

        /**
         * Forward path to internet address
         */


        SmtpPostMaster postmaster = smtpSession.getGreeting().getRequestedHostOrDefault().getPostmaster();
        BMailInternetAddress recipientInternetAddress = SmtpPath.of(forwardPathString).getInternetAddress(postmaster).orElse(null);
        if (recipientInternetAddress == null) {
            throw SmtpException.create(SmtpReplyCode.USER_AMBIGUOUS_553, forwardPathString + " Invalid TO forward path address.");
        }

        /**
         * Check and processing
         * Determine the transaction delivery
         */
        BMailInternetAddress mailFromSender = smtpSession.getTransactionState().getSender();
        SmtpDeliveryType delivery;
        if (smtpSession.isFirstPartyEmail(mailFromSender)) {
            SmtpFiltering.checkFirstPartySenderShouldBeAuthenticated(smtpSession, mailFromSender);
            if (smtpSession.isFirstPartyEmail(recipientInternetAddress)) {
                delivery = SmtpDeliveryType.LOCAL;
            } else {
                delivery = SmtpDeliveryType.REMOTE;
            }
        } else {
            if (smtpSession.isFirstPartyEmail(recipientInternetAddress)) {
                delivery = SmtpDeliveryType.LOCAL;
            } else {
                throw SmtpException.create(SmtpReplyCode.NO_SUCH_USER_550, "User "+recipientInternetAddress+" is unknown (not a local user)")
                        .setBadBehaviorFlag();
            }
        }

        /**
         * Do we have the mailboxes
         */
        SmtpUser smtpUser = null;
        if (delivery == SmtpDeliveryType.LOCAL) {
            smtpUser = smtpSession.checkUserExists(recipientInternetAddress);
        }

        return new SmtpRecipient(recipientInternetAddress, delivery, smtpUser);
    }

    @Override
    public String toString() {
        return internetAddress.toString();
    }

    public SmtpDeliveryType getDeliveryType() {
        return this.deliveryType;
    }

    public SmtpUser getLocalUser() {
        return this.smtpUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SmtpRecipient that = (SmtpRecipient) o;
        return Objects.equals(internetAddress, that.internetAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internetAddress);
    }

}
