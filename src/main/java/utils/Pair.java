package utils;

public class Pair {
  private int key;
  private int value;

  public Pair(int key, int value) {
    this.key = key;
    this.value = value;
  }

  public void setKey(int key) {
    this.key = key;
  }

  public int getKey() {
    return key;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
