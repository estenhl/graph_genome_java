package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import configuration.Configuration;
import data.Graph;
import data.Node;

public class ParseUtils {
  public static Graph stringToGraph(Configuration configuration, String s) {
    Graph graph = new Graph(configuration, GraphUtils.getGraphSize(s.length() * 2));
    Node prev = graph.getHead();
    char[] chars = s.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      Node curr = new Node(chars[i]);
      int index = graph.addNode(curr);
      prev.addOutgoing(index);
      curr.addIncoming(prev.getIndex());
      prev = curr;
    }
    Node tail = graph.getTail();
    tail.addIncoming(prev.getIndex());
    prev.addOutgoing(tail.getIndex());

    return graph;
  }

  public static String fastaToSequence(String fileName) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
    String header = reader.readLine();
    String sequence = "";
    String line;
    int read = 0;
    while ((line = reader.readLine()) != null) {
      read++;
      if (read % 1000 == 0) {
        LogUtils.printInfo("Read " + read + " lines");
      }
      for (Character c : line.trim().toCharArray()) {
        sequence += Character.toUpperCase(c);
      }
    }
    reader.close();

    return sequence;
  }

  public static Graph fastaToGraph(Configuration configuration, String fileName)
      throws IOException {
    return stringToGraph(configuration, fastaToSequence(fileName));
  }

  public static int parseInt(String s) {
    return parseInt(s, -1);
  }

  public static int parseInt(String s, int defaultValue) {
    if (s == null) {
      return defaultValue;
    }

    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public static double parseDouble(String s) {
    return parseDouble(s, -1.0);
  }

  public static double parseDouble(String s, double defaultValue) {
    if (s == null) {
      return defaultValue;
    }

    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public static void addVariants(String vcf, Graph graph) {
    if (graph == null) {
      LogUtils.printError("Unable to add variants without graph!");
      return;
    }
    LogUtils.printInfo("Adding variants from " + vcf);
    try {
      BufferedReader reader = new BufferedReader(new FileReader(new File(vcf)));

      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("#")) {
          continue;
        }

        String[] tokens = line.split("\t");
        if (tokens.length < 5) {
          LogUtils.printError("Invalid format on vcf file " + vcf
              + "! Skipping variants. See README for info on vcf-files");
          return;
        }
        int index = 0;
        try {
          index = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
          LogUtils.printError("Invalid format on vcf file " + vcf
              + "! Skipping variants. See README for info on vcf-files");
          return;
        }
        String ref = tokens[3];
        String alt = tokens[4];
        if (alt.contains(",")) {
          LogUtils.printError("Multiline SNPs not set up. Skipping variant");
        } else {
          addVariantLine(graph, index, ref, new String[] { alt });
        }
      }
    } catch (IOException e) {
      LogUtils.printError("Unable to find vcf-file " + vcf + "! Skipping variants");
      return;
    }
  }

  private static void addVariantLine(Graph graph, int index, String ref, String[] variants) {
    if (index > graph.getCurrentSize()) {
      LogUtils.printError("vcf file referencing indexes not in the graph! Skipping variants");
      return;
    }
    Node node = graph.getNode(index);
    if (ref.length() < 1 || node.getValue() != ref.charAt(0)) {
      LogUtils.printError("Mismatch between vcf and reference! Skipping variant on index " + index);
      return;
    }

    for (String variant : variants) {
      if (variant.length() == 1 && ref.length() == 1) {
        LogUtils.printInfo("Adding SNP on index " + index);
        graph.addSNP(variant.charAt(0), index);
      } else if (variant.length() == 1) {
        LogUtils.printInfo("Adding deletion on index " + index);
        graph.addDeletion(ref.substring(1), index);
      } else if (ref.length() == 1) {
        LogUtils.printInfo("Adding insertion on index " + index);
        graph.addInsertion(variant.substring(1), index);
      } else {
        LogUtils.printError("Handling of complex variants not set up");
      }
    }
  }
}
