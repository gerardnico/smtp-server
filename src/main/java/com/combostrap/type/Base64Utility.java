package com.combostrap.type;

import java.nio.charset.StandardCharsets;

/**
 * Just wrapper
 */
public class Base64Utility {

  public static String bytesToBase64String(byte[] binaryData) {
    return java.util.Base64.getEncoder().encodeToString(binaryData);
  }

  public static String stringToBase64UrlString(String string) {
    return java.util.Base64.getUrlEncoder().encodeToString(string.getBytes(StandardCharsets.UTF_8));
  }

  @SuppressWarnings("unused")
  public static String bytesToBase64UrlBytes(byte[] binaryData) {
    return java.util.Base64.getUrlEncoder().encodeToString(binaryData);
  }

  public static byte[] base64StringToByte(String base64String) {
    return java.util.Base64.getDecoder().decode(base64String);
  }

  public static String base64UrlStringToString(String string) throws Base64CastException {
    byte[] bytes = base64UrlStringToByte(string);
    return Strings.createFromByte(bytes, StandardCharsets.UTF_8).toString();

  }

  public static byte[] base64UrlStringToByte(String base64String) throws Base64CastException {
    try {
      return java.util.Base64.getUrlDecoder().decode(base64String);
    } catch (Exception e) {
      throw new Base64CastException("The string is not a base 64 string",e);
    }
  }

}
