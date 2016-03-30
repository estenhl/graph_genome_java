import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import configuration.Configuration;
import configuration.EditDistanceConfiguration;
import configuration.LastzConfiguration;
import data.Alignment;
import data.Graph;
import data.Node;
import index.FuzzySearchIndex;
import utils.AlignmentUtils;
import utils.DOTUtils;
import utils.GraphUtils;
import utils.ParseUtils;

public class GraphGenome {

  private static List<String> VALID_PARAMS;
  private static Map<String, String> SHORTHAND_PARAMS;
  private static Map<String, String> HELP_MENU;

  static {
    VALID_PARAMS = new ArrayList<String>();
    VALID_PARAMS.add("--input-fastas");
    VALID_PARAMS.add("--input-sequences");
    VALID_PARAMS.add("--index");
    VALID_PARAMS.add("--align-sequence");
    VALID_PARAMS.add("--align-file");
    VALID_PARAMS.add("--scoring-system");
    VALID_PARAMS.add("--threshold");
    VALID_PARAMS.add("--suffix-length");
    VALID_PARAMS.add("--dot");
    VALID_PARAMS.add("--help");

    SHORTHAND_PARAMS = new HashMap<String, String>();
    SHORTHAND_PARAMS.put("-if", "--input-fastas");
    SHORTHAND_PARAMS.put("-is", "--input-sequences");
    SHORTHAND_PARAMS.put("-i", "--index");
    SHORTHAND_PARAMS.put("-as", "--align-sequence");
    SHORTHAND_PARAMS.put("-af", "--align-fasta");
    SHORTHAND_PARAMS.put("-ss", "--scoring-system");
    SHORTHAND_PARAMS.put("-t", "--threshold");
    SHORTHAND_PARAMS.put("-sl", "--suffix-length");
    SHORTHAND_PARAMS.put("-d", "--dot");
    SHORTHAND_PARAMS.put("-h", "--help");

    HELP_MENU = new HashMap<String, String>();
    HELP_MENU.put("-if", "Comma separated FASTA files used to build the graph");
    HELP_MENU.put("-is", "Comma separated input sequences used to build the graph");
    HELP_MENU.put("-i", "Name of file where index is written or read, depending on the type");
    HELP_MENU.put("-as", "Sequence which is to be aligned against the graph");
    HELP_MENU.put("-af", "FASTA file which is to be aligned against the graph");
    HELP_MENU.put("-ss",
        "Scoring schema to use. Possible values are lastz and edit-distance. Defaults to edit-distance");
    HELP_MENU.put("-t", "Threshold used for alignment. Defaults to "
        + Configuration.DEFAULT_CONTEXT_SEARCH_THRESHOLD);
    HELP_MENU.put("-sl",
        "Suffix length to use. Default to length with " + GraphUtils.SHARED_SUFFIX_PROBABILITY +
            " probability of sharing suffixes");
    HELP_MENU.put("-d", "Filename of dot file visualizing either graph or alignment");
    HELP_MENU.put("-h", "Shows this menu");
  }

  public static void main(String[] args)
      throws IOException, InterruptedException, ClassNotFoundException {
    if (args.length == 0) {
      System.out.println("Needs a type parameter! Either index or align");
      System.out.println("Run with help parameter for help menu");
      return;
    }
    Map<String, String> params = parseParams(args);
    if ("help".equals(args[0]) || params.keySet().contains("--help")) {
      printHelp();
      return;
    }
    Configuration configuration = getConfiguration(params.get("--scoring-system"));
    int suffixLength = ParseUtils.parseInt(params.get("--suffix-length"), -1);
    configuration.setContextSearchThreshold(
        ParseUtils.parseDouble(params.get("--threshold"),
            Configuration.DEFAULT_CONTEXT_SEARCH_THRESHOLD));
    if ("index".equals(args[0])) {
      Graph graph = parseGraph(configuration, params.get("--input-fastas"),
          params.get("--input-sequences"), suffixLength != -1);
      if (graph == null) {
        System.out.println("Unable to build graph! Exiting");
        return;
      }

      if (params.get("--dot") != null) {
        printGraph(graph, params.get("--dot"), null, null);
      }
      FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
      if (params.get("--index") == null) {
        System.out.println("Unable to write index without filename. Use --index=<filename>");
        return;
      }
      index.writeToFile(params.get("--index"));
    } else if ("align".equals(args[0])) {
      if (params.get("--index") == null) {
        System.out.println("Unable to align without an index. Use --index=<filename>");
        return;
      }
      FuzzySearchIndex index = FuzzySearchIndex.readIndex(params.get("--index"));
      Graph graph = index.getGraph();
      index.setConfiguration(configuration);
      Alignment fuzzy = null;
      Alignment poMsa = null;
      if (params.get("--align-sequence") == null && params.get("--align-fasta") == null) {
        System.out.println(
            "Need a sequence for alignment. Use --align-sequence=<sequence> or --align-fasta=<filename>");
        return;
      } else if (params.get("--align-sequence") != null && params.get("--align-fasta") != null) {
        System.out.println(
            "Unable to align multiple sequences. Use only one of the parameters --align-sequence or --align-fasta");
        return;
      } else if (params.get("--align-sequence") != null) {
        fuzzy = index.align(params.get("--align-sequence"));
        poMsa = AlignmentUtils.align(graph, params.get("--align-sequence"), configuration);
      } else {
        String sequence = ParseUtils.fastaToSequence(params.get("--align-fasta"));
        fuzzy = index.align(sequence);
        poMsa = AlignmentUtils.align(graph, sequence, configuration);
      }
      System.out.println(fuzzy);
      System.out.println(poMsa);
    } else {
      System.out.println("Invalid type parameter! See help");
      return;
    }

  }

