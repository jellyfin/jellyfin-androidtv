package android.util;

/**
 * Workaround slf4j trying to invoke Android.Log.isLoggable
 * by providing a shim which simply uses println on everything.
 */
public class Log {
  public static boolean isLoggable(String tag, int level) { return true; }

  public static int v(String tag, String msg) {
    // Skip printing verbose in unit tests by default
    // System.out.println("VERBOSE: " + tag + ": " + msg);
    return 0;
  }

  public static int d(String tag, String msg) {
    System.out.println("DEBUG: " + tag + ": " + msg);
    return 0;
  }

  public static int i(String tag, String msg) {
    System.out.println("INFO: " + tag + ": " + msg);
    return 0;
  }

  public static int w(String tag, String msg) {
    System.out.println("WARN: " + tag + ": " + msg);
    return 0;
  }

  public static int e(String tag, String msg) {
    System.out.println("ERROR: " + tag + ": " + msg);
    return 0;
  }
}
