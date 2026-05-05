package com.combostrap.smtp;

/**
 *
 */
public interface SmtpInputHandler {



  void handle(SmtpInputContext smtpInputContext) throws SmtpException;


}
