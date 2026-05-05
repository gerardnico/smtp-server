package com.combostrap.smtp.milter;

import com.combostrap.dmarc.*;
import com.combostrap.email.BMailMimeMessage;
import com.combostrap.smtp.SmtpMessage;
import com.combostrap.type.MediaType;
import com.combostrap.type.Timestamp;
import io.vertx.core.json.JsonObject;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;


public class DmarcMilter implements SmtpMilter {


  /**
   * The {@link LocalDateTime#toString()} print a short iso string
   * where the seconds and minute may be missing.
   * To have a consistent number of string, we use a fix pattern
   */
  private static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
  private static final String ERROR_DIRECTORY = "error/";



  public SmtpMessage apply(SmtpMessage smtpMessage) {

    Object object = smtpMessage.getObject();
    try {
      DmarcReport dmarcReport;

      if (object.getClass().equals(BMailMimeMessage.class)) {
        dmarcReport = DmarcManager.getDmarcReportFromMime((BMailMimeMessage) object);
      } else if (object.getClass().equals(JsonObject.class)) {
        dmarcReport = DmarcManager.getDmarcReportFromJsonEmail((JsonObject) object);
      } else {
        return smtpMessage;
      }

      String remoteUniquePath = getRemoteUniquePath(dmarcReport.getFeedbackObject());
      return new SmtpMessage() {

        @Override
        public Object getObject() {
          return dmarcReport.getFeedbackXml();
        }

        @Override
        public byte[] getBytes() {
          return dmarcReport.getFeedbackXml().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String getPath() {
          return remoteUniquePath;
        }

        @Override
        public MediaType getMediaType() {
          return MediaType.of("text","xml");
        }

      };
    } catch (DmarcIllegalStructure e) {
      return getErrorObject(e, object.toString());
    }


  }

  private static SmtpMessage getErrorObject(DmarcIllegalStructure e, String data) {
    JsonObject errorJsonWithDataObject = new JsonObject();
    errorJsonWithDataObject.put("data", data);
    JsonObject errorJson = new JsonObject();
    errorJson.put("message", e.getMessage());
    errorJson.put("code", e.getClass().getName());
    errorJsonWithDataObject.put("error", errorJson);
    Timestamp now = Timestamp.createFromNowLocalSystem();
    String errorName = now.toString(ISO_DATE_FORMAT);
    String partition = now.toString("yyyy-MM");
    return new SmtpMessage() {

      @Override
      public Object getObject() {
        return errorJsonWithDataObject.encode();
      }

      @Override
      public byte[] getBytes() {
        return errorJsonWithDataObject.encode().getBytes(StandardCharsets.UTF_8);
      }

      @Override
      public String getPath() {
        return ERROR_DIRECTORY + partition + "/" + errorName + "-error.json";
      }

      @Override
      public MediaType getMediaType() {
        return MediaType.of("text","json");
      }

    };

  }


  /**
   * @return a file path with a name that is unique and a parent path to create a partition on the remote bucket
   */
  public static String getRemoteUniquePath(DmarcFeedback dmarcFeedback) {


    DmarcFeedbackMetadata reportMetadata = dmarcFeedback.getReportMetadata();
    Timestamp beginTimestamp = Timestamp.createFromEpochSec(reportMetadata.getReportDateRange().getBegin());
    String monthPartition = beginTimestamp.toString("YYYY-MM");
    String yearPartition = beginTimestamp.toString("YYYY");
    String beginIsoString = beginTimestamp.toString(ISO_DATE_FORMAT);
    String endIsoString = Timestamp.createFromEpochSec(reportMetadata.getReportDateRange().getEnd()).toString(ISO_DATE_FORMAT);
    return yearPartition + "/" + monthPartition + "/"
      + beginIsoString + "!" +
      endIsoString + "!" +
      reportMetadata.getOrganisationName().trim() + "!" +
      dmarcFeedback.getPolicyPublished().getDomain().trim() +
      ".xml";

  }


}
