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
import utils.LogUtils;
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
    VALID_PARAMS.add("--error-margin");
    VALID_PARAMS.add("--suffix-length");
    VALID_PARAMS.add("--png");
    VALID_PARAMS.add("--help");
    VALID_PARAMS.add("--type");
    VALID_PARAMS.add("--merge");
    VALID_PARAMS.add("--parallellization");
    VALID_PARAMS.add("--vcf");

    SHORTHAND_PARAMS = new HashMap<String, String>();
    SHORTHAND_PARAMS.put("-if", "--input-fastas");
    SHORTHAND_PARAMS.put("-is", "--input-sequences");
    SHORTHAND_PARAMS.put("-i", "--index");
    SHORTHAND_PARAMS.put("-as", "--align-sequence");
    SHORTHAND_PARAMS.put("-af", "--align-fasta");
    SHORTHAND_PARAMS.put("-ss", "--scoring-system");
    SHORTHAND_PARAMS.put("-em", "--error-margin");
    SHORTHAND_PARAMS.put("-sl", "--suffix-length");
    SHORTHAND_PARAMS.put("-p", "--png");
    SHORTHAND_PARAMS.put("-h", "--help");
    SHORTHAND_PARAMS.put("-t", "--type");
    SHORTHAND_PARAMS.put("-m", "--merge");
    SHORTHAND_PARAMS.put("-pa", "--parallellization");
    SHORTHAND_PARAMS.put("-v", "--vcf");

    HELP_MENU = new HashMap<String, String>();
    HELP_MENU.put("-if", "Comma separated FASTA files used to build the graph");
    HELP_MENU.put("-is", "Comma separated input sequences used to build the graph");
    HELP_MENU.put("-i", "Name of file where index is written or read, depending on the type");
    HELP_MENU.put("-as", "Sequence which is to be aligned against the graph");
    HELP_MENU.put("-af", "FASTA file which is to be aligned against the graph");
    HELP_MENU.put("-ss",
        "Scoring schema to use. Possible values are lastz and edit-distance. Defaults to edit-distance");
    HELP_MENU.put("-em", "Error margin for alignment. Defaults to "
        + Configuration.DEFAULT_ERROR_MARGIN);
    HELP_MENU.put("-sl",
        "Suffix length to use. Default to length with " + GraphUtils.SHARED_SUFFIX_PROBABILITY +
            " probability of sharing suffixes");
    HELP_MENU.put("-p",
        "Filename of png file visualizing either graph or alignment. Will store dot-file if dot is not installed");
    HELP_MENU.put("-h", "Shows this menu");
    HELP_MENU.put("-t", "Alignment algorithm to use. po_msa or fuzzy. Defaults to fuzzy");
    HELP_MENU.put("-m",
        "Chooses whether the aligned sequence should be merged in to the graph and index");
    HELP_MENU.put("-pa",
        "Decides whether or not to use parallellization in suffix tree search, true/false");
    HELP_MENU.put("-v",
        "A vcf file containing variants. See README for allowed formats");
  }

  public static void main(String[] args)
      throws IOException, InterruptedException, ClassNotFoundException {
    if (args.length == 0) {
      LogUtils.printError("Needs a type parameter! Either index or align");
      LogUtils.printError("Run with help parameter for help menu");
      return;
    }
    Map<String, String> params = parseParams(args);
    if ("help".equals(args[0]) || params.keySet().contains("--help")) {
      printHelp();
      return;
    }
    Configuration configuration = getConfiguration(params.get("--scoring-system"));
    int suffixLength = ParseUtils.parseInt(params.get("--suffix-length"), -1);
    configuration.setErrorMargin(ParseUtils.parseInt(params.get("--error-margin"),
        Configuration.DEFAULT_ERROR_MARGIN));
    configuration.setAllowParallellization("true".equals(params.get("--parallellization")));
    if ("index".equals(args[0])) {
      buildIndex(configuration, params, suffixLength, true);
    } else if ("align".equals(args[0])) {
      align(configuration, params, null);
    } else if ("build-and-align".equals(args[0])) {
      FuzzySearchIndex index = buildIndex(configuration, params, suffixLength, false);
      align(configuration, params, index);
    } else {
      System.out.println("Invalid type parameter! See help");
      return;
    }
  }

  private static FuzzySearchIndex buildIndex(Configuration configuration,
      Map<String, String> params,
      int suffixLength, boolean write) {
    long start = System.nanoTime();
    long graphStart = System.nanoTime();
    Graph graph = parseGraph(configuration, params.get("--input-fastas"),
        params.get("--input-sequences"), suffixLength != -1);
    LogUtils.printInfo("Time used building graph: " + (System.nanoTime() - graphStart));
    if (graph == null) {
      LogUtils.printInfo("Unable to build graph! Exiting");
      return null;
    }
    if (params.get("--vcf") != null) {
      ParseUtils.addVariants(params.get("--vcf"), graph);
    }

    if (params.get("--png") != null) {
      printGraph(graph, params.get("--png"), null, null);
    }
    long indexStart = System.nanoTime();
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    LogUtils.printInfo("Time used creating index: " + (System.nanoTime() - indexStart));

    if (write) {
      if (params.get("--index") == null) {
        LogUtils.printError("Unable to write index without filename. Use --index=<filename>");
      }
      try {
        index.writeToFile(params.get("--index"));
      } catch (IOException e) {
        LogUtils.printError("IOException when writing to file " + params.get("--index"));
      }
    }
    LogUtils.printInfo("Time used building the index: " + (System.nanoTime() - start));

    return index;
  }

  private static void align(Configuration configuration, Map<String, String> params,
      FuzzySearchIndex index) {
    if (index == null) {
      if (params.get("--index") == null) {
        LogUtils.printError("Unable to align without an index. Use --index=<filename>");
        return;
      }
      index = FuzzySearchIndex.readIndex(params.get("--index"));
    }
    if (index == null) {
      LogUtils.printError("Unable to align sequence without an index");
      return;
    }
    Graph graph = index.getGraph();
    index.setConfiguration(configuration);
    configuration.setContextLength(GraphUtils.optimalSuffixLength(graph));
    String sequence = null;
    if (params.get("--align-sequence") == null && params.get("--align-fasta") == null) {
      LogUtils.printError(
          "Need a sequence for alignment. Use --align-sequence=<sequence> or --align-fasta=<filename>");
      return;
    } else if (params.get("--align-sequence") != null && params.get("--align-fasta") != null) {
      LogUtils.printError(
          "Alignment of multiple sequences disabled. Use only one of the parameters --align-sequence or --align-fasta");
      return;
    } else if (params.get("--align-sequence") != null) {
      sequence = params.get("--align-sequence");
    } else {
      try {
        sequence = ParseUtils.fastaToSequence(params.get("--align-fasta"));
      } catch (IOException e) {
        LogUtils.printError("Unable to open file " + params.get("--align-fasta"));
        return;
      }
    }
    Alignment alignment = alignSequence(configuration, graph, index, sequence,
        params.get("--type"));
    if (alignment != null) {
      LogUtils.printInfo("Branching factor: " + graph.getApproxBranchingFactor());
      LogUtils.printInfo(alignment.toString());
    }

    if ("true".equals(params.get("--merge"))) {
      graph.mergeSequence(sequence, alignment.getAlignment());
      index = FuzzySearchIndex.buildIndex(graph, configuration);
      try {
        index.writeToFile(params.get("--index"));
      } catch (IOException e) {
        LogUtils.printError("Unable to write index to file " + params.get("--index"));
      }
      if (params.get("--png") != null) {
        printGraph(graph, params.get("--png"), null, null);
      }
    } else if (params.get("--png") != null) {
      printGraph(graph, params.get("--png"), alignment.getAlignment(), sequence);
    }
  }

  private static Alignment alignSequence(Configuration configuration, Graph g,
      FuzzySearchIndex index, String sequence, String type) {
    if (type == null || "fuzzy".equals(type)) {
      return index.align(sequence);
    } else if ("po_msa".equals(type)) {
      return AlignmentUtils.align(g, sequence, configuration);
    } else {
      LogUtils.printError("Invalid alignment algorithm");
      return null;
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
        } else {
          System.out.println("Dropping invalid parameter " + args[i]);
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
      LogUtils.printInfo("Using LASTZ scoring configuration");
      configuration = new LastzConfiguration();
    } else if ("edit-distance".equals(type)) {
      LogUtils.printInfo("Using edit distance scoring configuration");
      configuration = new EditDistanceConfiguration();
    } else {
      LogUtils.printInfo("Using default scoring configuration (edit distance)");
      configuration = new EditDistanceConfiguration();
    }

    return configuration;
  }

  public static Graph parseGraph(Configuration configuration, String filesString,
      String sequencesString, boolean setSuffixLength) {
    String[] files = null;
    if (filesString != null) {
      files = filesString.split(",");
    }
    String[] sequences = null;
    if (sequencesString != null) {
      sequences = sequencesString.split(",");
    }
    Graph graph = null;

    if (files == null && sequences == null) {
      LogUtils.printError("Needs atleast one fasta file or input sequence to build graph");
      return null;
    }
    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        try {
          graph = createOrMerge(configuration, graph, ParseUtils.fastaToSequence(files[i]));
          configuration.setContextLength(GraphUtils.optimalSuffixLength(graph));
        } catch (IOException e) {
          LogUtils.printError("Unable to open file " + files[i]);
        }
      }
    }

    if (sequences != null) {
      for (int i = 0; i < sequences.length; i++) {
        graph = createOrMerge(configuration, graph, sequences[i]);
        configuration.setContextLength(GraphUtils.optimalSuffixLength(graph));
      }
    }

    return graph;
  }

  public static Graph createOrMerge(Configuration configuration, Graph graph, String sequence) {
    if (graph == null) {
      return ParseUtils.stringToGraph(configuration, sequence);
    }
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    graph.mergeSequence(sequence, index.align(sequence).getAlignment());

    return graph;
  }

  public static void printGraph(Graph graph, String filename, int[] alignment, String sequence) {
    if (graph == null || filename == null) {
      return;
    }
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

    File dotFile = new File(filename + ".dot");
    File pngFile = new File(filename + ".png");
    try {
      LogUtils.printInfo("Writing dot representation to " + dotFile.getAbsolutePath());
      BufferedWriter writer = new BufferedWriter(new FileWriter(dotFile));
      writer.write(output);
      writer.close();

      try {
        LogUtils.printInfo("Writing png representation to " + pngFile.getAbsolutePath());
        String[] c = { "dot", "-Tpng", dotFile.getAbsolutePath(), "-o", pngFile.getAbsolutePath() };
        Process p = Runtime.getRuntime().exec(c);
        int err = p.waitFor();
        if (err != 0) {
          LogUtils.printError("Unable to convert dot to png. Maybe you don't have dot?");
        }
      } catch (Exception e) {
        LogUtils.printError("Unable to write png-file to " + pngFile.getAbsolutePath());
      }
    } catch (IOException e) {
      LogUtils.printError("Unable to write dot-file to " + dotFile.getAbsolutePath());
    }
  }
}
