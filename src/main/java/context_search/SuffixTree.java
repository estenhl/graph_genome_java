package context_search;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import configuration.Configuration;

public class SuffixTree implements Serializable, Runnable {

  public static final int GAP_STATUS_NO_GAP = 0;
  public static final int GAP_STATUS_GAP_IN_SEQUENCE = 1;
  public static final int GAP_STATUS_GAP_IN_GRAPH = 2;

  private Configuration configuration;
  private SuffixTreeNode head;
  private int maxDepth;
  private String s;
  private boolean force;
  private int index;
  private Map<Integer, HashMap<Integer, Integer>> scores;
  private boolean ready = false;

  public SuffixTree(Configuration configuration) {
    this.configuration = configuration;
    head = new SuffixTreeNode(configuration, 0);
    maxDepth = configuration.getContextLength();
    scores = new HashMap<Integer, HashMap<Integer, Integer>>();
  }

  /**
   * Method used in parallelization
   */
  public synchronized void setSearchParams(String s, boolean force, int index) {
    this.s = s;
    this.force = force;
    this.index = index;
    setReady(false);
  }

  /**
   * Method used in parallelization
   */
  public synchronized boolean getReady() {
    return ready;
  }

  /**
   * Method used in parallelization
   */
  public synchronized void await() {
    while (!ready) {
      try {
        wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return;
  }

  /**
   * Method used in parallelization
   */
  public void run() {
    improvedSearch(s, force, index);
  }

  /**
   * Method used in parallelization
   */
  public synchronized void setReady(boolean ready) {
    this.ready = ready;
  }

  /**
   * Method used in parallelization
   */
  public HashMap<Integer, Integer> getScores(int index) {
    return scores.get(index);
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

  /**
   * The recursive suffix tree search
   */
  public synchronized HashMap<Integer, Integer> improvedSearch(String s, boolean force, int index) {
    setReady(true);
    notifyAll();
    if ((!force && s.length() < configuration.getContextLength()) || s.length() == 0) {
      this.scores.put(index, new HashMap<Integer, Integer>());
      return new HashMap<Integer, Integer>();
    }

    // Initializes the base cases of an empty string
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

    this.scores.put(index, finalScores);
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
