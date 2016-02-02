package utils;

public class ArrayUtils {
  public static int max(int[] arr) {
    int max = Integer.MIN_VALUE;
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] > max) {
        max = arr[i];
      }
    }

    return max;
  }

  public static double max(double[] arr) {
    double max = Double.MIN_VALUE;
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] > max) {
        max = arr[i];
      }
    }

    return max;
  }

  public static int findHighestIndex(double[] arr) {
    if (arr.length <= 0) {
      return -1;
    }
    double max = arr[0];
    int index = 0;
    for (int i = 1; i < arr.length; i++) {
      if (arr[i] > max) {
        max = arr[i];
        index = i;
      }
    }

    return index;
  }
}
