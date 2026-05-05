package com.combostrap.smtp;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import net.bytle.email.BMailMimeMessage;
import net.bytle.email.BMailSmtpClient;
import net.bytle.email.BMailSmtpConnection;
import net.bytle.email.BMailStartTls;
import net.bytle.exception.NotFoundException;
import net.bytle.type.MediaTypes;
import net.bytle.type.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.bytle.dmarc.DmarcManagerTest.DMARC_RESSOURCE_ROOT;
import static net.bytle.dmarc.DmarcManagerTest.getResourceAsString;

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
    verticleConfig.put(SmtpServer.SESSION_REPLAY_CONF, false);
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
    BMailMimeMessage bMailMimeMessage = BMailMimeMessage.createFromBuilder()
      .setFrom("foo@gmail.com")
      .setTo(destination)
      .setBodyPlainText(body)
      .build();

    try (BMailSmtpConnection transport = BMailSmtpClient.create()
      .setPort(SmtpService.PORT_2525)
      .setStartTls(BMailStartTls.NONE)
      .setDebug(true)
      .build()
      .getTransportConnection()
    ) {
      //transport.sayEhlo("hostname");
      transport.sendMessage(bMailMimeMessage);
    }

    checkReceivedMessage(testContext, bMailMimeMessage);

    awaitCompletion(testContext);

  }

  /**
   * To not lose the IDE integration
   * (ie a double click on the test navigates to the method)
   * we handle the completion
   */
  private static void awaitCompletion(VertxTestContext testContext) throws Throwable {
    int timeout = 30;
    Assertions.assertTrue(testContext.awaitCompletion(timeout, TimeUnit.SECONDS), "Should finish in " + timeout + " seconds");
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private void checkReceivedMessage(VertxTestContext testContext, BMailMimeMessage mimeMessage) {

    String destination = mimeMessage.getToInternetAddress().getAddress();
    smtpServer
      .getSmtpDeliveryQueue()
      .run()
      .onComplete(ar -> {
        List<SmtpMessage> mimeMessageList;
        try {
          mimeMessageList = smtpServer.pumpMessagesForUser(destination);
        } catch (SmtpException | NotFoundException e) {
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
          String receivedNormalized = Strings.createFromString(receivedMessage.getPlainText()).rtrimEol().toString();
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
    BMailMimeMessage emailBuilder = BMailMimeMessage.createFromBuilder()
      .setFrom("foo@gmail.com")
      .setTo(destination)
      .setBodyPlainText(body)
      .build();

    try (BMailSmtpConnection transport = BMailSmtpClient.create()
      .setPort(SmtpService.PORT_2525)
      .setStartTls(BMailStartTls.NONE)
      .setDebug(true)
      .build()
      .getTransportConnection()
    ) {
      //transport.sayEhlo("hostname");
      transport.sendMessage(emailBuilder);
    }

    checkReceivedMessage(testContext, emailBuilder);
    awaitCompletion(testContext);

  }

  @Test
  void plainChunkLocalDelivery() throws Throwable {
    VertxTestContext testContext = new VertxTestContext();
    BMailMimeMessage message = BMailMimeMessage.createFromBuilder()
      .setFrom("foo@gmail.com")
      .setTo(LOCAL_USER_EMAIL)
      .setBodyPlainText("Yolo Yolo Yolo YoloYolo " +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo"
      )
      .build();

    int sizeInBytes = 20;
    BMailSmtpClient client = BMailSmtpClient.create()
      .setPort(SmtpService.PORT_2525)
      .setDebug(true)
      .setWithChunkingInSize(sizeInBytes)
      .setTimeout(1000000)
      .setStartTls(BMailStartTls.NONE)
      .build();
    try (BMailSmtpConnection transport = client.getTransportConnection()) {
      transport.sendMessage(message);
    }
    checkReceivedMessage(testContext, message);
    awaitCompletion(testContext);
  }

  @Test
  void plainStartTLSLocalDeliveryTest() throws Throwable {
    VertxTestContext testContext = new VertxTestContext();
    BMailMimeMessage email = BMailMimeMessage.createFromBuilder()
      .setFrom("foo@gmail.com")
      .setTo(LOCAL_USER_EMAIL)
      .setBodyPlainText("Yolo Yolo Yolo YoloYolo " +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo"
      )
      .build();

    BMailSmtpClient client = BMailSmtpClient.create()
      .setHost("mx1." + LOCAL_DOMAIN)
      .setPort(SmtpService.PORT_2525)
      .setDebug(true)
      .setStartTls(BMailStartTls.REQUIRE)
      .setTrustAll(true)
      .setTimeout(1000)
      .build();

    try (BMailSmtpConnection bMailTransport = client.getTransportConnection()) {
      bMailTransport.sendMessage(email);
    }

    checkReceivedMessage(testContext, email);
    awaitCompletion(testContext);

  }

  /**
   * With TLS
   */
  @Test
  void tlsAuthLocalDeliveryTest() throws Throwable {

    VertxTestContext testContext = new VertxTestContext();
    BMailMimeMessage email = BMailMimeMessage.createFromBuilder()
      .setFrom("foo@gmail.com")
      .setTo(LOCAL_USER_EMAIL)
      .setBodyPlainText("Yolo Yolo Yolo YoloYolo " +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo" +
        "YoloYolo YoloYolo YoloYolo YoloYolo YoloYolo Yolo"
      )
      .build();
    BMailSmtpClient client = BMailSmtpClient.create()
      .setHost("mx1." + LOCAL_DOMAIN)
      .setPort(SmtpService.PORT_587)
      .isSslConnection(true)
      .setDebug(true)
      .setTrustAll(true)
      .setTimeout(1000)
      .setUsername(LOCAL_USER_NAME)
      .setPassword("secret")
      .build();
    try (BMailSmtpConnection bMailTransport = client.getTransportConnection()) {
      bMailTransport.sendMessage(email);
    }
    checkReceivedMessage(testContext, email);
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
    BMailMimeMessage dmarcMime = BMailMimeMessage.createFromEml(result)
      .setTo(dmarcUser);
    try (BMailSmtpConnection transport = BMailSmtpClient.create()
      .setPort(SmtpService.PORT_2525)
      .setStartTls(BMailStartTls.NONE)
      .setDebug(true)
      .build()
      .getTransportConnection()
    ) {
      //transport.sayEhlo("hostname");
      transport.sendMessage(dmarcMime);
    }

    smtpServer
      .getSmtpDeliveryQueue()
      .run()
      .onComplete(ar -> {
        List<SmtpMessage> smtpMessages;
        try {
          smtpMessages = smtpServer.pumpMessagesForUser(dmarcUser);
        } catch (SmtpException | NotFoundException e) {
          testContext.failNow(e);
          return;
        }
        testContext.verify(() -> {
          Assertions.assertEquals(1, smtpMessages.size(), "Message list size is one");
          SmtpMessage smtpMessage = smtpMessages.get(0);
          Assertions.assertEquals(MediaTypes.TEXT_XML, smtpMessage.getMediaType());
          Assertions.assertEquals("2023/2023-09/2023-09-14T02:00:00!2023-09-15T01:59:59!google.com!tabulify.com.xml", smtpMessage.getPath());
        });
        testContext.completeNow();
      });


    awaitCompletion(testContext);

  }

//  @Disabled
  @Test
  void dmarcBeauBytleTest() throws Throwable {
    VertxTestContext testContext = new VertxTestContext();
    String result = getResourceAsString(DMARC_RESSOURCE_ROOT + "/dmarc-zip.eml");
    String dmarcUser = "dmarc@inbox.eraldy.com";
    BMailMimeMessage dmarcMime = BMailMimeMessage.createFromEml(result)
      .setTo(dmarcUser);
    try (BMailSmtpConnection transport = BMailSmtpClient.create()
      .setPort(25025)
      .setHost("beau.bytle.net")
      .setStartTls(BMailStartTls.NONE)
      .setDebug(true)
      .build()
      .getTransportConnection()
    ) {
      //transport.sayEhlo("hostname");
      transport.sendMessage(dmarcMime);
    }

    smtpServer
      .getSmtpDeliveryQueue()
      .run()
      .onComplete(ar -> testContext.completeNow());


    awaitCompletion(testContext);

  }

  @Disabled
  @Test
  void dmarcS3Test() throws Throwable {
    VertxTestContext testContext = new VertxTestContext();
    String result = getResourceAsString(DMARC_RESSOURCE_ROOT + "/dmarc-zip.eml");
    String dmarcUser = "dmarc-s3@" + LOCAL_DOMAIN;
    BMailMimeMessage dmarcMime = BMailMimeMessage.createFromEml(result)
      .setTo(dmarcUser);
    try (BMailSmtpConnection transport = BMailSmtpClient.create()
      .setPort(SmtpService.PORT_2525)
      .setStartTls(BMailStartTls.NONE)
      .setDebug(true)
      .build()
      .getTransportConnection()
    ) {
      //transport.sayEhlo("hostname");
      transport.sendMessage(dmarcMime);
    }

    smtpServer
      .getSmtpDeliveryQueue()
      .run()
      .onComplete(ar -> testContext.completeNow());


    awaitCompletion(testContext);

  }
}
