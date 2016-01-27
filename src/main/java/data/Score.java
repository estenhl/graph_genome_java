package data;

public class Score implements Comparable<Score> {
  private double score;
  private int index;

  public Score() {
  }

  public Score(double score, int index) {
    this.score = score;
    this.index = index;
  }

  public void setScore(double score) {
    this.score = score;
  }

  public double getScore() {
    return score;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public int getIndex() {
    return index;
  }

  public int compareTo(Score score) {
    double val = new Double(score.getScore() - this.score);
    if (val > 0) {
      return 1;
    } else if (val < 0) {
      return -1;
    } else {
      return 0;
    }
  }
}
