package context_search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import configuration.Configuration;
import utils.ArrayUtils;

public class SuffixTreeNode {
  private Configuration configuration;
  private Map<Character, SuffixTreeNode> children;
  private Set<Integer> indexes;
  private int depth;

  public SuffixTreeNode(Configuration configuration, int depth) {
    this.configuration = configuration;
    children = new HashMap<Character, SuffixTreeNode>();
    indexes = new HashSet<Integer>();
    this.depth = depth;
  }

  public void addSuffix(String suffix, int node) {
    if (suffix.length() == 0) {
      indexes.add(node);
      return;
    }

    SuffixTreeNode next = children.get(suffix.charAt(0));
    if (next == null) {
      next = new SuffixTreeNode(configuration, depth + 1);
      children.put(suffix.charAt(0), next);
    }

    next.addSuffix(suffix.substring(1), node);
  }

  public int count() {
    int count = 1;
    for (SuffixTreeNode child : children.values()) {
      count += child.count();
    }
    return count;
  }

  public Set<Integer> getIndexes() {
    Set<Integer> cumulativeIndexes = new HashSet<Integer>();

    for (SuffixTreeNode child : children.values()) {
      cumulativeIndexes.addAll(child.getIndexes());
    }

    cumulativeIndexes.addAll(indexes);

    return cumulativeIndexes;
  }

  public int improvedSearch(char[] suffix, int[] scores, int maxScore, int depth,
      HashMap<Integer, Integer> finalScores, boolean[] gaps, String path, int maxDepth) {
    int current = ArrayUtils.max(scores);
    if (current + (maxDepth - depth) * configuration.getMaxPairwiseScore()
        < maxScore - configuration.getSuffixScoreThreshold()) {
      return maxScore;
    }
    if (children.size() == 0) {
      int score = ArrayUtils.max(scores);
      for (Integer i : indexes) {
        if ((!finalScores.containsKey(i) || score > finalScores.get(i))
            && score >= maxScore - configuration.getSuffixScoreThreshold()) {
          finalScores.put(i, score);
        }
      }
      return Math.max(maxScore, score);
    }
    for (Character c : children.keySet()) {
      int[] myScores = new int[scores.length];
      boolean[] myGaps = new boolean[gaps.length];
      if (depth == 0) {
        myScores[0] = scores[0] - configuration.getGapOpeningPenalty();
      } else {
        myScores[0] = scores[0] - configuration.getGapExtensionPenalty();
      }
      for (int i = 1; i < scores.length; i++) {
        int verticalScore = myScores[i - 1] - getGapPenalty(myScores, i);
        int horizontalScore = scores[i] - configuration.getGapOpeningPenalty();
        if (gaps[i]) {
          horizontalScore = scores[i] - configuration.getGapExtensionPenalty();
        }
        int diagonalScore = scores[i - 1] + configuration.getScore(suffix[i - 1], c);
        myScores[i] = ArrayUtils
            .max(new int[] { verticalScore, horizontalScore, diagonalScore });
        if (myScores[i] == horizontalScore) {
          myGaps[i] = true;
        }
      }

      maxScore = Math
          .max(children.get(c).improvedSearch(suffix, myScores, maxScore, depth + 1, finalScores,
              myGaps, path + c, maxDepth), maxScore);
    }
    return maxScore;
  }

  private int getGapPenalty(int[] scores, int index) {
    if (index - 1 == 0) {
      return configuration.getGapExtensionPenalty();
    } else if ((scores[index - 1] == scores[index - 2] - configuration.getGapOpeningPenalty())
        || (scores[index - 1]
        == scores[index - 2] - configuration.getGapExtensionPenalty())) {
      return configuration.getGapExtensionPenalty();
    } else {
      return configuration.getGapOpeningPenalty();
    }
  }

  public String prettyPrint(int spaces) {
    if (children.size() == 0) {
      return ": " + indexes + "\n";
    }
    String s = "";
    for (Character c : children.keySet()) {
      s += new String(new char[spaces]).replace("\0", " ");
      s += c + "\n";
      s += children.get(c).prettyPrint(spaces + 1);
    }
    return s;
  }

  public Set<Integer> strictSearch(String s) {
    if (s.length() == 0 || children.size() == 0) {
      return getIndexes();
    } else if (children.containsKey(s.charAt(0))) {
      return children.get(s.charAt(0)).strictSearch(s.substring(1));
    } else {
      return null;
    }
  }
}
