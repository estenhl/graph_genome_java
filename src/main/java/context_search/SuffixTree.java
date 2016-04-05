package context_search;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

import configuration.Configuration;

public class SuffixTree implements Serializable {

  public static final int GAP_STATUS_NO_GAP = 0;
  public static final int GAP_STATUS_GAP_IN_SEQUENCE = 1;
  public static final int GAP_STATUS_GAP_IN_GRAPH = 2;

  private Configuration configuration;
  private SuffixTreeNode head;
  private int maxDepth;

  public SuffixTree(Configuration configuration) {
    this.configuration = configuration;
    head = new SuffixTreeNode(configuration, 0);
    maxDepth = configuration.getContextLength();
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public void addSuffix(String suffix, int node) {
    if (suffix.length() > maxDepth) {
      suffix = suffix.substring(0, maxDepth);
    }
    head.addSuffix(suffix, node);
  }

  public HashMap<Integer, Integer> improvedSearch(String s, boolean force) {
    if ((!force && s.length() < configuration.getContextLength()) || s.length() < 2) {
      return new HashMap<Integer, Integer>();
    }
    int[] scores = new int[s.length() + 1];
    scores[0] = 0;
    scores[1] = scores[0] - configuration.getGapOpeningPenalty();
    for (int i = 2; i < scores.length; i++) {
      scores[i] = scores[i - 1] - configuration.getGapExtensionPenalty();
    }
    HashMap<Integer, Integer> finalScores = new HashMap<Integer, Integer>();
    int maxScore = configuration.getMaxAlignmentScore(s) - configuration.getErrorMargin();
    int depth = 0;
    head.improvedSearch(s.toCharArray(), scores, maxScore, depth, finalScores,
        new boolean[scores.length], maxDepth);

    return finalScores;
  }

  public Set<Integer> strictSearch(String s) {
    return head.strictSearch(s);
  }

  public int getNumberOfNodes() {
    return head.count();
  }

  public void printSuffix(int index) {
    head.printSuffix("", index);
  }

  @Override
  public String toString() {
    return "SuffixTree:\n" + head.prettyPrint(0);
  }
}
