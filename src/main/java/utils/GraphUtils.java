package utils;

import data.Node;

public class GraphUtils {
  public static int getGraphSize(int num) {
    int i = 1;
    while (true) {
      double val = Math.pow(2, i);
      if (val > num) {
        return (int) val;
      } else {
        i++;
      }
    }
  }

  public static Node[] doubleNodeArray(Node[] oldArr) {
    Node[] newArr = new Node[oldArr.length * 2];
    for (int i = 0; i < oldArr.length - 1; i++) {
      newArr[i] = oldArr[i];
    }
    newArr[newArr.length - 1] = oldArr[oldArr.length - 1];

    return newArr;
  }
}
