package com.combostrap.vertx;

import io.vertx.ext.healthchecks.CheckResult;
import io.vertx.ext.healthchecks.Status;

import java.util.HashMap;
import java.util.Map;

/**
 * Takes a check result and handle it
 * to create a summary
 */
public class ServerHealthReport {

  private boolean passed = true;
  private Throwable failure;

  final Map<String, String> results = new HashMap<>();


  public ServerHealthReport(CheckResult checkResult) {

    /**
     * Note that CheckResult is a tree of report
     * And the top Status is always null.
     */
    failure = checkResult.getFailure();
    for (CheckResult subCheck : checkResult.getChecks()) {
      Status subCheckStatus = subCheck.getStatus();
      Throwable subCheckFailure = subCheck.getFailure();
      if (subCheckStatus == null || subCheckFailure != null) {
        passed = false;
        failure = subCheckFailure;
        this.results.put(subCheck.getId(), "Failure -> " + subCheckFailure.getMessage());
      } else {
        boolean ok = subCheckStatus.isOk();
        if (!ok) {
          passed = false;
        }
        this.results.put(subCheck.getId(), ok ? "Ok" : "Ko");
      }
    }

  }


  /**
   *
   * @return a list of checks suited for the console (ie printed with a carriage return)
   */
  String getReportForConsole() {
    StringBuilder message = new StringBuilder();
    for (Map.Entry<String, String> entry : this.results.entrySet()) {
      message
        .append(" - ")
        .append(entry.getKey())
        .append(": ")
        .append(entry.getValue())
        .append('\n')
      ;
    }
    return message.toString();

  }

  public boolean isOk() {
    return passed;
  }

  public Throwable getFailure() {
    return this.failure;
  }
}
