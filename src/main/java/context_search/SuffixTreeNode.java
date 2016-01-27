package context_search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import utils.AlignmentUtils;
import utils.ArrayUtils;

public class SuffixTreeNode {
  public Map<Character, SuffixTreeNode> children;
  public Set<Integer> indexes;
  public int depth;

  public SuffixTreeNode(int depth) {
    children = new HashMap<Character, SuffixTreeNode>();
    indexes = new HashSet<Integer>();
  }

  public void addSuffix(String suffix, int node) {
    if (suffix.length() == 0) {
      indexes.add(node);
      return;
    }

    SuffixTreeNode next = children.get(suffix.charAt(0));
    if (next == null) {
      next = new SuffixTreeNode(depth + 1);
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

  public int search(String suffix, int score, HashMap<Integer, Set<Integer>> scores,
      int maxScore, int maxDepth, int gapStatus) {
    SuffixTree.operations += 1;
    if (score + (maxDepth - depth) * AlignmentUtils.MAX_PAIRWISE_ALIGNMENT_SCORE < maxScore) {
      return maxScore;
    }
    if (suffix.length() == 0) {
      if (score >= maxScore - AlignmentUtils.SUFFIX_SCORE_THRESHOLD) {
        if (scores.containsKey(score)) {
          scores.get(score).addAll(getIndexes());
        } else {
          scores.put(score, getIndexes());
        }
      }
    } else if (children.size() == 0) {
      if (score >= maxScore - AlignmentUtils.SUFFIX_SCORE_THRESHOLD) {
        if (scores.containsKey(score)) {
          scores.get(score).addAll(getIndexes());
        } else {
          scores.put(score, getIndexes());
        }
      }
    } else {
      // Gap in sequence
      if (gapStatus == AlignmentUtils.GAP_STATUS_GAP_IN_SEQUENCE) {
        maxScore = Math.max(maxScore,
            search(suffix.substring(1), score - AlignmentUtils.AFFINE_GAP_PENALTY_CONTINUATION,
                scores,
                maxScore, maxDepth, AlignmentUtils.GAP_STATUS_GAP_IN_SEQUENCE));
      } else {
        maxScore = Math.max(maxScore,
            search(suffix.substring(1), score - AlignmentUtils.AFFINE_GAP_PENALTY_OPENING, scores,
                maxScore, maxDepth, AlignmentUtils.GAP_STATUS_GAP_IN_SEQUENCE));
      }

      for (Character c : children.keySet()) {
        // Gap in graph
        if (gapStatus == AlignmentUtils.GAP_STATUS_GAP_IN_GRAPH) {
          maxScore = Math.max(maxScore, children.get(c)
              .search(suffix, score - AlignmentUtils.AFFINE_GAP_PENALTY_CONTINUATION, scores,
                  maxScore,
                  maxDepth, AlignmentUtils.GAP_STATUS_GAP_IN_GRAPH));
        } else {
          maxScore = Math.max(maxScore, children.get(c)
              .search(suffix, score - AlignmentUtils.AFFINE_GAP_PENALTY_OPENING, scores, maxScore,
                  maxDepth, AlignmentUtils.GAP_STATUS_GAP_IN_GRAPH));
        }

        // Match and SNP
        maxScore = Math.max(maxScore, children.get(c)
            .search(suffix.substring(1), score + AlignmentUtils.getScore(c, suffix.charAt(0)),
                scores, maxScore, maxDepth, AlignmentUtils.GAP_STATUS_NO_GAP));
      }
    }

    return Math.max(score, maxScore);
  }

  public int improvedSearch(char[] suffix, int[] scores, int maxScore, int depth,
      HashMap<Integer, Integer> finalScores, boolean[] gaps, String path, int maxDepth) {
    int current = ArrayUtils.max(scores);
    if (current + (maxDepth - depth) * AlignmentUtils.MAX_PAIRWISE_ALIGNMENT_SCORE
        < maxScore - AlignmentUtils.SUFFIX_SCORE_THRESHOLD) {
      return maxScore;
    }
    if (children.size() == 0) {
      int score = ArrayUtils.max(scores);
      for (Integer i : indexes) {
        if ((!finalScores.containsKey(i) || score > finalScores.get(i))
            && score >= maxScore - AlignmentUtils.SUFFIX_SCORE_THRESHOLD) {
          finalScores.put(i, score);
        }
      }
      return Math.max(maxScore, score);
    }
    for (Character c : children.keySet()) {
      int[] myScores = new int[scores.length];
      boolean[] myGaps = new boolean[gaps.length];
      if (depth == 0) {
        myScores[0] = scores[0] - AlignmentUtils.AFFINE_GAP_PENALTY_OPENING;
      } else {
        myScores[0] = scores[0] - AlignmentUtils.AFFINE_GAP_PENALTY_CONTINUATION;
      }
      for (int i = 1; i < scores.length; i++) {
        int verticalScore = myScores[i - 1] - getGapPenalty(myScores, i);
        int horizontalScore = scores[i] - AlignmentUtils.AFFINE_GAP_PENALTY_OPENING;
        if (gaps[i]) {
          horizontalScore = scores[i] - AlignmentUtils.AFFINE_GAP_PENALTY_CONTINUATION;
        }
        int diagonalScore = scores[i - 1] + AlignmentUtils.getScore(suffix[i - 1], c);
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
      return AlignmentUtils.AFFINE_GAP_PENALTY_CONTINUATION;
    } else if ((scores[index - 1] == scores[index - 2] - AlignmentUtils.AFFINE_GAP_PENALTY_OPENING)
        || (scores[index - 1]
        == scores[index - 2] - AlignmentUtils.AFFINE_GAP_PENALTY_CONTINUATION)) {
      return AlignmentUtils.AFFINE_GAP_PENALTY_CONTINUATION;
    } else {
      return AlignmentUtils.AFFINE_GAP_PENALTY_OPENING;
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
