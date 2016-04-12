package utils;

/**
 * Created by esten on 11.04.16.
 */
public class LogUtils {
  public static final int INFO = 0;
  public static final int WARNING = 1;
  public static final int ERROR = 2;

  public static void print(int level, String message) {
    if (level == INFO) {
      printInfo(message);
    } else if (level == WARNING) {
      printWarning(message);
    } else if (level == ERROR) {
      printWarning(message);
    } else {
      printError("Unknown log level " + level);
    }
  }

  public static void printInfo(String message) {
    System.out.printf("%-10s%s\n", "[INFO]", message);
  }

  public static void printWarning(String message) {
    System.out.printf("%-10s%s\n", "[WARNING]", message);
  }

  public static void printError(String message) {
    System.out.printf("%-10s%s\n", "[ERROR]", message);
  }
}
