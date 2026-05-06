package com.combostrap.smtp;

import com.combostrap.common.JavaEnvs;
import com.combostrap.email.BMailMimeMessage;
import com.combostrap.type.Strings;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.converter.EmailConverter;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.combostrap.dmarc.DmarcManagerTest.DMARC_RESSOURCE_ROOT;
import static com.combostrap.dmarc.DmarcManagerTest.getResourceAsString;


/**
 * <a href="https://datatracker.ietf.org/doc/html/rfc2821#appendix-D">Scenarios to test</a>
 */
@ExtendWith(VertxExtension.class)
class SmtpServerTest {

    static Vertx vertx;
    private static String deploymentId;
    private static final String LOCAL_DOMAIN = "eraldy.dev";
    private static final String LOCAL_USER_NAME = "memory";
    private static final String LOCAL_USER_EMAIL = LOCAL_USER_NAME + "@" + LOCAL_DOMAIN;

    private static SmtpVerticle verticle;
    private static SmtpServer smtpServer;


    // Deploy the verticle and execute the test methods when the verticle
    // is successfully deployed
    @BeforeEach
    void deploy_verticle(VertxTestContext testContext) {
        if (deploymentId != null) {
            testContext.completeNow();
            return;
        }
        vertx = Vertx.vertx();
        verticle = new SmtpVerticle();
        JsonObject verticleConfig = new JsonObject();
        verticleConfig.put("sessionReplay", false);
        // delivery cannot be immediate otherwise the test will finish before delivery
        // why ? As the delivery is running, the delivery run is returning immediately
        verticleConfig.put(SmtpDelivery.DELIVERY_RUN_AFTER_RECEPTION_CONF, false);
        // no periodic delivery run otherwise it may interfere with the test
        // that tries to retrieve the message
        verticleConfig.put(SmtpDelivery.DELIVERY_RUN_INTERVAL_KEY, 0);
        vertx.deployVerticle(
                verticle,
                new DeploymentOptions().setConfig(verticleConfig),
                ar -> {
                    if (ar.succeeded()) {
                        deploymentId = ar.result();
                        smtpServer = verticle.getSmtpServer();
                        testContext.completeNow();
                    } else {
                        testContext.failNow(ar.cause());
                    }
                }
        );
    }

    @Test
    void plainLocalDeliveryTest() throws Throwable {

        VertxTestContext testContext = new VertxTestContext();
        String destination = LOCAL_USER_NAME + "@" + LOCAL_DOMAIN;
        String body = "Yolo";


        int sessionTimeout = 3 * 1000;
        if (JavaEnvs.isIsIdeDebugging()) {
            sessionTimeout = sessionTimeout * 120;
        }

        try (Mailer mailer = MailerBuilder
                .withSMTPServerPort(SmtpService.PORT_2525)
                .withTransportStrategy(TransportStrategy.SMTP)
                .withSessionTimeout(sessionTimeout)
                .withDebugLogging(true)
                .buildMailer()) {

            Email email = EmailBuilder.startingBlank()
                    .from("foo@gmail.com")
                    .to(destination)
                    .withPlainText(body)
                    .buildEmail();
            mailer.sendMail(email);

            checkReceivedMessage(testContext, email);
        }


        awaitCompletion(testContext);

    }

    /**
     * To not lose the IDE integration
     * (ie a double click on the test navigates to the method)
     * we handle the completion
     */
    private static void awaitCompletion(VertxTestContext testContext) throws Throwable {
        int timeout = 30;
        if(JavaEnvs.isIsIdeDebugging()) {
            timeout = timeout * 120;
        }
        Assertions.assertTrue(testContext.awaitCompletion(timeout, TimeUnit.SECONDS), "Should finish in " + timeout + " seconds");
        if (testContext.failed()) {
            throw testContext.causeOfFailure();
        }
    }

