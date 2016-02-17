package configuration;

import data.Graph;

public abstract class Configuration {
  public static final int DEFAULT_SUFFIX_LENGTH = 15;
  public static final double DEFAULT_CONTEXT_SEARCH_THRESHOLD = 0;
  public static final char WILDCARD = 'N';

  private int[][] scoringMatrix;
  private int gapOpeningPenalty;
  private int gapExtensionPenalty;
  private int suffixLength;
  private int maxPairwiseScore;
  private double contextSearchThreshold;

  protected Configuration(int[][] scoringMatrix, int gapOpeningPenalty, int gapExtensionPenalty) {
    this.scoringMatrix = scoringMatrix;
    this.gapOpeningPenalty = gapOpeningPenalty;
    this.gapExtensionPenalty = gapExtensionPenalty;
    this.suffixLength = DEFAULT_SUFFIX_LENGTH;
    maxPairwiseScore = findMaxScore(scoringMatrix);
    this.contextSearchThreshold = DEFAULT_CONTEXT_SEARCH_THRESHOLD;
  }

  private int findMaxScore(int[][] scoringMatrix) {
    int max = Integer.MIN_VALUE;
    for (int i = 0; i < scoringMatrix.length; i++) {
      for (int j = 0; j < scoringMatrix[i].length; j++) {
        if (scoringMatrix[i][j] > max) {
          max = scoringMatrix[i][j];
        }
      }
    }

    return max;
  }

  public int getScore(char a, char b) {
    if (a == WILDCARD || b == WILDCARD) {
      return maxPairwiseScore;
    }

    final int A = 0;
    final int C = 1;
    final int G = 2;
    final int T = 3;

    if (a == Graph.HEAD_VALUE || a == Graph.TAIL_VALUE || b == Graph.HEAD_VALUE
        || b == Graph.TAIL_VALUE) {
      return gapOpeningPenalty;
    }

    int i = -1;
    if (a == 'A') {
      i = A;
    } else if (a == 'C') {
      i = C;
    } else if (a == 'G') {
      i = G;
    } else if (a == 'T') {
      i = T;
    }

    int j = -1;
    if (b == 'A') {
      j = A;
    } else if (b == 'C') {
      j = C;
    } else if (b == 'G') {
      j = G;
    } else if (b == 'T') {
      j = T;
    }

    return scoringMatrix[i][j];
  }

  public int getGapPenalty(int distance) {
    if (distance == 0) {
      return gapOpeningPenalty;
    } else if (distance == 1) {
      return 0;
    } else if (distance == 2) {
      return gapOpeningPenalty;
    } else {
      return gapOpeningPenalty + ((distance - 2) * gapExtensionPenalty);
    }
  }

  public void setSuffixLength(int suffixLength) {
    System.out.println("Sat suffix length to " + suffixLength);
    this.suffixLength = suffixLength;
  }

  public int getSuffixLength() {
    return suffixLength;
  }

  public int getGapOpeningPenalty() {
    return gapOpeningPenalty;
  }

  public int getGapExtensionPenalty() {
    return gapExtensionPenalty;
  }

  public int getMaxPairwiseScore() {
    return maxPairwiseScore;
  }

  public void setContextSearchThreshold(double contextSearchThreshold) {
    this.contextSearchThreshold = contextSearchThreshold;
  }

  public int getMaxAlignmentScore(String s) {
    int score = 0;
    for (Character c : s.toCharArray()) {
      score += getScore(c, c);
    }

    return score;
  }

  public int getMaxDistance() {
    int i = 1;
    while (getGapPenalty(i) < getContextSearchThreshold()) {
      i++;
    }

    return i;
  }

  public double getContextSearchThreshold() {
    return contextSearchThreshold;
  }
}
