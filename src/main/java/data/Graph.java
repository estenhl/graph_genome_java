package data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import configuration.Configuration;
import utils.GraphUtils;
import utils.Pair;

public class Graph {

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

    Node newPrev = getHead();
    for (int i = 0; i < characters.length; i++) {
      if (alignment[i] == -1) {
        Node n = new Node(characters[i]);
        int value = addNode(n);
        n.addIncoming(newPrev.getIndex());
        newPrev.addOutgoing(value);
        newPrev = n;
        continue;
      }
      Node curr = getNode(alignment[i]);
      if (curr.getValue() != characters[i]) {
        Node n = new Node(characters[i]);
        int value = addNode(n);
        n.addIncoming(newPrev.getIndex());
        newPrev.addOutgoing(value);
        newPrev = n;
      } else {
        newPrev.addOutgoing(curr.getIndex());
        curr.addIncoming(newPrev.getIndex());
        newPrev = curr;
      }
    }
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
    Object[] suffixes = new Object[nodes.length];
    if (LEFT_CONTEXT.equals(direction)) {
      queue.add(getHead());
      addSuffix(suffixes, 0, "");
    } else {
      queue.add(getTail());
      addSuffix(suffixes, -1, "");
    }

    while (!queue.isEmpty()) {
      Node curr = queue.remove(0);
      int index = curr.getIndex();
      if (index == -1) {
        index = nodes.length - 1;
      }
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
        queue.add(getNode(i));
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
      return maxDistance;
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

  public int getTotalSize() {
    return totalSize;
  }
}