    private void checkReceivedMessage(VertxTestContext testContext, Email mimeMessage) {

        String destination = mimeMessage.getToRecipients().get(0).getAddress();
        smtpServer
                .getSmtpDeliveryQueue()
                .run()
                .onComplete(ar -> {
                    List<SmtpMessage> mimeMessageList;
                    try {
                        mimeMessageList = smtpServer.pumpMessagesForUser(destination);
                    } catch (SmtpException e) {
                        testContext.failNow(e);
                        return;
                    }
                    testContext.verify(() -> {
                        Assertions.assertEquals(1, mimeMessageList.size(), "Message list size is one");
                        Object object = mimeMessageList.get(0).getObject();
                        Assertions.assertInstanceOf(BMailMimeMessage.class, object, "Message is a mime message");
                        BMailMimeMessage receivedMessage = (BMailMimeMessage) object;
                        /**
                         * EOL at the end of a message body are not send by JMail
                         * We compare the body without the end of line then
                         */
                        String expectedNormalized = Strings.createFromString(mimeMessage.getPlainText()).rtrimEol().toString();
                        String receivedNormalized = Strings.createFromString(receivedMessage.getPlainText().orElse("")).rtrimEol().toString();
                        testContext.verify(() -> Assertions.assertEquals(expectedNormalized, receivedNormalized, "Plain text is equal"));
                    });
                    testContext.completeNow();
                });
    }

    /**
     * Body may be sent in multiple buffer
     * when they are too big
     */
    @Test
    void plainLocalDeliveryLongBodyTest() throws Throwable {
        VertxTestContext testContext = new VertxTestContext();
        String destination = LOCAL_USER_NAME + "@" + LOCAL_DOMAIN;
        String body = Strings.createFromString("Alice and Bob\r\n").multiply(1000)
                .toString();


        try (Mailer mailer = MailerBuilder
                .withSMTPServerPort(SmtpService.PORT_2525)
                .withTransportStrategy(TransportStrategy.SMTP)
                .withSessionTimeout(10 * 1000)
                .withDebugLogging(true)
                .buildMailer()) {

            Email email = EmailBuilder.startingBlank()
                    .from("foo@gmail.com")
                    .to(destination)
                    .withPlainText(body)
                    .buildEmail();
            mailer.sendMail(email);

            checkReceivedMessage(testContext, email);
        }

        awaitCompletion(testContext);

    }

