import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import configuration.Configuration;
import configuration.EditDistanceConfiguration;
import configuration.LastzConfiguration;
import data.Graph;
import data.Node;
import index.FuzzySearchIndex;
import results.Tester;
import utils.DOTUtils;
import utils.ParseUtils;

public class GraphGenome {
  public static void main(String[] args) throws IOException {
    Map<String, String> params = parseArgs(args);
    if (params.containsKey("Test")) {
      Tester tester = new Tester(params.get("Test"));
      tester.test();
      System.exit(-1);
    }
    Configuration configuration = getConfiguration(params.get("type"), params.get("suffixLength"),
        params.get("gapLength"));
    Graph graph = parseGraph(configuration, params.get("Input file"), params.get("Input sequence"));
    if (params.get("Sequence") == null && params.get("Sequence file") == null) {
      printGraph(graph, params.get("Print"), null, null);
      return;
    }
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    String sequence = params.get("Sequence");
    if (sequence == null && params.get("Sequence file") != null) {
      sequence = ParseUtils.fastaToSequence(params.get("Sequence file"));
    }
    int[] alignment = index.align(sequence);
    printGraph(graph, params.get("Print"), alignment, sequence);
  }

  public static Map<String, String> parseArgs(String[] args) {
    Map<String, String> params = new HashMap<String, String>();
    for (String s : args) {
      String key;
      if (s.startsWith("--input-file") || s.startsWith("-if")) {
        key = "Input file";
      } else if (s.startsWith("--input-sequence") || s.startsWith("-is")) {
        key = "Input sequence";
      } else if (s.startsWith("--sequence-file") || s.startsWith("-sf")) {
        key = "Sequence file";
      } else if (s.startsWith("--sequence") || s.startsWith("-s")) {
        key = "Sequence";
      } else if (s.startsWith("--print") || s.startsWith("-p")) {
        key = "Print";
      } else if (s.startsWith("--test")) {
        key = "Test";
      } else {
        System.out.println("Invalid parameter " + s + " not handled");
        continue;
      }
      String input = s.split("=")[1];
      params.put(key, input);
    }

    return params;
  }

  public static Configuration getConfiguration(String type, String suffixLength,
      String maxGapLength) {
    Configuration configuration;
    if ("lastz".equals(type)) {
      System.out.println("Using LASTZ scoring configuration");
      configuration = new LastzConfiguration();
    } else if ("edit-distance".equals(type)) {
      System.out.println("Using edit distance scoring configuration");
      configuration = new EditDistanceConfiguration();
    } else {
      System.out.println("Using default scoring configuration (edit distance)");
      configuration = new EditDistanceConfiguration();
    }

    if (suffixLength != null) {
      configuration.setSuffixLength(Integer.parseInt(suffixLength));
    }

    if (maxGapLength != null) {
      configuration.setMaxGapLength(Integer.parseInt(maxGapLength));
    }

    return configuration;
  }

  public static Graph parseGraph(Configuration configuration, String filesString,
      String sequencesString) throws IOException {
    String[] files = null;
    if (filesString != null) {
      files = filesString.split(",");
    }
    String[] sequences = null;
    if (sequencesString != null) {
      sequences = sequencesString.split(",");
    }
    Graph graph = null;
    if (files != null && sequences != null) {
      System.out.println("Creating graph");
      graph = ParseUtils.stringToGraph(configuration, sequences[0]);
      for (int i = 1; i < sequences.length; i++) {
        FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
        graph.mergeSequence(sequences[i], index.align(sequences[i]));
      }
      for (int i = 0; i < files.length; i++) {
        System.out.println("Currently unable to merge sequences. Skipping file " + files[i]);
      }
    } else if (files != null) {
      System.out.println("Creating graph from files");
      graph = ParseUtils.fastaToGraph(configuration, files[0]);
      for (int i = 1; i < files.length; i++) {
        System.out.println("Adding sequence from " + files[i]);
        String sequence = ParseUtils.fastaToSequence(files[i]);
        FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
        int[] alignment = index.align(sequence);
        graph.mergeSequence(sequence, alignment);
      }
    } else if (sequences != null) {
      System.out.println("Creating graph from sequence " + sequences[0]);
      graph = ParseUtils.stringToGraph(configuration, sequences[0]);
      for (int i = 1; i < sequences.length; i++) {
        FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
        graph.mergeSequence(sequences[i], index.align(sequences[i]));
      }
    } else {
      System.out.println("No input sequences given. Aborting");
    }
    return graph;
  }

  public static void printGraph(Graph graph, String fileName, int[] alignment, String sequence)
      throws IOException {
    if (graph == null || fileName == null) {
      return;
    }
    System.out.println("Writing dot representation to " + fileName);
    StringBuilder nodes = new StringBuilder();
    StringBuilder edges = new StringBuilder();
    for (int i = 0; i < graph.getCurrentSize(); i++) {
      Node curr = graph.getNode(i);
      nodes.append(DOTUtils.getNodeRepresentation(curr));

      for (Integer dest : curr.getOutgoing()) {
        int index = curr.getIndex();
        edges.append(
            DOTUtils.getEdgeRepresentation(Integer.toString(index), Integer.toString(dest)));
      }
    }

    if (alignment != null && sequence != null) {
      char[] characters = sequence.toCharArray();
      for (int i = 0; i < alignment.length; i++) {
        nodes.append(DOTUtils.getNodeRepresentation("seq_" + i, characters[i]));
        if (alignment[i] != -1 && alignment[i] != 0) {
          edges.append(DOTUtils.getEdgeRepresentation("seq_" + i, Integer.toString(alignment[i]),
              DOTUtils.ALIGNMENT_COLOR));
        }
        if (i > 0) {
          edges.append(DOTUtils.getEdgeRepresentation("seq_" + (i - 1), "seq_" + i));
        }
      }
    }

    String output = "digraph {\n";
    output += "graph [rankdir=LR, fontname=fixed, splines=true overlap=false, nodesep=1.0]\n\n";
    output += nodes.toString();
    output += "\n";
    output += edges.toString();
    output += "}";

    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)));
    writer.write(output);
    writer.close();
  }
}
