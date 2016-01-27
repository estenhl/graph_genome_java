package utils;

/**
 * Created by esten on 12.01.16.
 */
public class StringUtils {
  public static String reverse(String s) {
    int length = s.length();
    char[] original = s.toCharArray();
    char[] reversed = new char[length];
    for (int i = 0; i < length; i++) {
      reversed[i] = original[length - 1 - i];
    }
    return new String(reversed);
  }
}
