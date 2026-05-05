package com.combostrap.dmarc;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DmarcFeedbackMetadata {

  private String organisationName;
  private String email;
  private String extraContactInfo;

  private String reportId;

  private DmarcFeedbackDateRange dmarcFeedbackDateRange;

  @JsonProperty("org_name")
  public String getOrganisationName() {
    return organisationName;
  }

  @SuppressWarnings("unused")
  public void setOrganisationName(String organisationName) {
    this.organisationName = organisationName;
  }

  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  @SuppressWarnings("unused")
  public void setEmail(String email) {
    this.email = email;
  }

  @JsonProperty("extra_contact_info")
  public String getExtraContactInfo() {
    return extraContactInfo;
  }

  @SuppressWarnings("unused")
  public void setExtraContactInfo(String extraContactInfo) {
    this.extraContactInfo = extraContactInfo;
  }

  @JsonProperty("report_id")
  public String getReportId() {
    return reportId;
  }

  @SuppressWarnings("unused")
  public void setReportId(String reportId) {
    this.reportId = reportId;
  }

  @JsonProperty("date_range")
  public DmarcFeedbackDateRange getReportDateRange() {
    return dmarcFeedbackDateRange;
  }

  @SuppressWarnings("unused")
  public void setReportDateRange(DmarcFeedbackDateRange dmarcFeedbackDateRange) {
    this.dmarcFeedbackDateRange = dmarcFeedbackDateRange;
  }
}
