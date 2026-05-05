package com.combostrap.dmarc;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import jakarta.mail.MessagingException;
import net.bytle.email.BMailMimeMessage;
import net.bytle.exception.IllegalStructure;
import net.bytle.smtp.milter.DmarcMilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

@ExtendWith(VertxExtension.class)
public class DmarcManagerTest {


  public static final String DMARC_RESSOURCE_ROOT = "/dmarc";

  @Test
  public void zipDmarcFromEmlTest() throws IllegalStructure, MessagingException, IOException {

    String result = getResourceAsString(DMARC_RESSOURCE_ROOT + "/dmarc-zip.eml");
    BMailMimeMessage fromEml = BMailMimeMessage.createFromEml(result);
    DmarcReport dmarcReport = DmarcManager.getDmarcReportFromMime(fromEml);
    String fileName = DmarcMilter.getRemoteUniquePath(dmarcReport.getFeedbackObject());
    Assertions.assertEquals("2023/2023-09/2023-09-14T02:00:00!2023-09-15T01:59:59!google.com!tabulify.com.xml", fileName);


  }

  @Test
  public void gzipDmarcFromEmlTest() throws IllegalStructure, MessagingException, IOException {

    String result = getResourceAsString(DMARC_RESSOURCE_ROOT + "/dmarc-gzip.eml");
    BMailMimeMessage fromEml = BMailMimeMessage.createFromEml(result);
    DmarcReport dmarcReport = DmarcManager.getDmarcReportFromMime(fromEml);
    String fileName = DmarcMilter.getRemoteUniquePath(dmarcReport.getFeedbackObject());
    Assertions.assertEquals("2023/2023-04/2023-04-20T06:00:04!2023-04-21T06:00:05!esa1.hc4958-69.iphmx.com!combostrap.com.xml", fileName);


  }

  @Test
  public void zipDmarcFromJsonEmailTest() throws IllegalStructure {

    String result = getResourceAsString(DMARC_RESSOURCE_ROOT + "/dmarc-zip.eml.json");
    JsonObject jsonObject = new JsonObject(result);
    DmarcReport dmarcReport = DmarcManager.getDmarcReportFromJsonEmail(jsonObject);
    String fileName = DmarcMilter.getRemoteUniquePath(dmarcReport.getFeedbackObject());
    Assertions.assertEquals("2023/2023-09/2023-09-17T02:00:00!2023-09-18T01:59:59!google.com!bytle.net.xml", fileName);


  }

  @Test
  public void gzipDmarcFromJsonEmailTest() throws IllegalStructure {

    String result = getResourceAsString(DMARC_RESSOURCE_ROOT + "/dmarc-gzip.eml.json");
    JsonObject jsonObject = new JsonObject(result);
    DmarcReport dmarcReport = DmarcManager.getDmarcReportFromJsonEmail(jsonObject);
    String fileName = DmarcMilter.getRemoteUniquePath(dmarcReport.getFeedbackObject());
    Assertions.assertEquals("2023/2023-06/2023-06-14T06:00:03!2023-06-15T06:00:04!esa2.hc4958-69.iphmx.com!combostrap.com.xml", fileName);

  }


  public static String getResourceAsString(String name) {
    InputStream inputStream = DmarcManagerTest.class.getResourceAsStream(name);
    assert inputStream != null;
    Scanner s = new Scanner(inputStream).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

  @Test
  public void testFullXml() throws XMLStreamException, IOException {
    String xml = getResourceAsString(DMARC_RESSOURCE_ROOT + "/dmarc.xml");
    DmarcReport dmarcReport = DmarcReport.create("dmarc", xml);
    String fileName = DmarcMilter.getRemoteUniquePath(dmarcReport.getFeedbackObject());
    Assertions.assertEquals(fileName, "2023/2023-09/2023-09-17T02:00:00!2023-09-18T01:59:59!google.com!bytle.net.xml");
  }

}
