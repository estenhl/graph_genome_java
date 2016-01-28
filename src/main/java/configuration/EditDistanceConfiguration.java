package configuration;

public class EditDistanceConfiguration extends Configuration {
  private static final int[][] SCORING_MATRIX = new int[][] {
      new int[] { 1, 0, 0, 0 },
      new int[] { 0, 1, 0, 0 },
      new int[] { 0, 0, 1, 0 },
      new int[] { 0, 0, 0, 1 }
  };
  private static final int GAP_OPENING_PENALTY = 1;
  private static final int GAP_EXTENSION_PENALTY = 1;

  public EditDistanceConfiguration() {
    super(SCORING_MATRIX, GAP_OPENING_PENALTY, GAP_EXTENSION_PENALTY);
  }
}
