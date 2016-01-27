package context_search;

import java.util.HashMap;
import java.util.Set;

import utils.AlignmentUtils;

public class SuffixTree {
  private SuffixTreeNode head;
  private int maxDepth;
  public static int operations;

  public SuffixTree() {
    head = new SuffixTreeNode(0);
    maxDepth = AlignmentUtils.SUFFIX_LENGTH;
    operations = 0;
  }

  public void addSuffix(String suffix, int node) {
    if (suffix.length() > maxDepth) {
      suffix = suffix.substring(0, maxDepth);
    }
    head.addSuffix(suffix, node);
  }

  public HashMap<Integer, Set<Integer>> search(String s) {
    System.out.println("Searching for string " + s);
    HashMap<Integer, Set<Integer>> scores = new HashMap<Integer, Set<Integer>>();
    operations = 0;
    head.search(s, 0, scores, 0, Math.min(maxDepth, s.length()), AlignmentUtils.GAP_STATUS_NO_GAP);
    System.out.println("Number of operations: " + operations);
    return scores;
  }

  public HashMap<Integer, Integer> improvedSearch(String s) {
    if (s.length() < 2) {
      return new HashMap<Integer, Integer>();
    }
    int[] scores = new int[s.length() + 1];
    scores[0] = 0;
    scores[1] = scores[0] - AlignmentUtils.AFFINE_GAP_PENALTY_OPENING;
    for (int i = 2; i < scores.length; i++) {
      scores[i] = scores[i - 1] - AlignmentUtils.AFFINE_GAP_PENALTY_CONTINUATION;
    }
    HashMap<Integer, Integer> finalScores = new HashMap<Integer, Integer>();
    head.improvedSearch(s.toCharArray(), scores,
        Integer.MIN_VALUE + AlignmentUtils.SUFFIX_SCORE_THRESHOLD * 10, 0, finalScores,
        new boolean[scores.length], "", maxDepth);

    return finalScores;
  }

  public Set<Integer> strictSearch(String s) {
    return head.strictSearch(s);
  }

  public int getNumberOfNodes() {
    return head.count();
  }

  @Override
  public String toString() {
    return "SuffixTree:\n" + head.prettyPrint(0);
  }
}
