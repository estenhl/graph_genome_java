package data;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import configuration.Configuration;
import utils.GraphUtils;
import utils.Pair;

public class Graph implements Serializable {

  public static final char HEAD_VALUE = '$';
  public static final char TAIL_VALUE = '$';
  public static final int HEAD_INDEX = 0;
  public static final int TAIL_INDEX = -1;

  public static final String LEFT_CONTEXT = "left";
  public static final String RIGHT_CONTEXT = "right";

  private Configuration configuration;
  private int totalSize;
  private int currentIndex;
  private Node[] nodes;

  public Graph(Configuration configuration, int size) {
    this.configuration = configuration;
    this.totalSize = size;
    this.currentIndex = 1;

    nodes = new Node[size];
    nodes[0] = new Node(HEAD_VALUE);
    nodes[0].setIndex(HEAD_INDEX);
    nodes[size - 1] = new Node(TAIL_VALUE);
    nodes[size - 1].setIndex(TAIL_INDEX);
  }

  public int getCurrentSize() {
    return currentIndex;
  }

  public int addNode(Node n) {
    if (currentIndex == totalSize - 1) {
      nodes = GraphUtils.doubleNodeArray(nodes);
      totalSize = (totalSize * 2) - 1;
    }
    nodes[currentIndex] = n;
    n.setIndex(currentIndex++);

    return n.getIndex();
  }

  public int addSNP(char c, int i) {
    Node old = getNode(i);
    if (old.getValue() == c) {
      System.out.println("Impossible to create SNP with matching character");
      return i;
    }
    Node n = new Node(c);
    Set<Integer> incoming = new HashSet<Integer>();
    incoming.addAll(old.getIncoming());
    n.setIncoming(incoming);
    Set<Integer> outgoing = new HashSet<Integer>();
    outgoing.addAll(old.getOutgoing());
    n.setOutgoing(outgoing);
    return addNode(n);
  }

  public boolean isNeighbours(int node, int neighbour) {
    if (node >= currentIndex) {
      return false;
    }
    return getNode(node).getOutgoing().contains(neighbour);
  }

  public void mergeSequence(String sequence, int[] alignment) throws IOException {
    char[] characters = sequence.toCharArray();
    if (characters.length != alignment.length) {
      System.out.println("Invalid alignment");
      return;
    }

    Set<Node> newPrev = getPrev(alignment);
    for (int i = 0; i < characters.length; i++) {
      if (alignment[i] == 0) {
        Node n = new Node(characters[i]);
        int value = addNode(n);
        for (Node prev : newPrev) {
          n.addIncoming(prev.getIndex());
          prev.addOutgoing(value);
          newPrev.remove(prev);
        }
        newPrev.add(n);
        continue;
      }
      Node curr = getNode(alignment[i]);
      if (curr.getValue() != characters[i]) {
        Node n = new Node(characters[i]);
        int value = addNode(n);
        for (Node prev : newPrev) {
          n.addIncoming(prev.getIndex());
          prev.addOutgoing(value);
          newPrev.remove(prev);
        }
        newPrev.add(n);
      } else {
        for (Node prev : newPrev) {
          curr.addIncoming(prev.getIndex());
          prev.addOutgoing(curr.getIndex());
          newPrev.remove(prev);
        }
        newPrev.add(curr);
      }
    }
  }

  private Set<Node> getPrev(int[] alignment) {
    Set<Node> prev = new HashSet<Node>();
    for (int i = 0; i < alignment.length; i++) {
      if (alignment[i] != 0) {
        for (Integer neighbour : getNode(alignment[i]).getIncoming()) {
          prev.add(getNode(neighbour));
        }
        return prev;
      }
    }
    prev.add(getHead());
    return prev;
  }

  public Node getNode(int index) {
    if (index == -1) {
      return nodes[nodes.length - 1];
    } else {
      return nodes[index];
    }
  }

