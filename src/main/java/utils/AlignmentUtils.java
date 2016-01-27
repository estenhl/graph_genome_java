package utils;

public class AlignmentUtils {
  public static final int SUFFIX_SCORE_THRESHOLD = 2;
  public static final int SUFFIX_LENGTH = 15;

  public static final int MAX_PAIRWISE_ALIGNMENT_SCORE = 4;

  public static final int AFFINE_GAP_PENALTY_OPENING = 6;
  public static final int AFFINE_GAP_PENALTY_CONTINUATION = 3;

  public static final int GAP_STATUS_NO_GAP = 0;
  public static final int GAP_STATUS_GAP_IN_SEQUENCE = 1;
  public static final int GAP_STATUS_GAP_IN_GRAPH = 2;

  public static final int MAX_GAP_LENGTH = 5;

  public static int getScore(char a, char b) {
    if (a == b) {
      return 2;
    } else {
      return 0;
    }
  }

  public static double scaleContextScore(int score) {
    return (double) score / 11;
  }

  public static int getLogGapPenalty(int distance) {
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
