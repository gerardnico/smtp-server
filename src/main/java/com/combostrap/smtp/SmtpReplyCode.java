package com.combostrap.smtp;

/**
 * Reply code
 * Same as HTTP status code the class
 * *  * 4xx are error from the client
 * *  * 5xx are error from the server
 * The list is here:
 * <a href="https://www.rfc-editor.org/rfc/rfc5321.html#section-4.2.2">...</a>
 * <p>
 * See also the {@link SmtpExtensionParameter#ENHANCEDSTATUSCODES enhancecd status code} X.X.X
 * with <a href="https://datatracker.ietf.org/doc/html/rfc1893#section-8">List here</a>
 */
public enum SmtpReplyCode {

  /**
   * Service not available, closing transmission channel,
   * too many connection
   */
  SERVICE_NOT_AVAILABLE_421(421, "Service not available"),
  /**
   * <a href="https://datatracker.ietf.org/doc/html/rfc2821#section-4.2.4">502</a>
   */
  NOT_IMPLEMENTED_502(502, "Command not implemented"),
  NOT_RECOGNIZED_500(500, "Command not recognized"),
  SYNTAX_ERROR_501(501, "Syntax error in command or arguments"),
  /**
   * Transaction failed (Or, in the case of a connection-opening response, "No SMTP service here")
   * Internal error
   */
  TRANSACTION_FAILED_554(554, "Service shutting down and closing transmission channel"),
  BAD_SEQUENCE_OF_COMMAND_503(503, "Bad sequence of commands"),
  NO_VALID_RECIPIENTS_504(504, "No valid recipients"),
  GREETING_220(220, "Greeting"),
  CODE_211(211, ""),
  CODE_214(214, ""),
  OK_250(250, "Ok"),
  NO_SUCH_USER_550(550, ""),
  MESSAGE_SIZE_EXCEED_LIMIT_552(552, "5.3.4 Message size exceeds fixed limit"),
  ERROR_IN_PROCESSING_451(451, "Requested action aborted: error in processing"),
  INSUFFISANT_STORAGE_452(452, "Requested action not taken: insufficient system storage"),
  USER_AMBIGUOUS_553(553, "User ambiguous"),
  CODE_251(251, ""),
  USER_NOT_LOCAL_551(551, "User not local; please try <forward-path>"),
  CODE_450(450, ""),
  START_MAIL_INPUT_354(354, "Start mail input; end with <CRLF>.<CRLF>"),
  CODE_252(252, ""),
  CLOSING_QUITING_221(221, ""),

  TLS_NOT_AVAILABLE_TEMPORARILY_454(421, "TLS not available due to temporary reason"),
  AUTHENTICATION_REQUIRED_530(530, "5.7.0  Authentication required. Must issue a AUTH command first"),
  READY_FOR_CREDENTIALS_334(334, "Enter credentials"),
  SUCCESSFUL_AUTHENTICATION_235(235, "2.7.0 Authentication successful"),
  CREDENTIAL_INVALID_535(525, "5.7.8  Authentication credentials invalid"),
  SSL_REQUIRED_538(538, "5.7.11  Encryption required for requested authentication mechanism: Must issue a STARTTLS command first"),
  /**
   * Code used only internally for the {@link SmtpSessionInteractionHistory}
   */
  INTERNAL_CLIENT_QUIT_999(999, "Client has disconnected"),
  /**
   * 8xx reply code are bad behaviors
   */
  CONNECTION_WITH_BAD_HOSTNAME_899(899, "Bad Hostname in connection"),
  FIRST_PARTY_NOT_AUTHENTICATED_898(898, "Try to send a first party email not authenticated");

  private final int statusCode;
  private final String defaultHumanDescription;

  SmtpReplyCode(int i, String defaultHumanDescription) {
    this.statusCode = i;
    this.defaultHumanDescription = defaultHumanDescription;
  }

  public int getCode() {
    return statusCode;
  }

  public String getHumanText() {
    return defaultHumanDescription;
  }
}
