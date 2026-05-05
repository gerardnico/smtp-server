package com.combostrap.common;



import java.io.Console;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;

@SuppressWarnings("unused")
public class JavaEnvs {


  private static final boolean IS_IDE_DEBUGGING;
  private static Boolean IS_DEV;
  private static Boolean IS_TEST;

  static {

    /*
     * If the mode is debugging, this value is true
     * (Works in eclipse, idea)
     * It's used mostly to disable the timeout that will otherwise
     * kick you out of a debug session.
     * https://stackoverflow.com/questions/1109019/determine-if-a-java-application-is-in-debug-mode-in-eclipse
     */
    String inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments().toString();
    IS_IDE_DEBUGGING = inputArguments.contains("-agentlib:jdwp");


  }

  /**
   * Check if `System.out` is connected to a terminal
   */
  public static boolean isRunningInTerminal() {

    // Pure Terminal
    Console console = System.console();
    if(console != null) {
      return true;
    }

    // Terminal in IDE for instance
    // TERM is typically set
    String term = System.getenv("TERM");
    return term != null;

    // A shell can be run in background
    // Discovering the SHELL env does not say anything about running in a terminal
    // return System.getenv("SHELL") !=null;

  }

  public static boolean isDev() {
    return isDev("", "");
  }

  /**
   * @return true if a dev environment is found via the properties or env name
   * or if a `.git` directory is found in the ancestors of the current directory
   * Example of value:
   * * "web.environment", "WEB_ENVIRONMENT"
   * * "vertxweb.environment", "VERTXWEB_ENVIRONMENT"
   * ie <a href="https://github.com/vert-x3/vertx-web/blob/master/vertx-web-common/src/main/java/io/vertx/ext/web/common/WebEnvironment.java">...</a>
   */
  public static boolean isDev(String javaSysProp, String osEnvName) {

    if (IS_DEV != null) {
      return IS_DEV;
    }

    /*
     *  On an idea of
     *  https://github.com/vert-x3/vertx-web/blob/master/vertx-web-common/src/main/java/io/vertx/ext/web/common/WebEnvironment.java
     */
    String env = System.getProperty(javaSysProp, System.getenv(osEnvName));
    if ("dev".equalsIgnoreCase(env) || "Development".equalsIgnoreCase(env)) {
      IS_DEV = true;
      return true;
    }

    /**
     * Search for a file that would indicate that
     * it's a dev environment. Git is the best.
     */
    try {
      Fs.closest(Paths.get("."), ".git");
      IS_DEV = true;
      return true;
    } catch (FileNotFoundException e) {
      // ok
    }

    IS_DEV = false;
    return false;

  }

  public static boolean isJUnitTest() {
    if (IS_TEST != null) {
      return IS_TEST;
    }
    for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
      if (element.getClassName().startsWith("org.junit.")) {
        IS_TEST = true;
        return IS_TEST;
      }
    }
    IS_TEST = false;
    return IS_TEST;
  }

  public static boolean isIsIdeDebugging() {

    return IS_IDE_DEBUGGING;

  }


}
