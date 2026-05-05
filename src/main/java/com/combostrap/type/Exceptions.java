package com.combostrap.type;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Exceptions {


  public static String getStackTraceAsString(Throwable thrown) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    thrown.printStackTrace(pw);
    return sw.toString();
  }

}
