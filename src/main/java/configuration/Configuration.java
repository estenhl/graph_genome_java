package configuration;

import java.io.Serializable;

import data.Graph;
import utils.LogUtils;

public abstract class Configuration implements Serializable {
  public static final int DEFAULT_SUFFIX_LENGTH = 15;
  public static final int DEFAULT_ERROR_MARGIN = 0;
  public static final char WILDCARD = 'N';

  private int[][] scoringMatrix;
  private int gapOpeningPenalty;
  private int gapExtensionPenalty;
  private int contextLength;
  private int maxPairwiseScore;
  private int minPairwiseScore;
  private int errorMargin;
  private boolean allowParallellization;

  protected Configuration(int[][] scoringMatrix, int gapOpeningPenalty, int gapExtensionPenalty) {
    this.scoringMatrix = scoringMatrix;
    this.gapOpeningPenalty = gapOpeningPenalty;
    this.gapExtensionPenalty = gapExtensionPenalty;
    this.contextLength = DEFAULT_SUFFIX_LENGTH;
    this.errorMargin = DEFAULT_ERROR_MARGIN;
    setMinAndMax(scoringMatrix);
  }

  public void setAllowParallellization(boolean allowParallellization) {
    this.allowParallellization = allowParallellization;
  }

  public boolean getAllowParallellization() {
    return allowParallellization;
  }

  private void setMinAndMax(int[][] scoringMatrix) {
    int max = Integer.MIN_VALUE;
    int min = Integer.MAX_VALUE;
    for (int i = 0; i < scoringMatrix.length; i++) {
      for (int j = 0; j < scoringMatrix[i].length; j++) {
        if (scoringMatrix[i][j] > max) {
          max = scoringMatrix[i][j];
        }
        if (scoringMatrix[i][j] < min) {
          min = scoringMatrix[i][j];
        }
      }
    }

    maxPairwiseScore = max;
    minPairwiseScore = min;
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
    if (i != -1 && j != -1) {
      return scoringMatrix[i][j];
    } else {
      LogUtils.printError("Looking up invalid bases " + a + " or " + b);
      return minPairwiseScore;
    }
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

  public void setContextLength(int contextLength) {
    LogUtils.printInfo("Sat context length to " + contextLength);
    this.contextLength = contextLength;
  }

  public int getContextLength() {
    return contextLength;
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

  public void setErrorMargin(int errorMargin) {
    this.errorMargin = errorMargin;
  }

  public int getErrorMargin() {
    return errorMargin;
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
    while (getGapPenalty(i) <= getErrorMargin()) {
      i++;
    }

    return i;
  }

}
