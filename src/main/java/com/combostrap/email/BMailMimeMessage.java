package com.combostrap.email;

import com.combostrap.type.MediaType;
import com.combostrap.type.MediaTypePathNotAbsoluteException;
import com.combostrap.type.MediaTypes;
import jakarta.activation.DataHandler;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/**
 * A wrapper around a {@link MimeMessage}
 * See also the new com.sun.mail.smtp.SMTPMessage
 * that wraps the Mime Message for other options
 * <p>
 *
 * @link <a href="https://www.rfc-editor.org/rfc/rfc5322">Last rfc on message format</a>
 * @link <a href="https://www.rfc-editor.org/rfc/rfc822.html">...</a>
 * @link <a href="https://docs.oracle.com/javaee/6/api/javax/mail/internet/MimeMessage.html">...</a>
 */
public class BMailMimeMessage {


    private static final Charset EMAIL_CHARSET = StandardCharsets.UTF_8;

    private final MimeMessage mimeMessage;


    private static final MediaType HTML_MIME = MediaType.TEXT_HTML;
    private static final MediaType TEXT_MIME = MediaType.TEXT_PLAIN;


    private BMailMimeMessage(MimeMessage mimeMessage) {

        this.mimeMessage = mimeMessage;


    }


    public static builder createFromBuilder() {

        return new builder();


    }

