import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import configuration.Configuration;
import configuration.EditDistanceConfiguration;
import data.Graph;
import data.Node;
import utils.ParseUtils;
import utils.TestUtils;

public class ReadGenerator {

  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      printHelp();
      System.exit(0);
    } else if ("help".equals(args[0])) {
      printHelp();
      System.exit(0);
    } else if ("reads".equals(args[0]) || "vcf".equals(args[0])) {
      if (args.length < 4) {
        System.out.println("Read generator needs a input file, output file and a number of reads");
        printHelp();
        System.exit(0);
      }
      String inputFile = null;
      String outputFile = null;
      int num = 0;
      int len = TestUtils.READ_LENGTH;
      double prob = TestUtils.MUTATION_PROBABILITY;
      for (String arg : args) {
        if (arg.startsWith("file=")) {
          inputFile = arg.substring(5);
        } else if (arg.startsWith("out=")) {
          outputFile = arg.substring(4);
        } else if (arg.startsWith("num=")) {
          try {
            num = Integer.parseInt(arg.substring(4));
          } catch (NumberFormatException e) {
            System.out.println("Invalid num-argument " + arg.substring(4) + "! must be an integer");
            System.exit(0);
          }
        } else if (arg.startsWith("len=")) {
          try {
            len = Integer.parseInt(arg.substring(4));
          } catch (NumberFormatException e) {
            System.out.println("Invalid len-argument " + arg.substring(4) + "! Must be an integer");
            System.exit(0);
          }
        } else if (arg.startsWith("prob=")) {
          try {
            prob = Double.parseDouble(arg.substring(5));
          } catch (NumberFormatException e) {
            System.out.println("Invalid prob-argument " + arg.substring(4) + "! Must be a float");
            System.exit(0);
          }
        } else if (!"reads".equals(arg) && !"vcf".equals(arg)) {
          System.out.println("Invalid argument " + arg + " skipped");
        }
      }
      if (inputFile != null && outputFile != null && num > 0) {
        if ("reads".equals(args[0])) {
          createSampleReads(inputFile, outputFile, num, len, prob);
        } else {
          createVCF(inputFile, outputFile, num);
        }
      } else {
        System.out.println("Invalid parameters:");
        System.out
            .println("Input file: " + inputFile + ", output file: " + outputFile + ", num: " + num);
      }
    } else if ("vcf".equals(args[0])) {

    } else {
      System.out.println("Invalid type argument");
      printHelp();
      System.exit(-1);
    }
  }

  private static void printHelp() {
    System.out.println("Syntax");
    System.out.println(
        ">java ReadGenerator reads file=<input-file> out=<output-file> num=<num> (len=<read-length>) (prob=<mutation-probability>)");
    System.out.printf("%20s%40s\n", "file=<input-file>", "The fasta file to create reads from");
    System.out.printf("%20s%40s\n", "out=<output-file>", "The file where the reads are written");
    System.out.printf("%20s%40s\n", "num=<num>", "The number of reads to generate");
    System.out
        .printf("%20s%40s\n", "len=<read-length", "Length of the reads (optional, default=100)");
    System.out.printf("%20s%40s\n", "prob=<mutation-probability>",
        "Probability of indels and snps (optional, default=0.01)");
  }

  private static void createSampleReads(String inputFile, String outputFile, int num, int len,
      double prob) throws IOException {
    Configuration configuration = new EditDistanceConfiguration();
    Graph graph = ParseUtils.fastaToGraph(configuration, inputFile);
    Random random = new Random(TestUtils.SEED);
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputFile)));
    for (int i = 0; i < num; i++) {
      String read = TestUtils.generateRandomSequence(random, graph, len, prob);
      writer.write(read);
      writer.newLine();
    }
    writer.flush();
    writer.close();
  }

  private static void createVCF(String inputFile, String outputFile, int num) throws IOException {
    Configuration configuration = new EditDistanceConfiguration();
    Graph graph = ParseUtils.fastaToGraph(configuration, inputFile);
    Random random = new Random(TestUtils.SEED);
    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputFile)));
    writer.write("# Sample VCF file for " + inputFile + "\n");
    for (int i = 0; i < num; i++) {
      int choice = random.nextInt(3);
      Node curr = graph.getNode(random.nextInt(graph.getCurrentSize() - 10));
      if (choice == 0) {
        char base;
        do {
          base = TestUtils.getRandomBase(random);
        } while (base == curr.getValue());
        writer.write("x\t" + curr.getIndex() + "\tx\t" + curr.getValue() + "\t" + base + "\tSNP\n");
      } else if (choice == 1) {
        writer.write("x\t" + curr.getIndex() + "\tx\t" + curr.getValue() + "\t" + curr.getValue() + TestUtils
            .generateRandomString(random, random.nextInt(5) + 1) + "\tInsertion\n");
      } else {
        int index = curr.getIndex();
        String path = "";
        for (int j = 0; j < random.nextInt(5) + 2; j++) {
          path += curr.getValue();
          curr = graph.getNode(
              (Integer) curr.getOutgoing().toArray()[random.nextInt(curr.getOutgoing().size())]);
        }
        writer.write(
            "x\t" + index + "\tx\t" + path + "\t" + graph.getNode(index).getValue()
                + "\tDeletion\n");
      }
    }
    writer.flush();
    writer.close();
  }
}
