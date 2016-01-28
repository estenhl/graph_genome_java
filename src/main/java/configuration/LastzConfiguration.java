package configuration;

public class LastzConfiguration extends Configuration {
  private static final int[][] SCORING_MATRIX = new int[][] {
      new int[] { 91, -114, -31, -123 },
      new int[] { -114, 100, -125, -31 },
      new int[] { -31, -125, 100, -114 },
      new int[] { -123, -31, -114, 91 }
  };
  private static final int GAP_OPENING_PENALTY = 400;
  private static final int GAP_EXTENSION_PENALTY = 30;

  public LastzConfiguration() {
    super(SCORING_MATRIX, GAP_OPENING_PENALTY, GAP_EXTENSION_PENALTY);
  }
}
