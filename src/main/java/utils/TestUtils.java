package utils;

import java.util.Random;

import data.Graph;
import data.Node;

public class TestUtils {
  public static final double MUTATION_PROBABILITY = 0.00;
  public static final int READ_LENGTH = 100;
  public static int SEED = 10215;
  public static final int RUNS = 50;
  public static int[] SIZES = { 500, 1000, 5000, 10000, 50000, 100000 };

  public static String generateRandomString(Random random, int length) {
    String s = "";
    for (int i = 0; i < length; i++) {
      s += getRandomBase(random);
    }

    return s;
  }

  public static String generateRandomSequence(Random random, Graph graph) {
    return generateRandomSequence(random, graph, READ_LENGTH, MUTATION_PROBABILITY);
  }

  public static String generateRandomSequence(Random random, Graph graph, int length,
      double mutationProbability) {
    int start = random.nextInt(graph.getCurrentSize() - length - 1);
    String sequence = "";
    Node curr = graph.getNode(start);

    for (int i = 0; i < length; i++) {
      if (random.nextDouble() < mutationProbability) {
        // Deletion
        System.out.println("Generated deletion!");
      } else if (random.nextDouble() < mutationProbability) {
        // Insertion
        System.out.println("Generated insertion!");
        sequence += curr.getValue();
        sequence += getRandomBase(random);
      } else if (random.nextDouble() < mutationProbability) {
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

  public static String generateStrictRandomSequence(Random random, Graph graph, int length,
      int errors) {
    int start = random.nextInt(graph.getCurrentSize() - length - 1);
    String sequence = "";
    Node curr = graph.getNode(start);

    for (int i = 0; i < length; i++) {
      sequence += curr.getValue();
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

    for (int i = 0; i < errors; i++) {
      int type = random.nextInt(3);
      int index = random.nextInt(sequence.length());
      if (type == 0) {
        sequence = sequence.substring(0, index) + sequence
            .substring(Math.min(index, sequence.length() - 1) + 1, sequence.length());
      } else if (type == 1) {
        sequence = sequence.substring(0, index) + getRandomBase(random) + sequence
            .substring(index, sequence.length());
      } else {
        sequence = sequence.substring(0, index) + getRandomBase(random) + sequence
            .substring(Math.min(index, sequence.length() - 1) + 1, sequence.length());
      }
    }

    return sequence;
  }

  public static char getRandomBase(Random random) {
    return new char[] { 'A', 'C', 'G', 'T' }[random.nextInt(4)];
  }
}
