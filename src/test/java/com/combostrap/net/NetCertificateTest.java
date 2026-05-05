package com.combostrap.net;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.impl.KeyStoreHelper;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.cert.Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ExtendWith(VertxExtension.class)
public class NetCertificateTest {



  @Test
  public void testSSL() throws Throwable {
    VertxTestContext testContext = new VertxTestContext();

    NetClientOptions options = new NetClientOptions();
    options.setSsl(true);
    NetClient client = Vertx.vertx().createNetClient(options);
    client.connect(465, "smtp.gmail.com", r -> {
      if (r.succeeded()) {
        NetSocket ns = r.result();
        List<Certificate> certs;
        try {
          certs = ns.peerCertificates();
          for (Certificate cert : certs) {
            cnOf(cert);
          }
          testContext.completeNow();
        } catch (Exception ex) {
          testContext.failNow(ex);
        }
      }
    });
    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  @Test
  public void testHttps() throws Throwable {
    VertxTestContext testContext = new VertxTestContext();

    NetClientOptions options = new NetClientOptions();
    options.setSsl(true);
    NetClient client = Vertx.vertx().createNetClient(options);
    client.connect(443, "datacadamia.com", r -> {
      if (r.succeeded()) {
        NetSocket ns = r.result();
        List<Certificate> certs;
        try {
          certs = ns.peerCertificates();
          for (Certificate cert : certs) {
            String name = cnOf(cert);
            Assertions.assertNotNull(name);
            System.out.println(name);
          }
          testContext.completeNow();
        } catch (Exception ex) {
          testContext.failNow(ex);
        }
      }
    });
    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  /**
   * When looking for upgrade
   * <a href="https://gist.github.com/alexlehm/a7527a1eac119cca606e">...</a>
   *
   */
  @Test
  public void testTLS() throws Throwable {

    VertxTestContext testContext = new VertxTestContext();
    NetClientOptions options = new NetClientOptions();
    NetClient client = Vertx.vertx().createNetClient(options);
    client.connect(465, "smtp.gmail.com", r -> {
      if (r.succeeded()) {
        NetSocket ns = r.result();
        ns.upgradeToSsl(v -> {
          List<Certificate> certs;
          try {
            certs = ns.peerCertificates();
            for (Certificate cert : certs) {
              cnOf(cert);
            }
            testContext.completeNow();
          } catch (Exception ex) {
            testContext.failNow(ex);
          }
        });
      }
    });
    Assertions.assertTrue(testContext.awaitCompletion(200, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  public static String cnOf(Certificate cert) throws Exception {
    if (cert instanceof java.security.cert.X509Certificate) {
      String dn = ((java.security.cert.X509Certificate)cert).getSubjectX500Principal().getName();
      List<String> names = KeyStoreHelper.getX509CertificateCommonNames(dn);
      return names.isEmpty() ? null : names.get(0);
    }
    return null;
  }

}