  public Node[] getNodes() {
    return nodes;
  }

  public Node getHead() {
    return getNode(0);
  }

  public Node getTail() {
    return getNode(-1);
  }

  public Object[] getContexts(String direction) {
    List<Node> queue = new ArrayList<Node>();
    Set<Integer> active = new HashSet<Integer>();
    Object[] suffixes = new Object[nodes.length];
    if (LEFT_CONTEXT.equals(direction)) {
      for (Integer i : getHead().getOutgoing()) {
        queue.add(getNode(i));
        addSuffix(suffixes, i, "");
        active.add(i);
      }
    } else {
      for (Integer i : getTail().getIncoming()) {
        queue.add(getNode(i));
        addSuffix(suffixes, i, "");
        active.add(i);
      }
    }

    while (!queue.isEmpty()) {
      Node curr = queue.remove(0);
      if (curr == getHead() || curr == getTail()) {
        continue;
      }
      int index = curr.getIndex();
      if (index == -1) {
        index = nodes.length - 1;
      }
      active.remove(index);
      Set<Integer> neighbours;
      Set<Integer> prev;
      if (LEFT_CONTEXT.equals(direction)) {
        neighbours = curr.getOutgoing();
        prev = curr.getIncoming();
      } else {
        neighbours = curr.getIncoming();
        prev = curr.getOutgoing();
      }
      if (((Set<String>) suffixes[index]).size() < prev.size()) {
        System.out.println("Readding " + curr.getIndex());
        queue.add(curr);
        continue;
      }

      for (Integer i : neighbours) {
        for (String suffix : (Set<String>) suffixes[index]) {
          addSuffix(suffixes, i, curr.getValue() + suffix);
        }
        if (!active.contains(i)) {
          active.add(i);
          queue.add(getNode(i));
        }
      }
    }

    return suffixes;
  }

  private void addSuffix(Object[] suffixes, int index, String suffix) {
    if (index == -1) {
      index = suffixes.length - 1;
    }
    if (suffixes[index] == null) {
      suffixes[index] = new HashSet<String>();
    }
    suffix = suffix.substring(0, Math.min(suffix.length(), configuration.getSuffixLength()));
    ((Set<String>) suffixes[index]).add(suffix);
  }

  public int[] getDistances(int source) {
    int[] distances = new int[nodes.length];
    List<Integer> queue = new ArrayList<Integer>();
    queue.add(source);
    while (!queue.isEmpty()) {
      Node curr = getNode(queue.remove(0));
      for (Integer neighbour : curr.getOutgoing()) {
        if (neighbour == TAIL_INDEX) {
          distances[distances.length - 1] = distances[curr.getIndex() + 1];
        } else if (neighbour != source && distances[neighbour] == 0) {
          distances[neighbour] = distances[curr.getIndex()] + 1;
          queue.add(neighbour);
        }
      }
    }
    return distances;
  }

  public int getDistance(int source, int dest, int maxDistance) {
    if (source == dest) {
      return 2;
    }
    Set<Integer> visited = new HashSet<Integer>();
    List<Pair> queue = new ArrayList<Pair>();
    queue.add(new Pair(source, 0));
    while (!queue.isEmpty()) {
      Pair p = queue.remove(0);
      int node = p.getKey();
      int distance = p.getValue();
      visited.add(node);
      if (node == dest) {
        return distance;
      } else {
        for (Integer i : getNode(node).getOutgoing()) {
          if (!visited.contains(i) && distance + 1 < maxDistance) {
            queue.add(new Pair(i, distance + 1));
          }
        }
      }
    }
    return maxDistance;
  }

  public double getBranchingFactor() {
    int branches = 0;
    for (int i = 0; i < currentIndex; i++) {
      branches += nodes[i].getOutgoing().size();
    }
    return (double) branches / currentIndex;
  }

  public int getTotalSize() {
    return totalSize;
  }
}
