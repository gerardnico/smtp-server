package com.combostrap.type;

import java.net.MalformedURLException;
import java.net.URL;

public class Urls {


  /**
   * A utility when we know the string is a valid URL
   * (ie literal or stored in a database)
   */
  public static URL toUrlFailSafe(String url) {
      try {
          return new URL(url);
      } catch (MalformedURLException e) {
          throw new RuntimeException(e);
      }
  }
}
