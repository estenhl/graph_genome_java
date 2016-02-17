import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import configuration.Configuration;
import configuration.EditDistanceConfiguration;
import configuration.LastzConfiguration;
import data.Alignment;
import data.Graph;
import data.Node;
import index.FuzzySearchIndex;
import results.Tester;
import utils.AlignmentUtils;
import utils.DOTUtils;
import utils.GraphUtils;
import utils.ParseUtils;

public class GraphGenome {
  public static void main(String[] args) throws IOException, InterruptedException {
    Map<String, String> params = parseArgs(args);
    if (params == null) {
      return;
    }

    if (params.containsKey("Test")) {
      Tester tester = new Tester(params.get("Test"));
      tester.test();
      return;
    }
    Configuration configuration = getConfiguration(params.get("Type"));

    if (params.get("Threshold") != null) {
      configuration.setContextSearchThreshold(Double.parseDouble(params.get("Threshold")));
    } else {
      System.out.println(
          "No context search scoring threshold set. Defaulted threshold to " + configuration
              .getContextSearchThreshold());
    }

    Graph graph = parseGraph(configuration, params.get("Input file"), params.get("Input sequence"));

    if (params.get("Suffix length") != null) {
      configuration.setSuffixLength(Integer.parseInt(params.get("Suffix length")));
    } else {
      configuration.setSuffixLength(GraphUtils.optimalSuffixLength(graph, 1.0));
      System.out.println(
          "No suffix length supplied! Defaulted suffix length to " + configuration.getSuffixLength()
              + ". Probability of shared suffixes is <" + GraphUtils.SHARED_SUFFIX_PROBABILITY);
    }

    if (params.get("Sequence") == null && params.get("Sequence file") == null) {
      printGraph(graph, params.get("Print"), null, null);
      return;
    }

    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);

    String sequence = params.get("Sequence");
    if (sequence == null && params.get("Sequence file") != null) {
      sequence = ParseUtils.fastaToSequence(params.get("Sequence file"));
    }

    Alignment bruteForce = AlignmentUtils.align(graph, sequence, configuration);
    Alignment fuzzySearch = index.align(sequence);
    System.out.println(bruteForce);
    System.out.println(fuzzySearch);

    printGraph(graph, params.get("Print"), fuzzySearch.getAlignment(), sequence);
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
      } else if (s.startsWith("--scoring-system") || s.startsWith("-ss")) {
        key = "Type";
      } else if (s.startsWith("--threshold") || s.startsWith("-t")) {
        key = "Threshold";
      } else if (s.startsWith("--suffix-length") || s.startsWith("-sl")) {
        key = "Suffix length";
      } else if (s.startsWith("--help") || s.startsWith("-h")) {
        printHelp();
        return null;
      } else {
        System.out.println("Invalid parameter " + s + " not handled");
        continue;
      }
      String input = s.split("=")[1];
      params.put(key, input);
    }

    return params;
  }

  public static void printHelp() {
    System.out.printf("%-20s%-13s%-40s\n", "Name:", "Abbreviation:", "Description:");
    System.out.printf("%-20s%-13s%-40s\n", "--input-file", "-if",
        "Comma separated list of fasta files used to build graph");
    System.out.printf("%-20s%-13s%-40s\n", "--input-sequence", "-is",
        "Comma separated list of sequences used to build graph");
    System.out.printf("%-20s%-13s%-40s\n", "--sequence", "-s", "Sequence to be aligned");
    System.out.printf("%-20s%-13s%-40s\n", "--print", "-p",
        "Name of file where dot-formatted output is written");
    System.out.printf("%-20s%-13s%-40s\n", "--test", "NA", "Preset test runs");
    System.out.printf("%-20s%-13s%-40s\n", "--scoring-system", "-ss",
        "Scoring system to use. Can be either edit-distance or lastz. Defaults to edit-distance");
    System.out.printf("%-20s%-13s%-40s\n", "--threshold", "-t",
        "Threshold used for fuzzy context search. Defaults to "
            + Configuration.DEFAULT_CONTEXT_SEARCH_THRESHOLD);
    System.out.printf("%-20s%-13s%-40s\n", "--suffix-length", "-sl",
        "Length of contexts used (on each side). Default is computed by approximating the length needed for a probability of <"
            + GraphUtils.SHARED_SUFFIX_PROBABILITY + " of sharing contexts");
    System.out.printf("%-20s%-13s%-40s\n", "--help", "-h", "Shows this menu");
  }

  public static Configuration getConfiguration(String type) {
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
      graph = ParseUtils.fastaToGraph(configuration, files[0]);
      for (int i = 1; i < files.length; i++) {
        FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
        graph.mergeSequence(sequences[i],
            index.align(ParseUtils.fastaToSequence(files[i])).getAlignment());
      }
      for (int i = 0; i < sequences.length; i++) {
        FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
        graph.mergeSequence(sequences[i], index.align(sequences[i]).getAlignment());
      }
    } else if (files != null) {
      System.out.println("Creating graph from files");
      graph = ParseUtils.fastaToGraph(configuration, files[0]);
      for (int i = 1; i < files.length; i++) {
        System.out.println("Adding sequence from " + files[i]);
        String sequence = ParseUtils.fastaToSequence(files[i]);
        FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
        Alignment alignment = index.align(sequence);
        graph.mergeSequence(sequence, alignment.getAlignment());
      }
    } else if (sequences != null) {
      System.out.println("Creating graph from sequence " + sequences[0]);
      graph = ParseUtils.stringToGraph(configuration, sequences[0]);
      for (int i = 1; i < sequences.length; i++) {
        FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
        graph.mergeSequence(sequences[i], index.align(sequences[i]).getAlignment());
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
