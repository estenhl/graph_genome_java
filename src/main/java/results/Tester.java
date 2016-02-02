package results;

import java.io.IOException;
import java.util.Random;

import configuration.Configuration;
import configuration.EditDistanceConfiguration;
import data.Alignment;
import data.Graph;
import data.Node;
import index.FuzzySearchIndex;
import index.Index;
import utils.AlignmentUtils;
import utils.ParseUtils;

public class Tester {

  private final int READ_LENGTH = 100;
  private final int RUNS = 50;
  private final double BRANCHING_PROBABILITY = 0.01;
  private final int SEED = 10215;

  private String filename;

  public Tester(String filename) {
    this.filename = filename;
  }

  public void test() throws IOException {
    editDistanceTest();
  }

  public void editDistanceTest() throws IOException {
    Configuration configuration = new EditDistanceConfiguration();
    Graph graph = ParseUtils.fastaToGraph(configuration, filename);
    Index index = FuzzySearchIndex.buildIndex(graph, configuration);
    Random rand = new Random(SEED);
    long bruteForceTime = 0;
    long fuzzySearchTime = 0;
    double scoreDifference = 0;
    int errors = 0;
    for (int i = 0; i < RUNS; i++) {
      String sequence = generateRandomSequence(rand, graph);
      Alignment bruteForce = AlignmentUtils.align(graph, sequence, configuration);
      Alignment fuzzySearch = index.align(sequence);
      if (bruteForce.getScore() != fuzzySearch.getScore()) {
        System.out.println("Mismatch!" + bruteForce.getScore() + ", " + fuzzySearch.getScore());
        for (int k = 0; k < sequence.length(); k++) {
          System.out.println(bruteForce.getAlignment()[k] + ":" + fuzzySearch.getAlignment()[k]);
        }
      }
      bruteForceTime += bruteForce.getTime();
      fuzzySearchTime += fuzzySearch.getTime();
      scoreDifference = fuzzySearch.getScore() - bruteForce.getScore();
    }
    System.out.println("Size: " + graph.getCurrentSize());
    System.out.println("Avg brute force over " + RUNS + " runs: " + (bruteForceTime / RUNS));
    System.out.println("Avg fuzzy search over " + RUNS + " runs: " + (fuzzySearchTime / RUNS));
    System.out.println("Avg score difference over " + RUNS + " runs: " + (scoreDifference / RUNS));
  }

  private String generateRandomSequence(Random random, Graph graph) {
    int start = random.nextInt(graph.getCurrentSize() - READ_LENGTH - 1);
    String sequence = "";
    Node curr = graph.getNode(start);
    for (int i = 0; i < READ_LENGTH; i++) {
      if (random.nextDouble() < BRANCHING_PROBABILITY) {
        // Deletion
        System.out.println("Generated deletion!");
      } else if (random.nextDouble() < BRANCHING_PROBABILITY) {
        // Insertion
        System.out.println("Generated insertion!");
        sequence += curr.getValue();
        sequence += getRandomBase(random);
      } else if (random.nextDouble() < BRANCHING_PROBABILITY) {
        // SNP
        System.out.println("Generated SNP!");
        sequence += getRandomBase(random);
      } else {
        sequence += curr.getValue();
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
