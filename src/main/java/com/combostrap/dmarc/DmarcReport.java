package com.combostrap.dmarc;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * An object that encapsulates the DMARC
 * report file name and the xml feedback.
 */
public class DmarcReport {

  private final String attachementFileName;
  private final String feedbackXml;
  private final DmarcFeedback dmarcFeedback;

  public DmarcReport(String attachementFileName, String xmlString) throws XMLStreamException, IOException {
    this.attachementFileName = attachementFileName;
    this.feedbackXml = xmlString;
    this.dmarcFeedback = this.builtDmarcFeedback(xmlString);
  }

  public static DmarcReport create(String fileHeaderName, String feedbackXml) throws XMLStreamException, IOException {
    return new DmarcReport(fileHeaderName, feedbackXml);
  }


  /**
   * @return a DmarcFeedback object
   * @throws XMLStreamException - bad xml
   * @throws IOException        - bad io
   */
  private DmarcFeedback builtDmarcFeedback(String feedbackXmlString) throws XMLStreamException, IOException {


    DmarcFeedback dmarcFeedback = new DmarcFeedback();

    /**
     * A Dmarc Xml string has multiple record on the same level.
     * It's therefore not serializable directly with the Jackson library
     * because it should be an array.
     * <p>
     * We loop therefore on each node to build the object
     */
    XmlMapper xm = new XmlMapper();
    XMLInputFactory xif = XMLInputFactory.newInstance();
    XMLStreamReader xr = xif.createXMLStreamReader(new ByteArrayInputStream(feedbackXmlString.getBytes(StandardCharsets.UTF_8)));


    try {

      while (xr.hasNext()) {
        xr.next();
        if (xr.getEventType() == START_ELEMENT) {
          String localName = xr.getLocalName();
          switch (localName) {
            case "report_metadata": {
              DmarcFeedbackMetadata dmarcFeedbackMetadata = xm.readValue(xr, DmarcFeedbackMetadata.class);
              dmarcFeedback.setReportMetadata(dmarcFeedbackMetadata);
              break;
            }
            case "policy_published": {
              DmarcFeedbackPolicyPublished dmarcFeedbackPolicyPublished = xm.readValue(xr, DmarcFeedbackPolicyPublished.class);
              dmarcFeedback.setReportPolicyPublished(dmarcFeedbackPolicyPublished);
              break;
            }
            default:
              /**
               * We don't need more data to build the {@link net.bytle.edge.routehandler.DmarcHandler#getRemoteUniquePath(DmarcFeedback)} remote path
               * Record are not parsed for now
               */
              break;
          }

        }
      }
      return dmarcFeedback;

    } finally {
      xr.close();
    }
  }



  /**
   * @return the original attachment file name
   */
  @SuppressWarnings("unused")
  public String getAttachementFileName() {
    return attachementFileName;
  }

  @SuppressWarnings("unused")
  public String getFeedbackXml() {
    return feedbackXml;
  }

  public DmarcFeedback getFeedbackObject() {
    return this.dmarcFeedback;
  }

}
