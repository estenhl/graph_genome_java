package utils;

import data.Graph;
import data.Node;

public class GraphUtils {

  public static final double SHARED_SUFFIX_PROBABILITY = 0.01;

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

  public static int optimalSuffixLength(Graph graph) {
    int nodes = graph.getCurrentSize();
    double branchingFactor = graph.getApproxBranchingFactor();
    int suffixes = (int) (nodes * branchingFactor);
    int i = 1;
    while (approximateProbability(suffixes, i) > SHARED_SUFFIX_PROBABILITY) {
      i++;
    }

    return i;
  }

  private static double approximateProbability(int x, int y) {
    return 1.0 - Math.pow(Math.E, -Math.pow(x, 2) / (2.0 * Math.pow(4, y)));
  }
}
