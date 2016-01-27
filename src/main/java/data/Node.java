package data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Node {
  private final char value;
  private int index;
  private Set<Integer> incoming;
  private Set<Integer> outgoing;

  public Node(char value) {
    this.value = value;
    this.incoming = new HashSet<Integer>();
    this.outgoing = new HashSet<Integer>();
  }

  public char getValue() {
    return value;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public int getIndex() {
    return index;
  }

  public void setIncoming(Set<Integer> incoming) {
    this.incoming = incoming;
  }

  public Set<Integer> getIncoming() {
    return incoming;
  }

  public void addIncoming(int neighbour) {
    this.incoming.add(neighbour);
  }

  public void setOutgoing(Set<Integer> outgoing) {
    this.outgoing = outgoing;
  }

  public Set<Integer> getOutgoing() {
    return outgoing;
  }

  public void addOutgoing(int neighbour) {
    this.outgoing.add(neighbour);
  }

  @Deprecated
  public void generateSuffixes(Object[] suffixes, Node[] nodes, String direction) {
    Set<Integer> neighbours = outgoing;
    if (Graph.LEFT_CONTEXT.equals(direction)) {
      neighbours = incoming;
    }
    if ((Graph.RIGHT_CONTEXT.equals(direction) && index == Graph.TAIL_INDEX) ||
        (Graph.LEFT_CONTEXT.equals(direction) && index == Graph.HEAD_INDEX)) {
      addSuffix(suffixes, "", index);
      return;
    }
    for (Integer i : neighbours) {
      if (i == Graph.TAIL_INDEX) {
        i = suffixes.length - 1;
      }
      if (suffixes[i] == null) {
        nodes[i].generateSuffixes(suffixes, nodes, direction);
      }
      for (String s : (List<String>) suffixes[i]) {
        addSuffix(suffixes, nodes[i].getValue() + s, index);
      }
    }
  }

  private void addSuffix(Object[] suffixes, String suffix, int index) {
    if (index == -1) {
      index = suffixes.length - 1;
    }
    Set<String> temp = new HashSet<String>();
    if (suffixes[index] == null) {
      suffixes[index] = temp;
    } else {
      temp = (Set<String>) suffixes[index];
    }
    temp.add(suffix);
  }
}
