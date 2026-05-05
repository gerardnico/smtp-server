package com.combostrap.dmarc;

/**
 * Raw reports are in XML format, and include report metadata and one or more records.
 * The important information in the reports is whether messages from your domain pass DMARC.
 * <p>
 * Each record summarizes:
 * <p>
 * The number of messages sent from a single IP address for the report time period
 * The SPF, DKIM, and DMARC authentication results for the messages
 * Any actions taken by the receiving server, for example accepting unauthenticated messages because they passed ARC authentication
 * <p>
 * <a href="https://datatracker.ietf.org/doc/html/rfc7489#appendix-C">Xml Report Format</a>
 */
public class DmarcFeedback {


  private DmarcFeedbackMetadata reportMetadata;
  private DmarcFeedbackPolicyPublished policyPublished;

  public DmarcFeedback() {
  }


  public DmarcFeedbackMetadata getReportMetadata() {
    return reportMetadata;
  }

  public void setReportMetadata(DmarcFeedbackMetadata reportMetadata) {
    this.reportMetadata = reportMetadata;
  }

  public void setReportPolicyPublished(DmarcFeedbackPolicyPublished dmarcFeedbackPolicyPublished) {
    this.policyPublished = dmarcFeedbackPolicyPublished;
  }

  public DmarcFeedbackPolicyPublished getPolicyPublished() {
    return this.policyPublished;
  }
}
