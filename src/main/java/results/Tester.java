package results;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import configuration.Configuration;
import configuration.EditDistanceConfiguration;
import data.Graph;
import data.Node;
import index.FuzzySearchIndex;
import index.Index;
import utils.ParseUtils;

public class Tester {

  private final int READ_LENGTH = 200;
  private final int RUNS = 20;
  private final double BRANCHING_PROBABILITY = 0.02;
  private final int SEED = 10215;

  private String filename;

  public Tester(String filename) {
    this.filename = filename;
  }

  public void test() throws IOException {
    editDistanceTest();
  }

  public void editDistanceTest() throws IOException {
    System.out.println("Testing with edit distance configuration");
    double[] correctness = new double[RUNS];
    long[] totalTimes = new long[RUNS];
    long[] alignmentTimes = new long[RUNS];
    Configuration configuration = new EditDistanceConfiguration();
    Graph graph = ParseUtils.fastaToGraph(configuration, filename);
    Index index = FuzzySearchIndex.buildIndex(graph, configuration);
    Random random = new Random(SEED);
    for (int i = 0; i < RUNS; i++) {
      List<Node> sequence = generateRandomSequence(random, graph);
      String s = "";
      int[] original = new int[sequence.size()];
      for (int j = 0; j < sequence.size(); j++) {
        Node n = sequence.get(j);
        s += sequence.get(j).getValue();
        if (n.getIndex() != Integer.MIN_VALUE) {
          original[j] = n.getIndex();
        }
      }
      long startTime = System.nanoTime();
      long endTime = System.nanoTime();
      long indexBuildingTime = startTime - endTime;

      startTime = System.nanoTime();
      int[] alignment = index.align(s);
      endTime = System.nanoTime();
      long alignmentTime = endTime - startTime;

      int correctlyMappedBases = 0;
      for (int k = 0; k < original.length; k++) {
        if (alignment[k] == original[k]) {
          correctlyMappedBases += 1;
        }
      }
      double percentage = (double) correctlyMappedBases / alignment.length;
      correctness[i] = percentage;
      alignmentTimes[i] = alignmentTime;
    }
    System.out.println("Number of nodes: " + graph.getCurrentSize());
    for (int i = 0; i < RUNS; i++) {
      System.out.println(
          "Correctness: " + correctness[i] + ", alignmentTime: " + alignmentTimes[i] + "ns");
    }
  }

  private List<Node> generateRandomSequence(Random random, Graph graph) {
    int start = random.nextInt(graph.getCurrentSize() - READ_LENGTH - 1);
    List<Node> sequence = new ArrayList<Node>();
    Node curr = graph.getNode(start);
    for (int i = 0; i < READ_LENGTH; i++) {
      if (random.nextDouble() < BRANCHING_PROBABILITY) {
        // Deletion
        System.out.println("Generated deletion!");
        continue;
      } else if (random.nextDouble() < BRANCHING_PROBABILITY) {
        // Insertion
        System.out.println("Generated insertion!");
        sequence.add(curr);
        Node insert = new Node(getRandomBase(random));
        insert.setIndex(Integer.MIN_VALUE);
        sequence.add(insert);
      } else if (random.nextDouble() < BRANCHING_PROBABILITY) {
        // SNP
        System.out.println("Generated SNP);");
        Node snp = new Node(getRandomBase(random));
        snp.setIndex(curr.getIndex());
      } else {
        sequence.add(curr);
      }

      int index = random.nextInt(curr.getOutgoing().size());
      int j = 0;
      for (Integer neighbour : curr.getOutgoing()) {
        if (j == index) {
          curr = graph.getNode(neighbour);
          break;
        }
        j++;
      }
    }

    return sequence;
  }

  private char getRandomBase(Random random) {
    return new char[] { 'A', 'C', 'G', 'T' }[random.nextInt(4)];
  }
}
