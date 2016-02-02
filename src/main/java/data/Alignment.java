package data;

public class Alignment {
  private int[] alignment;
  private double score;
  private long time;
  private String type;
  private int sequenceLength;
  private int graphSize;

  public Alignment() {

  }

  public void setAlignment(int[] alignment) {
    this.alignment = alignment;
  }

  public int[] getAlignment() {
    return alignment;
  }

  public void setScore(double score) {
    this.score = score;
  }

  public double getScore() {
    return score;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public long getTime() {
    return time;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public void setSequenceLength(int sequenceLength) {
    this.sequenceLength = sequenceLength;
  }

  public int getSequenceLength() {
    return sequenceLength;
  }

  public void setGraphSize(int graphSize) {
    this.graphSize = graphSize;
  }

  public int getGraphSize() {
    return graphSize;
  }

  @Override
  public String toString() {
    String s = "Alignment type: " + type + "\n" +
        "Sequence length: " + sequenceLength + "\n" +
        "Graph size: " + graphSize + "\n" +
        "Score: " + score + "\n" +
        "Time: " + time + "\n" +
        "Alignment: ";
    if (alignment != null) {
      for (int i = 0; i < alignment.length; i++) {
        s += alignment[i] + " ";
      }
    }

    return s;
  }
}
