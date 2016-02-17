package results;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
  private final int GRAPH_SIZE = 10000;
  private final int RUNS = 50;
  private final double BRANCHING_PROBABILITY = 0.01;
  private final int SEED = 10215;
  private final int[] SIZES = { 500, 1000, 5000, 10000, 50000, 100000 };
  private final double[] THRESHOLDS = { 0.0, 1.0, 2.0, 3.0, 4.0 };

  private String filename;

  public Tester(String filename) {
    this.filename = filename;
  }

  public void test() throws IOException, InterruptedException {
    if ("generic".equals(filename)) {
      genericEditDistanceSizeTest();
    } else if ("controlled".equals(filename)) {
      controlledTest();
    } else if ("threshold".equals(filename)) {
      genericEditDistanceThresholdTest();
    } else if (filename != null && filename.startsWith("create")) {
      String[] split = filename.split(":");
      if (split.length > 1) {
        filename = split[1];
      } else {
        filename = "test";
      }
      createTestFiles();
    } else {
      editDistanceFromFileTest();
    }
  }

  public void controlledTest() throws IOException, InterruptedException {
    Configuration configuration = new EditDistanceConfiguration();
    Graph graph = ParseUtils.fastaToGraph(configuration, "data/mhc_A3105/primary.fasta");
    Random random = new Random(SEED);
    Index index = FuzzySearchIndex.buildIndex(graph, configuration);

    final int EDIT_DISTANCE_DIFFERENCE = 6;
    final int CONTEXT_SEARCH_THRESHOLD = 6;
    final int RUNS = 20;
    final int READ_LENGTH = 50;

    int[][] errors = new int[EDIT_DISTANCE_DIFFERENCE][CONTEXT_SEARCH_THRESHOLD];
    int[][] runs = new int[EDIT_DISTANCE_DIFFERENCE][CONTEXT_SEARCH_THRESHOLD];
    for (int i = 0; i < EDIT_DISTANCE_DIFFERENCE; i++) {
      for (int j = 0; j < CONTEXT_SEARCH_THRESHOLD; j++) {
        configuration.setContextSearchThreshold(j);
        for (int k = 0; k < RUNS; k++) {
          System.out.println("Edit distance: " + i + ", threshold: " + j + ", run: " + k);
          String s = generateStrictRandomSequence(random, graph, READ_LENGTH, i);
          Alignment bruteForce = AlignmentUtils.align(graph, s, configuration);
          Alignment fuzzy = index.align(s);
          if (fuzzy.getScore() != bruteForce.getScore()) {
            errors[Math.abs(new Double(bruteForce.getScore()).intValue())][j] += 1;
            if (j >= i) {
              System.out.println(bruteForce);
              System.out.println(fuzzy);
              System.exit(-1);
            }
          }
          runs[Math.abs(new Double(bruteForce.getScore()).intValue())][j] += 1;
        }
      }
    }

    for (int i = 0; i < EDIT_DISTANCE_DIFFERENCE; i++) {
      for (int j = 0; j < CONTEXT_SEARCH_THRESHOLD; j++) {
        System.out.println(
            "Actual edit distance: " + i + ", threshold: " + j + ", percentage errors: " + (
                (double) errors[i][j] / runs[i][j]));
      }
    }
    System.out.println("To r: \n");
    String s = "matrix(c(";
    for (int i = 0; i < EDIT_DISTANCE_DIFFERENCE; i++) {
      for (int j = 0; j < CONTEXT_SEARCH_THRESHOLD; j++) {
        s += (double) errors[i][j] / runs[i][j] + ",";
      }
    }
    s = s.substring(0, s.length() - 1) + "), nrow=" + EDIT_DISTANCE_DIFFERENCE + ", ncol="
        + CONTEXT_SEARCH_THRESHOLD + ")\n";
    System.out.println(s);
  }

  public void genericEditDistanceSizeTest() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextSearchThreshold(2);
    Random random = new Random(SEED);
    long[] bruteForceTimes = new long[SIZES.length];
    long[] fuzzySearchTimes = new long[SIZES.length];
    int[] errors = new int[SIZES.length];
    for (int i = 0; i < SIZES.length; i++) {
      Graph graph = ParseUtils.stringToGraph(configuration, generateRandomString(random, SIZES[i]));
      Index index = FuzzySearchIndex.buildIndex(graph, configuration);
      for (int j = 0; j < RUNS; j++) {
        System.out.println("Size: " + SIZES[i] + ", run: " + j + "/" + RUNS);
        String sequence = generateRandomSequence(random, graph);
        Alignment brute = AlignmentUtils.align(graph, sequence, configuration);
        Alignment fuzzy = index.align(sequence);
        bruteForceTimes[i] += brute.getTime();
        fuzzySearchTimes[i] += fuzzy.getTime();
        if (brute.getScore() != fuzzy.getScore()) {
          errors[i] += 1;
        }
      }
    }
    for (int i = 0; i < SIZES.length; i++) {
      System.out.println("Size: " + SIZES[i]);
      System.out.println("Brute force time: " + bruteForceTimes[i] / RUNS);
      System.out.println("Fuzzy search time: " + fuzzySearchTimes[i] / RUNS);
      System.out.println("Error percentage: " + (double) errors[i] / RUNS);
    }
  }

  public void createTestFiles() throws IOException {
    Random random = new Random(SEED);
    File dir = new File(filename);
    dir.mkdir();
    Configuration configuration = new EditDistanceConfiguration();
    for (int i = 0; i < SIZES.length; i++) {
      String path = filename + "/" + SIZES[i];
      dir = new File(path);
      dir.mkdir();
      String s = generateRandomString(random, SIZES[i]);
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(path) + "/ref.fasta"));
      writer.write("> Test fasta file with length " + SIZES[i] + "\n");
      writer.write(s);
      writer.close();
      path = path + "/sequences";
      dir = new File(path);
      dir.mkdir();
      Graph graph = ParseUtils.stringToGraph(configuration, s);
      for (int j = 0; j < RUNS; j++) {
        System.out.println("Size: " + SIZES[i] + ", run: " + j);
        String sequence = generateRandomSequence(random, graph);
        writer = new BufferedWriter(new FileWriter(new File(path + "/" + j + ".fasta")));
        writer.write("> Test fasta file\n");
        writer.write(sequence);
        writer.close();
        writer = new BufferedWriter(new FileWriter(new File(path + "/" + j + ".result")));
        Alignment alignment = AlignmentUtils.align(graph, sequence, configuration);
        writer.write("Score: " + alignment.getScore() + "\n");
        writer.write("Time: " + alignment.getTime() + "\n");
        writer.write("Alignment: " + Arrays.toString(alignment.getAlignment()) + "\n");
        writer.close();
      }
    }
  }

  public void genericEditDistanceThresholdTest() {
    Configuration configuration = new EditDistanceConfiguration();
    Random random = new Random(SEED);
    Graph graph = ParseUtils.stringToGraph(configuration, generateRandomString(random, GRAPH_SIZE));
    Index index = FuzzySearchIndex.buildIndex(graph, configuration);
    long[] fuzzyTimes = new long[THRESHOLDS.length];
    long bruteTime = 0;
    int[] errors = new int[THRESHOLDS.length];
    for (int i = 0; i < THRESHOLDS.length; i++) {
      configuration.setContextSearchThreshold(THRESHOLDS[i]);
      for (int j = 0; j < RUNS; j++) {
        System.out.println("Threshold: " + THRESHOLDS[i] + ", run: " + j);
        String sequence = generateRandomSequence(random, graph);
        Alignment brute = AlignmentUtils.align(graph, sequence, configuration);
        bruteTime += brute.getTime() / (THRESHOLDS.length * RUNS);
        Alignment fuzzy = index.align(sequence);
        fuzzyTimes[i] += fuzzy.getTime();
        if (brute.getScore() != fuzzy.getScore()) {
          errors[i] += 1;
        }
      }
    }
    System.out.println("Brute time: " + bruteTime);
    for (int i = 0; i < THRESHOLDS.length; i++) {
      System.out.println("Threshold: " + THRESHOLDS[i]);
      System.out.println("Time: " + fuzzyTimes[i] / RUNS);
      System.out.println("Error percentage: " + (double) errors[i] / RUNS);
    }
  }

  public void editDistanceFromFileTest() throws IOException {
    Configuration configuration = new EditDistanceConfiguration();
    Graph graph = ParseUtils.fastaToGraph(configuration, filename);
    Index index = FuzzySearchIndex.buildIndex(graph, configuration);
    Random rand = new Random(SEED);
    List<Integer[]> brute = new ArrayList<Integer[]>();
    List<Integer[]> fuzzy = new ArrayList<Integer[]>();
    long bruteForceTime = 0;
    long fuzzySearchTime = 0;
    double scoreDifference = 0;
    int errors = 0;
    for (int i = 0; i < RUNS; i++) {
      String sequence = generateRandomSequence(rand, graph);
      Alignment bruteForce = AlignmentUtils.align(graph, sequence, configuration);
      Alignment fuzzySearch = index.align(sequence);
      if (bruteForce.getScore() != fuzzySearch.getScore()) {
        errors += 1;
        Integer[] temp = new Integer[bruteForce.getAlignment().length];
        for (int j = 0; j < temp.length; j++) {
          temp[j] = Integer.valueOf(bruteForce.getAlignment()[j]);
        }
        brute.add(temp);
        temp = new Integer[fuzzySearch.getAlignment().length];
        for (int j = 0; j < temp.length; j++) {
          temp[j] = Integer.valueOf(fuzzySearch.getAlignment()[j]);
        }
        fuzzy.add(temp);
      }
      bruteForceTime += bruteForce.getTime();
      fuzzySearchTime += fuzzySearch.getTime();
      scoreDifference = fuzzySearch.getScore() - bruteForce.getScore();
    }
    for (int i = 0; i < brute.size(); i++) {
      Integer[] temp1 = brute.get(i);
      Integer[] temp2 = fuzzy.get(i);
      System.out.println("Error:");
      for (int j = 0; j < temp1.length; j++) {
        System.out.println(temp1[j] + ":" + temp2[j]);
      }
    }
    System.out.println("Size: " + graph.getCurrentSize());
    System.out.println("Avg brute force over " + RUNS + " runs: " + (bruteForceTime / RUNS));
    System.out.println("Avg fuzzy search over " + RUNS + " runs: " + (fuzzySearchTime / RUNS));
    System.out.println("Avg score difference over " + RUNS + " runs: " + (scoreDifference / RUNS));
    System.out.println("% of wrong alignments: " + (double) errors / RUNS);
  }

  private String generateRandomString(Random random, int length) {
    String s = "";
    for (int i = 0; i < length; i++) {
      s += getRandomBase(random);
    }

    return s;
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

  private String generateStrictRandomSequence(Random random, Graph graph, int length, int errors) {
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

  private char getRandomBase(Random random) {
    return new char[] { 'A', 'C', 'G', 'T' }[random.nextInt(4)];
  }
}
