package com.combostrap.smtp;


import com.combostrap.type.MediaType;

/**
 * Represents a message
 * After receiving a mime message
 * we may transform it via milters before delivering it
 * to the mailbox.
 */
public interface SmtpMessage {

  /**
   * @return the object
   */
  Object getObject();

  /**
   * @return the bytes to store
   */
  byte[] getBytes();

  /**
   * @return the path on the file system
   */
  String getPath();


  /**
   * @return the media type
   */
  MediaType getMediaType();


}