  public static Map<String, String> parseParams(String[] args) {
    Map<String, String> params = new HashMap<String, String>();
    for (int i = 1; i < args.length; i++) {
      if (args[i].contains("=")) {
        String key = args[i].split("=")[0];
        String value = args[i].split("=")[1];
        if (VALID_PARAMS.contains(key)) {
          params.put(key, value);
        } else if (SHORTHAND_PARAMS.keySet().contains(key)) {
          params.put(SHORTHAND_PARAMS.get(key), value);
        }
      }
    }

    return params;
  }

  private static void printHelp() {
    String shorthandName = "Short:";
    String paramName = "Parameter:";
    String helpName = "Description:";
    int shorthandLength =
        Math.max(shorthandName.length(), findLongestElement(SHORTHAND_PARAMS.keySet())) + 2;
    int paramLength =
        Math.max(paramName.length(), findLongestElement(SHORTHAND_PARAMS.values())) + 2;
    int helpLength = Math.max(helpName.length(), findLongestElement(HELP_MENU.values())) + 2;

    System.out.println("Run with one of two type flags: index or align");
    System.out.println(
        ">java -jar graph-genome.jar index [--input-fastas=<fasta_1>,<fasta_2>,...,<fasta_n> --input-sequences=<sequence_1>,<sequence_2>,...,<sequence_n>] --index=<index-file> (--scoring-system=<type>) (--suffix-length=<length>) (--threshold=<threshold>) (--dot=<dot-file>)");
    System.out.println(
        ">java -jar graph-genome.jar align --index=<index-file> [--align-fasta=<fasta> --align-sequence=<sequence>] (--scoring-system=<type>) (--suffix-length=<length>) (--threshold=<threshold>) (--dot=<dot-file>)");
    System.out.println(
        "Parameters in brackets means atleast one of them is necessary, parenthesis mean optional");
    System.out.println();
    System.out.printf("%-" + paramLength + "s %-" + shorthandLength + "s %-" + helpLength + "s\n",
        paramName, shorthandName, helpName);
    for (String s : SHORTHAND_PARAMS.keySet()) {
      System.out.printf("%-" + paramLength + "s %-" + shorthandLength + "s %-" + helpLength + "s\n",
          SHORTHAND_PARAMS.get(s), s, HELP_MENU.get(s));
    }
  }

  private static int findLongestElement(Collection<String> elements) {
    int length = 0;
    for (String s : elements) {
      if (s.length() > length) {
        length = s.length();
      }
    }

    return length;
  }

  private static Configuration getConfiguration(String type) {
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
      String sequencesString, boolean setSuffixLength)
      throws IOException {
    String[] files = null;
    if (filesString != null) {
      files = filesString.split(",");
    }
    String[] sequences = null;
    if (sequencesString != null) {
      sequences = sequencesString.split(",");
    }
    Graph graph = null;

    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        try {
          graph = createOrMerge(configuration, graph, ParseUtils.fastaToSequence(files[i]));
          configuration.setSuffixLength(GraphUtils.optimalSuffixLength(graph));
        } catch (IOException e) {
          System.out.println("Unable to open file " + files[i]);
        }
      }
    }

    if (sequences != null) {
      for (int i = 0; i < sequences.length; i++) {
        graph = createOrMerge(configuration, graph, sequences[i]);
        configuration.setSuffixLength(GraphUtils.optimalSuffixLength(graph));
      }
    }

    return graph;
  }

  public static Graph createOrMerge(Configuration configuration, Graph graph, String sequence)
      throws IOException {
    if (graph == null) {
      return ParseUtils.stringToGraph(configuration, sequence);
    }
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    graph.mergeSequence(sequence, index.align(sequence).getAlignment());

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
