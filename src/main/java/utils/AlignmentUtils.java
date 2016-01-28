package utils;

import data.Graph;

public class AlignmentUtils {
  public static final int SUFFIX_SCORE_THRESHOLD = 150;
  public static final int SUFFIX_LENGTH = 15;

  public static final int MAX_PAIRWISE_ALIGNMENT_SCORE = 100;

  public static final int AFFINE_GAP_PENALTY_OPENING = 400;
  public static final int AFFINE_GAP_PENALTY_CONTINUATION = 30;

  public static final int GAP_STATUS_NO_GAP = 0;
  public static final int GAP_STATUS_GAP_IN_SEQUENCE = 1;
  public static final int GAP_STATUS_GAP_IN_GRAPH = 2;

  public static final int MAX_GAP_LENGTH = 5;

  /*
             A    C    G    T
      A   91 -114  -31 -123
      C -114  100 -125  -31
      G  -31 -125  100 -114
      T -123  -31 -114   91
   */
  public static final int[][] SCORING_TABLE = new int[][] {
      new int[] { 91, -114, -31, -123 },
      new int[] { -114, 100, -125, -31 },
      new int[] { -31, -114, 100, -125 },
      new int[] { -125, -31, -114, 100 }
  };

  public static int getScore(char a, char b) {
    final int A = 0;
    final int C = 1;
    final int G = 2;
    final int T = 3;

    if (a == Graph.HEAD_VALUE || a == Graph.TAIL_VALUE || b == Graph.HEAD_VALUE
        || b == Graph.TAIL_VALUE || a == 'N' || b == 'N') {
      return -200;
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

    return SCORING_TABLE[i][j];
  }

  public static double scaleContextScore(int score) {
    return (double) score / 11;
  }

  public static int getGapPenalty(int distance) {
    if (distance == 1) {
      return 0;
    } else if (distance == 2) {
      return AFFINE_GAP_PENALTY_OPENING;
    } else if (distance <= MAX_GAP_LENGTH) {
      return AFFINE_GAP_PENALTY_OPENING + (distance - 2) * AFFINE_GAP_PENALTY_CONTINUATION;
    } else {
      return AFFINE_GAP_PENALTY_OPENING + (MAX_GAP_LENGTH - 2) * AFFINE_GAP_PENALTY_CONTINUATION;
    }
  }
}
