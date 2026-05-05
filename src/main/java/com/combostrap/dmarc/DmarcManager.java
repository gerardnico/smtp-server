package com.combostrap.dmarc;

import com.combostrap.email.BMailMimeMessage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.ContentType;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DmarcManager {


  private static final String ZIP_CONTENT_TYPE = "application/zip";
  private static final String GZIP_CONTENT_TYPE = "application/gzip";

  /**
   * Parse the <a href="https://forwardemail.net/en/faq#do-you-support-webhooks">forwardEmail json data</a> into
   * {@link DmarcReport}
   *
   * @param forwardEmailJsonObject - A Json Object with the forwardEmail structure
   * @return the Dmarc Report
   * @throws DmarcIllegalStructure if any error
   */
  public static DmarcReport getDmarcReportFromJsonEmail(JsonObject forwardEmailJsonObject) throws DmarcIllegalStructure {

    JsonArray jsonArrayAttachements = forwardEmailJsonObject.getJsonArray("attachments");
    if (jsonArrayAttachements == null) {
      throw new DmarcIllegalStructure("No attachements");
    }

    if (jsonArrayAttachements.size() == 0) {
      throw new DmarcIllegalStructure("No file found in the zip attachement.");
    }

    JsonObject attachement = jsonArrayAttachements.getJsonObject(0);
    String contentType = attachement.getString("contentType");
    if (!(contentType.equals(ZIP_CONTENT_TYPE) || contentType.equals(GZIP_CONTENT_TYPE))) {
      throw new DmarcIllegalStructure("Attachement is not a zip file");
    }
    JsonObject contentObject = attachement.getJsonObject("content");
    String contentObjectType = contentObject.getString("type");
    if (!contentObjectType.equals("Buffer")) {
      throw new DmarcIllegalStructure("Attachement type is not buffer");
    }

    JsonArray data = contentObject.getJsonArray("data");
    byte[] bytes = new byte[data.size()];
    for (int j = 0; j < data.size(); j++) {
      bytes[j] = data.getInteger(j).byteValue();
    }

    String xmlString = null;
    String xmlFileName = null;
    switch (contentType) {
      case ZIP_CONTENT_TYPE:
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
          ZipEntry localFileHeader;
          while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
            if (localFileHeader.isDirectory()) {
              continue;
            }

            ArrayList<Byte> fileAttachementBytes = new ArrayList<>();
            int readLen;
            while ((readLen = zipInputStream.read()) != -1) {
              fileAttachementBytes.add((byte) readLen);
            }
            byte[] fileAttachementBytesArray = new byte[fileAttachementBytes.size()];
            for (int k = 0; k < fileAttachementBytes.size(); k++) {
              fileAttachementBytesArray[k] = fileAttachementBytes.get(k);
            }

            xmlString = new String(fileAttachementBytesArray, StandardCharsets.UTF_8);
            xmlFileName = localFileHeader.getName();

          }
        } catch (IOException e) {
          throw new DmarcIllegalStructure("Unable to read the zip data attachment", e);
        }
        break;
      case GZIP_CONTENT_TYPE:
        try {
          xmlFileName = attachement.getString("filename");
          xmlString = Gzip.decompress(bytes);
        } catch (IOException e) {
          throw new DmarcIllegalStructure("Unable to read the gzip data attachment", e);
        }
        break;
      default:
        throw new DmarcIllegalStructure("Attachement Content Type (" + contentType + ") not supported");
    }

    try {
      return DmarcReport.create(xmlFileName, xmlString);
    } catch (XMLStreamException | IOException e) {
      throw new DmarcIllegalStructure("Unable to create the Dmarc Report", e);
    }


  }

  public static DmarcReport getDmarcReportFromMime(BMailMimeMessage mimeMessage) throws DmarcIllegalStructure {
    List<Part> attachments = mimeMessage.getAttachments();
    if (attachments.isEmpty()) {
      throw new DmarcIllegalStructure("A dmarc message should have at minimal an attachment");
    }

    for (Part mailPart : attachments) {

      ContentType contentType;
      try {
        contentType = new ContentType(mailPart.getContentType());
      } catch (MessagingException e) {
        continue;
      }
      String xmlString = null;
      String xmlFileName = null;
      InputStream inputStream;
      String baseType = contentType.getBaseType();
      switch (baseType) {
        case ZIP_CONTENT_TYPE:

          try {
            inputStream = mailPart.getInputStream();
          } catch (IOException | MessagingException e) {
            throw new DmarcIllegalStructure(e);
          }
          try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry localFileHeader;
            while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
              if (localFileHeader.isDirectory()) {
                continue;
              }

              ArrayList<Byte> fileAttachementBytes = new ArrayList<>();
              int readLen;
              while ((readLen = zipInputStream.read()) != -1) {
                fileAttachementBytes.add((byte) readLen);
              }
              byte[] fileAttachementBytesArray = new byte[fileAttachementBytes.size()];
              for (int k = 0; k < fileAttachementBytes.size(); k++) {
                fileAttachementBytesArray[k] = fileAttachementBytes.get(k);
              }

              xmlString = new String(fileAttachementBytesArray, StandardCharsets.UTF_8);
              xmlFileName = localFileHeader.getName();

            }
          } catch (IOException e) {
            throw new DmarcIllegalStructure("Unable to read the zip data attachment", e);
          }
          if (xmlString == null) {
            throw new DmarcIllegalStructure("The content is null");
          }
          try {
            return DmarcReport.create(xmlFileName, xmlString);
          } catch (XMLStreamException | IOException e) {
            throw new DmarcIllegalStructure("Unable to create the Dmarc Report", e);
          }
        case GZIP_CONTENT_TYPE:
          try {
            xmlFileName = mailPart.getFileName();
          } catch (MessagingException e) {
            throw new DmarcIllegalStructure("Unable to read the gzip file name", e);
          }
          try {

            try {
              inputStream = mailPart.getInputStream();
            } catch (IOException | MessagingException e) {
              throw new DmarcIllegalStructure(e);
            }
            xmlString = Gzip.decompress(inputStream.readAllBytes());
          } catch (IOException e) {
            throw new DmarcIllegalStructure("Unable to read the gzip data attachment", e);
          }
          try {
            return DmarcReport.create(xmlFileName, xmlString);
          } catch (XMLStreamException | IOException e) {
            throw new DmarcIllegalStructure("Unable to create the Dmarc Report", e);
          }
        default:
          throw new DmarcIllegalStructure("Attachement Content Type (" + contentType + ") not supported");
      }

    }
    throw new DmarcIllegalStructure("No Attachements found");
  }
}
