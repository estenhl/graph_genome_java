package data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import configuration.Configuration;
import utils.GraphUtils;
import utils.LogUtils;
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

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
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
      LogUtils.printError("Impossible to create SNP with matching character");
      return i;
    }
    Node n = new Node(c);
    addNode(n);
    Set<Integer> incoming = new HashSet<Integer>();
    incoming.addAll(old.getIncoming());
    for (Integer neighbour : incoming) {
      getNode(neighbour).addOutgoing(n.getIndex());
    }
    n.setIncoming(incoming);
    Set<Integer> outgoing = new HashSet<Integer>();
    outgoing.addAll(old.getOutgoing());
    for (Integer neighbour : outgoing) {
      getNode(neighbour).addIncoming(n.getIndex());
    }
    n.setOutgoing(outgoing);
    return n.getIndex();
  }

  public void addDeletion(String ref, int index) {
    Node curr = getNode(index);
    if (curr == null) {
      return;
    }
    Node end = findEndOfPath(index, ref);
    if (end != null) {
      curr.getOutgoing().addAll(end.getOutgoing());
      for (Integer neighbour : end.getOutgoing()) {
        getNode(neighbour).addIncoming(curr.getIndex());
      }
    }
  }

  public void addInsertion(String variant, int index) {
    Node start = getNode(index);
    Set<Integer> outgoing = new HashSet<Integer>();
    outgoing.addAll(start.getOutgoing());
    Node prev = start;
    for (int i = 0; i < variant.length(); i++) {
      Node curr = new Node(variant.charAt(i));
      addNode(curr);
      curr.addIncoming(prev.getIndex());
      prev.addOutgoing(curr.getIndex());
      prev = curr;
    }
    prev.getOutgoing().addAll(outgoing);
    for (Integer neighbour : outgoing) {
      getNode(neighbour).addIncoming(prev.getIndex());
    }
  }

  public Node findEndOfPath(int start, String path) {
    Node n = getNode(start);
    if (n == null) {
      LogUtils.printError("Looking up invalid path " + path);
      return null;
    }
    for (int i = 0; i < path.length(); i++) {
      Node next = null;
      for (Integer neighbour : n.getOutgoing()) {
        next = getNode(neighbour);
        if (next.getValue() == path.charAt(i)) {
          break;
        }
        next = null;
      }
      if (next == null) {
        LogUtils.printError("Looking up invalid path");
        return null;
      }
      n = next;
    }

    return n;
  }

  public boolean isNeighbours(int node, int neighbour) {
    if (node >= currentIndex) {
      return false;
    }
    return getNode(node).getOutgoing().contains(neighbour);
  }

  public void mergeSequence(String sequence, int[] alignment) {
    char[] characters = sequence.toCharArray();
    if (characters.length != alignment.length) {
      LogUtils.printError("Invalid alignment");
      return;
    }

    Node prev = getHead();
    for (int i = 0; i < characters.length; i++) {
      Node n;
      int index;
      if (alignment[i] == 0 || sequence.charAt(i) != getNode(alignment[i]).getValue()) {
        n = new Node(characters[i]);
        index = addNode(n);
      } else {
        n = getNode(alignment[i]);
        index = n.getIndex();
      }
      prev.addOutgoing(index);
      n.addIncoming(prev.getIndex());
      prev = n;
    }
    prev.addOutgoing(TAIL_INDEX);
    getTail().addIncoming(prev.getIndex());
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
    boolean[] finished = new boolean[nodes.length];
    if (LEFT_CONTEXT.equals(direction)) {
      for (Integer i : getHead().getOutgoing()) {
        queue.add(getNode(i));
        addSuffix(suffixes, i, "");
        active.add(i);
        finished[0] = true;
      }
    } else {
      for (Integer i : getTail().getIncoming()) {
        queue.add(getNode(i));
        addSuffix(suffixes, i, "");
        active.add(i);
        finished[finished.length - 1] = true;
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
      for (Integer incoming : prev) {
        if (incoming == TAIL_INDEX) {
          incoming = finished.length - 1;
        }
        if (!finished[incoming]) {
          queue.add(curr);
          continue;
        }
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
      if (curr.getIndex() == TAIL_INDEX) {
        finished[finished.length - 1] = true;
      } else {
        finished[curr.getIndex()] = true;
      }
    }

    return suffixes;
  }

  private void addSuffix(Object[] suffixes, int index, String suffix) {
    if (index == TAIL_INDEX || index == HEAD_INDEX) {
      return;
    }
    if (suffixes[index] == null) {
      suffixes[index] = new HashSet<String>();
    }
    suffix = suffix.substring(0, Math.min(suffix.length(), configuration.getContextLength()));
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
      return maxDistance * 2;
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
    return maxDistance * 2;
  }

  public double getApproxBranchingFactor() {
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