    @Test
    void plainChunkLocalDelivery() throws Throwable {
        VertxTestContext testContext = new VertxTestContext();


        int sizeInBytes = 20;

        try (Mailer mailer = MailerBuilder
                .withSMTPServerPort(SmtpService.PORT_2525)
                .withTransportStrategy(TransportStrategy.SMTP)
                .withProperty("mail.smtp.chunksize", sizeInBytes)
                .withSessionTimeout(10 * 1000)
                .withDebugLogging(true)
                .buildMailer()) {

            Email email = EmailBuilder.startingBlank()
                    .from("foo@gmail.com")
                    .to(LOCAL_USER_EMAIL)
                    .withPlainText("Yolo Yolo Yolo YoloYolo " +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo")
                    .buildEmail();
            mailer.sendMail(email);

            checkReceivedMessage(testContext, email);
        }

        awaitCompletion(testContext);
    }

    @Test
    void plainStartTLSLocalDeliveryTest() throws Throwable {
        VertxTestContext testContext = new VertxTestContext();


        try (Mailer mailer = MailerBuilder
                .withSMTPServer("mx1." + LOCAL_DOMAIN, SmtpService.PORT_2525)
                .withTransportStrategy(TransportStrategy.SMTP_TLS) // mandatory startTls
                .withSessionTimeout(10 * 1000)
                .withDebugLogging(true)
                .buildMailer()) {

            Email email = EmailBuilder.startingBlank()
                    .from("foo@gmail.com")
                    .to(LOCAL_USER_EMAIL)
                    .withPlainText("Yolo Yolo Yolo YoloYolo " +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo")
                    .buildEmail();
            mailer.sendMail(email);

            checkReceivedMessage(testContext, email);
        }

        awaitCompletion(testContext);

    }

    /**
     * With TLS
     */
    @Test
    void tlsAuthLocalDeliveryTest() throws Throwable {

        VertxTestContext testContext = new VertxTestContext();

        try (Mailer mailer = MailerBuilder
                .withSMTPServer("mx1." + LOCAL_DOMAIN, SmtpService.PORT_587)
                .withSMTPServerUsername(LOCAL_USER_NAME)
                .withSMTPServerPassword("secret")
                .withTransportStrategy(TransportStrategy.SMTPS)
                .withSessionTimeout(10 * 1000)
                .withDebugLogging(true)
                .trustingAllHosts(true)
                .buildMailer()) {

            Email email = EmailBuilder.startingBlank()
                    .from("foo@gmail.com")
                    .to(LOCAL_USER_EMAIL)
                    .withPlainText("Yolo Yolo Yolo YoloYolo " +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
                            "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo")
                    .buildEmail();
            mailer.sendMail(email);

            checkReceivedMessage(testContext, email);
        }

        awaitCompletion(testContext);
    }

    @Test
    void listSmtpCapabilities() {
        // gerardnico@inbox.mailbrew.com
        // SES: https://docs.aws.amazon.com/general/latest/gr/ses.html
        // MX: inbound-smtp.us-east-1.amazonaws.com.
    }

    @Test
    void dmarcTest() throws Throwable {
        VertxTestContext testContext = new VertxTestContext();
        String result = getResourceAsString(DMARC_RESSOURCE_ROOT + "/dmarc-zip.eml");
        String dmarcUser = "dmarc-memory@" + LOCAL_DOMAIN;


        try (Mailer mailer = MailerBuilder
                .withSMTPServerPort(SmtpService.PORT_2525)
                .withTransportStrategy(TransportStrategy.SMTP)
                .withSessionTimeout(10 * 1000)
                .withDebugLogging(true)
                .buildMailer()) {

            Email emailEml = EmailConverter.emlToEmail(result);
            Email email = EmailBuilder.copying(emailEml)
                    .to(dmarcUser)
                    .buildEmail();

            mailer.sendMail(email);

            checkReceivedMessage(testContext, email);
        }


        smtpServer
                .getSmtpDeliveryQueue()
                .run()
                .onComplete(ar -> {
                    List<SmtpMessage> smtpMessages;
                    try {
                        smtpMessages = smtpServer.pumpMessagesForUser(dmarcUser);
                    } catch (SmtpException e) {
                        testContext.failNow(e);
                        return;
                    }
                    testContext.verify(() -> {
                        Assertions.assertEquals(1, smtpMessages.size(), "Message list size is one");
                        SmtpMessage smtpMessage = smtpMessages.get(0);
                        Assertions.assertEquals("xml", smtpMessage.getMediaType().getSubType());
                        Assertions.assertEquals("2023/2023-09/2023-09-14T02:00:00!2023-09-15T01:59:59!google.com!tabulify.com.xml", smtpMessage.getPath());
                    });
                    testContext.completeNow();
                });


        awaitCompletion(testContext);

    }


    @Disabled
    @Test
    void dmarcS3Test() throws Throwable {
        VertxTestContext testContext = new VertxTestContext();
        String result = getResourceAsString(DMARC_RESSOURCE_ROOT + "/dmarc-zip.eml");
        String dmarcUser = "dmarc-s3@" + LOCAL_DOMAIN;

        try (Mailer mailer = MailerBuilder
                .withSMTPServerPort(SmtpService.PORT_2525)
                .withTransportStrategy(TransportStrategy.SMTP)
                .withSessionTimeout(10 * 1000)
                .withDebugLogging(true)
                .buildMailer()) {

            Email emailEml = EmailConverter.emlToEmail(result);
            Email email = EmailBuilder.copying(emailEml)
                    .to(dmarcUser)
                    .buildEmail();

            mailer.sendMail(email);

            checkReceivedMessage(testContext, email);
        }

        smtpServer
                .getSmtpDeliveryQueue()
                .run()
                .onComplete(ar -> testContext.completeNow());


        awaitCompletion(testContext);

    }
}