    public static BMailMimeMessage createFromBMail(BMailMimeMessage message) {

        try {
            return createFromMimeMessage(message.toMimeMessage());
        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BMailMimeMessage createFromEml(String message) throws IOException, MessagingException {

        Session session = Session.getInstance(new Properties());

        ByteArrayInputStream is = new ByteArrayInputStream(message.getBytes(EMAIL_CHARSET));
        MimeMessage mimeMessage = new MimeMessage(session, is);
        return createFromMimeMessage(mimeMessage);

    }


    @SuppressWarnings("unused")
    public BMailMimeMessage createMessageFromRawString(String emailContent) {

        ByteArrayInputStream inputStream = new ByteArrayInputStream(emailContent.getBytes(StandardCharsets.UTF_8));
        MimeMessage mimeMessage;
        try {

            mimeMessage = new MimeMessage(null, inputStream);

            return new BMailMimeMessage(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    public static BMailMimeMessage createFromMimeMessage(MimeMessage mimeMessage) throws MessagingException, IOException {
        return new BMailMimeMessage(mimeMessage);
    }

    public BMailMimeMessage setFrom(String from) {

        if (from == null) {
            throw new RuntimeException("From cannot be null");
        }
        try {
            InternetAddress address = new InternetAddress(from);
            mimeMessage.setFrom(address);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return this;

    }


    public MimeMessage toMimeMessage() {

        return this.mimeMessage;

    }

    @SuppressWarnings("unused")
    public boolean hasNoContent() {
        boolean hasNoContent = false;
        try {
            this.mimeMessage.getContent();
        } catch (MessagingException | IOException e) {
            hasNoContent = true;
        }
        return hasNoContent;
    }

    public BMailMimeMessage setTo(String to) {
        try {
            this.mimeMessage.setRecipients(MimeMessage.RecipientType.TO, to);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return this;

    }

    public BMailMimeMessage setToInternetAddresses(List<InternetAddress> to) {
        try {
            this.mimeMessage.setRecipients(MimeMessage.RecipientType.TO, to.toArray(new InternetAddress[0]));
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return this;

    }

    public BMailMimeMessage setTo(String to, String toName) {
        try {
            InternetAddress toAddress = new InternetAddress(to);
            toAddress.setPersonal(toName, String.valueOf(StandardCharsets.UTF_8));
            this.mimeMessage.addRecipient(MimeMessage.RecipientType.TO, toAddress);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return this;

    }

    @SuppressWarnings("unused")
    public BMailMimeMessage setBcc(String bcc) {
        try {
            this.mimeMessage.setRecipients(MimeMessage.RecipientType.BCC, InternetAddress.parse(bcc, false));
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    @SuppressWarnings("unused")
    public BMailMimeMessage setCc(String cc) {
        try {
            this.mimeMessage.setRecipients(MimeMessage.RecipientType.CC, InternetAddress.parse(cc, false));
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    public BMailMimeMessage setSubject(String title) {
        try {
            this.mimeMessage.setSubject(title);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public String getSubject() {
        try {
            return this.mimeMessage.getSubject();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<BMailInternetAddress> getTos() {
        try {
            Address[] allRecipients = this.mimeMessage.getRecipients(MimeMessage.RecipientType.TO);
            if (allRecipients == null) {
                return new ArrayList<>();
            }
            List<BMailInternetAddress> list = new ArrayList<>();
            for (Address recipientAddress : allRecipients) {
                if (recipientAddress instanceof InternetAddress) {
                    InternetAddress address = (InternetAddress) recipientAddress;
                    BMailInternetAddress bMailInternetAddress = BMailInternetAddress.of(address);
                    list.add(bMailInternetAddress);
                }
            }
            return list;
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public BMailAddresses getToAsAddresses() {
        try {
            return BMailAddresses.of(getTos());
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public InternetAddress getToInternetAddress() {
        List<InternetAddress> toInternetAddresses = getToInternetAddresses();
        if (toInternetAddresses.size() >= 1) {
            return toInternetAddresses.get(0);
        }
        return null;
    }

    /**
     * @return the raw data of the message (ie mime encoding)
     * You can pipe it to `sendmail` to send it for instance
     */
    public String toEml() {

        ByteArrayOutputStream copyBoas = new ByteArrayOutputStream();
        try {
            MimeMessage mimeMessage = toMimeMessage();
            mimeMessage.writeTo(copyBoas);
            return copyBoas.toString(EMAIL_CHARSET);
        } catch (IOException | MessagingException e) {
            throw new RuntimeException(e);
        }

    }

    public BMailInternetAddress getFrom() {

        try {
            return BMailInternetAddress.of(getFromInternetAddress());
        } catch (AddressException e) {
            throw new RuntimeException(e);
        }

    }

    public String getFromAsString() {
        InternetAddress fromInternetAddress = getFromInternetAddress();
        if (fromInternetAddress == null) {
            return null;
        }
        return fromInternetAddress.toString();
    }


    @SuppressWarnings("unused")
    public ContentType getContentType() {
        try {
            return new ContentType(this.mimeMessage.getContentType());
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    public String toEmlWithoutHeaders(String... ignoreList) {
        ByteArrayOutputStream copyBoas = new ByteArrayOutputStream();
        try {
            MimeMessage mimeMessage = toMimeMessage();
            mimeMessage.writeTo(copyBoas, ignoreList);
            return copyBoas.toString(EMAIL_CHARSET);
        } catch (IOException | MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the sending date
     * @throws MessagingException - if the message was not sent
     */
    public Date getSentDate() throws MessagingException {

        return this.mimeMessage.getSentDate();

    }


    public List<InternetAddress> getToInternetAddresses() {

        try {
            List<InternetAddress> internetAddresses = new ArrayList<>();
            Address[] allRecipients = this.mimeMessage.getRecipients(MimeMessage.RecipientType.TO);
            if (allRecipients == null) {
                return internetAddresses;
            }

            for (Address address : allRecipients) {
                if (address instanceof InternetAddress) {
                    internetAddresses.add((InternetAddress) address);
                }
            }
            return internetAddresses;
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

    }

    public InternetAddress getFromInternetAddress() {
        try {
            Address[] from = this.mimeMessage.getFrom();
            if (from != null && from.length >= 1) {
                return (InternetAddress) from[0];
            } else {
                return null;
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }


    public String getMessageId() {
        try {
            return this.mimeMessage.getMessageID();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @return the attachment part
     * ie returns :
     * * the mime message if this is a part
     * * or one of the part if this is a multipart message
     */
    public List<Part> getAttachments() {

        try {
            String contentTypeHeader = this.mimeMessage.getContentType();
            ContentType contentType = new ContentType(contentTypeHeader);

            List<Part> parts = new ArrayList<>();

            /**
             * The body may be the attachment
             */
            if (Part.ATTACHMENT.equalsIgnoreCase(this.mimeMessage.getDisposition())) {
                parts.add(this.mimeMessage);
                return parts;
            }

            /**
             * Multiparts
             */
            if (!contentType.getPrimaryType().equals("multipart")) {
                return parts;
            }

            Multipart multiPart = (Multipart) this.mimeMessage.getContent();

            for (int i = 0; i < multiPart.getCount(); i++) {
                MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                    parts.add(part);
                }
            }
            return parts;

        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return toEml();
    }

    public void addHeader(BMailMimeMessageHeader messageHeader, String value) {
        try {
            mimeMessage.addHeader(messageHeader.getName(), value);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<String> getPlainText() {

        Optional<Object> part = getPart(TEXT_MIME.toString());
        return part.map(Object::toString);

    }

    /**
     * @param baseType ie primaryType + '/' + subType;
     * @return the object
     */
    public Optional<Object> getPart(String baseType) {

        ContentType messageContentType = this.getContentType();

        if (messageContentType.getBaseType().equals(baseType)) {
            try {
                return Optional.of(mimeMessage.getContent());
            } catch (IOException | MessagingException e) {
                throw new RuntimeException(e);
            }
        }

        if (!messageContentType.getPrimaryType().equals("multipart")) {
            return Optional.empty();
        }

        // ie "multipart/alternative":
        try {
            Multipart multipart = (Multipart) mimeMessage.getContent();

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                ContentType contentType = new ContentType(bodyPart.getContentType());

                if (contentType.getBaseType().equals(baseType)) {
                    return Optional.of(bodyPart.getContent().toString());
                }
            }
        } catch (IOException | MessagingException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public Optional<String> getHtml() {
        Optional<Object> part = getPart("text/html");
        return part.map(Object::toString);
    }


    public static class builder {


        private String from;
        private String to;
        private String subject;
        private String bodyText;
        private String bodyHtml;
        private final Map<BMailMimeMessageHeader, String> headers = new HashMap<>();

        /**
         * content parts is there to
         * be able to construct a multipart email
         * You can't do when setting html or text body
         * because this is not the same
         */
        private final List<MimeBodyPart> attachmentParts = new ArrayList<>();
        private InternetAddress fromInternetAddress;
        private final List<InternetAddress> tos = new ArrayList<>();
        private final List<InternetAddress> ccs = new ArrayList<>();
        private final List<InternetAddress> bccs = new ArrayList<>();


        public BMailMimeMessage build() throws MessagingException {
            /**
             * Note: Passing a null session works also
             */
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage mimeMessage = new MimeMessage(session);
            if (this.from != null) {
                mimeMessage.setFrom(this.from);
            }
            if (this.fromInternetAddress != null) {
                mimeMessage.setFrom(this.fromInternetAddress);
            }
            if (this.to != null) {
                mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(this.to));
            }
            for (InternetAddress to : tos) {
                mimeMessage.addRecipient(Message.RecipientType.TO, to);
            }
            for (InternetAddress cc : ccs) {
                mimeMessage.addRecipient(Message.RecipientType.CC, cc);
            }
            for (InternetAddress bcc : bccs) {
                mimeMessage.addRecipient(Message.RecipientType.BCC, bcc);
            }

            if (this.subject != null) {
                mimeMessage.setSubject(subject);
            }
            /**
             * We set the content back
             * <p>
             * This is the only data that can be scalar or a list
             * <p>
             * Scalar: the content is only  HTML or Text content
             * Multipart: the body text, the body html and the attachement if any
             * <p>
             * All other data are in the mime message
             */
            try {

                /**
                 * https://javaee.github.io/javamail/FAQ
                 * You'll want to send a MIME multipart/alternative message.
                 * You construct such a message essentially the same way you construct a multipart/mixed message,
                 * using a MimeMultipart object constructed using new MimeMultipart("alternative").
                 * You then insert the text/plain body part as the first part in the multi-part
                 * and insert the text/html body part as the second part in the multipart.
                 * You'll need to construct the plain and html parts yourself to have appropriate content.
                 * See RFC2046 for details of the structure of such a message.
                 */
                int partCount = attachmentParts.size();
                boolean bodyPartFound = false;
                if (bodyText != null) {
                    bodyPartFound = true;
                    partCount++;
                }
                if (bodyHtml != null) {
                    bodyPartFound = true;
                    partCount++;
                }
                if (!bodyPartFound) {
                    throw new RuntimeException("A body part should be present (html or txt)");
                }
                if (partCount == 1) {

                    if (bodyHtml != null) {
                        mimeMessage.setContent(bodyHtml, HTML_MIME.toString());
                    }
                    if (bodyText != null) {
                        mimeMessage.setContent(bodyText, TEXT_MIME.toString());
                    }

                } else {

                    // create the Multipart and its parts to it

                    Multipart mp = new MimeMultipart("alternative");

                    String plain = bodyText;
                    if (plain != null) {
                        MimeBodyPart textBodyPart = new MimeBodyPart();
                        textBodyPart.setText(plain, EMAIL_CHARSET.name(), TEXT_MIME.getSubType());
                        mp.addBodyPart(textBodyPart);
                    }

                    String html = bodyHtml;
                    if (html != null) {
                        MimeBodyPart htmlBodyPart = new MimeBodyPart();
                        htmlBodyPart.setText(html, EMAIL_CHARSET.name(), HTML_MIME.getSubType());
                        mp.addBodyPart(htmlBodyPart);
                    }

                    for (MimeBodyPart attachmentPart : attachmentParts) {
                        mp.addBodyPart(attachmentPart);
                    }

                    // add the Multipart to the message
                    mimeMessage.setContent(mp);

                }

                /**
                 * Headers
                 */
                for (Map.Entry<BMailMimeMessageHeader, String> header : headers.entrySet()) {
                    mimeMessage.addHeader(header.getKey().toString(), header.getValue());
                }

                // Computes additional headers
                // Updates the appropriate header fields of this message to be consistent with the message's contents.
                mimeMessage.saveChanges();

            } catch (
                    MessagingException e) {
                throw new RuntimeException(e);
            }
            return new BMailMimeMessage(mimeMessage);
        }

        public builder setFrom(String from) {
            this.from = from;
            return this;
        }

        public builder setTo(String to) {
            this.to = to;
            return this;
        }

        public builder setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public builder setBodyPlainText(String plain) {
            this.bodyText = plain;
            return this;
        }

        public builder setBodyHtml(String html) {
            this.bodyHtml = html;
            return this;
        }


        /**
         * @param absolutePath an absolute path
         * @return the message
         * @throws MessagingException if any errors occurs
         */
        public builder addAttachment(Path absolutePath) throws MessagingException {

            MimeBodyPart attachmentPart = new MimeBodyPart();

            attachmentPart.setFileName(absolutePath.getFileName().toString());
            DataHandler dataHandler;
            try {
                dataHandler = new DataHandler(absolutePath.toUri().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            attachmentPart.setDataHandler(dataHandler);
            try {
                attachmentPart.setHeader("Content-Type", MediaTypes.detectMediaType(absolutePath).toString());
            } catch (MediaTypePathNotAbsoluteException e) {
                throw new MessagingException(e.getMessage(), e);
            }
            this.attachmentParts.add(attachmentPart);
            return this;

        }

        public builder addHeader(BMailMimeMessageHeader messageHeader, String value) {
            this.headers.put(messageHeader, value);
            return this;
        }

        public builder setFromInternetAddress(InternetAddress from) {
            this.fromInternetAddress = from;
            return this;
        }

        public builder addToInternetAddresses(List<InternetAddress> to) {
            this.tos.addAll(to);
            return this;
        }

        public builder addCcInternetAddresses(List<InternetAddress> cc) {
            this.ccs.addAll(cc);
            return this;
        }

        public builder addBccInternetAddresses(List<InternetAddress> bcc) {
            this.bccs.addAll(bcc);
            return this;
        }

    }
}
