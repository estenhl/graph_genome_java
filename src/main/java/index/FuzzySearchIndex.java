package index;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import configuration.Configuration;
import context_search.SuffixTree;
import data.Alignment;
import data.Graph;
import data.Score;
import utils.ArrayUtils;
import utils.StringUtils;

public class FuzzySearchIndex implements Serializable {
  private Configuration configuration;
  private Graph graph;
  private SuffixTree leftContexts;
  private SuffixTree rightContexts;

  public static FuzzySearchIndex buildIndex(Graph graph, Configuration configuration) {
    System.out.println("Building index");
    FuzzySearchIndex index = new FuzzySearchIndex();
    index.setConfiguration(configuration);
    index.setGraph(graph);

    Object[] contexts = graph.getContexts(Graph.LEFT_CONTEXT);
    SuffixTree leftContexts = new SuffixTree(configuration);
    int i = 1;
    while (i < contexts.length && contexts[i] != null) {
      for (String s : (Set<String>) contexts[i]) {
        leftContexts.addSuffix(s, i);
      }
      i++;
    }
    index.setLeftContexts(leftContexts);

    SuffixTree rightContexts = new SuffixTree(configuration);
    contexts = graph.getContexts(Graph.RIGHT_CONTEXT);
    i = 1;
    while (i < contexts.length && contexts[i] != null) {
      for (String s : (Set<String>) contexts[i]) {
        rightContexts.addSuffix(s, i);
      }
      i++;
    }
    index.setRightContexts(rightContexts);

    System.out.println("Finished building indexes");
    return index;
  }

  public static FuzzySearchIndex readIndex(String filename) {
    System.out.println("Reading index from file " + filename);
    try {
      long start = System.nanoTime();
      FileInputStream fis = new FileInputStream(filename);
      ObjectInputStream stream = new ObjectInputStream(fis);
      FuzzySearchIndex index = (FuzzySearchIndex) stream.readObject();
      System.out.println("Time for reading index: " + (System.nanoTime() - start));
      return index;
    } catch (IOException e) {
      System.out.println("Unable to read index from file " + filename);
      return null;
    } catch (ClassNotFoundException e) {
      System.out.println("Internal error. Try rebuilding the project");
      return null;
    }
  }

  public Object[] improvedFuzzyContextSearch(String s) {
    if (configuration.getAllowParallellization()) {
      System.out.println("Doing search with parallellization");
    }
    Object[] leftContextScores = new Object[s.length()];
    Object[] rightContextScores = new Object[s.length()];
    int tenPercent = s.length() / 10;
    int status = 0;
    long leftTotal = 0;
    long rightTotal = 0;
    Thread[] leftThreads = new Thread[s.length()];
    Thread[] rightThreads = new Thread[s.length()];
    for (int i = 0; i < s.length(); i++) {
      System.out.println("i: " + i);
      if (s.length() > 10 && i % tenPercent == 0) {
        System.out.println(status++ * 10 + " percent done");
      }
      boolean force = false;
      if (i - 1 < configuration.getContextLength() && s.length() - (i + 1) < configuration
          .getContextLength()) {
        force = true;
      }
      if (configuration.getAllowParallellization()) {
        leftContexts.setSearchParams(StringUtils.reverse(
            s.substring(Math.max(0, i - (configuration.getContextLength())), i)), force, i);
        rightContexts.setSearchParams(
            s.substring(i + 1, Math.min(s.length(), i + 1 + configuration.getContextLength())),
            force, i);
        leftThreads[i] = new Thread(leftContexts);
        leftThreads[i].start();
        rightThreads[i] = new Thread(rightContexts);
        rightThreads[i].start();
        int counter = 0;
        leftContexts.await();
        rightContexts.await();
      } else {
        leftContextScores[i] = leftContexts.improvedSearch(StringUtils.reverse(
            s.substring(Math.max(0, i - (configuration.getContextLength())), i)), force, i);
        rightContextScores[i] = rightContexts.improvedSearch(
            s.substring(i + 1, Math.min(s.length(), i + 1 + configuration.getContextLength())),
            force, i);
      }
    }
    if (configuration.getAllowParallellization()) {
      for (int i = 0; i < leftThreads.length; i++) {
        try {
          leftThreads[i].join();
          rightThreads[i].join();
        } catch (InterruptedException e) {
          e.printStackTrace();
          System.exit(-1);
        }
        leftContextScores[i] = leftContexts.getScores(i);
        rightContextScores[i] = rightContexts.getScores(i);
      }
    }

    return combineScores(leftContextScores, rightContextScores, s);
  }

  private double getMaxAlignmentScore(String sequence) {
    int score = 0;
    for (Character c : sequence.toCharArray()) {
      score += configuration.getScore(c, c);
    }
    return score;
  }

  private Object[] combineScores(Object[] scores1, Object[] scores2, String s) {
    char[] characters = s.toCharArray();
    Object[] combined = new Object[scores1.length];
    for (int i = 0; i < scores1.length; i++) {
      SortedSet<Score> combinedForPosition = new TreeSet<Score>();
      HashMap<Integer, Integer> map1 = (HashMap<Integer, Integer>) scores1[i];
      HashMap<Integer, Integer> map2 = (HashMap<Integer, Integer>) scores2[i];
      double maxScore = -1000;
      for (Map.Entry<Integer, Integer> entry : map1.entrySet()) {
        Score score;
        if (map2.containsKey(entry.getKey())) {
          score = new Score(entry.getValue() + map2.get(entry.getKey()), entry.getKey());
        } else {
          score = new Score(entry.getValue(), entry.getKey());
        }
        if (score.getScore() >= maxScore) {
          combinedForPosition.add(score);
          maxScore = score.getScore();
        } else if (score.getScore() >= maxScore - configuration.getErrorMargin()) {
          combinedForPosition.add(score);
        }
      }
      for (Map.Entry<Integer, Integer> entry : map2.entrySet()) {
        if (!map1.containsKey(entry.getKey())) {
          Score score = new Score(entry.getValue(), entry.getKey());
          if (score.getScore() >= maxScore) {
            combinedForPosition.add(score);
            maxScore = score.getScore();
          } else if (score.getScore() >= maxScore - configuration.getErrorMargin()) {
            combinedForPosition.add(score);
          }
        }
      }
      combined[i] = combinedForPosition.subSet(new Score(maxScore + 1, -1),
          new Score(maxScore - configuration.getErrorMargin() - 1, -1));
    }

    return combined;
  }

  public Alignment findMostProbablePath(Object[] alignmentScores, String sequence, long startTime) {
    System.out.println("Finding most probable path");

    int maxDistance = configuration.getMaxDistance();
    int[][] scores = new int[alignmentScores.length][0];
    int[][] indexes = new int[alignmentScores.length][0];
    String[][] backPointers = new String[alignmentScores.length][0];
    char[] characters = sequence.toCharArray();

    SortedSet<Score> row = (SortedSet<Score>) alignmentScores[0];
    scores[0] = new int[row.size()];
    indexes[0] = new int[row.size()];
    backPointers[0] = new String[row.size()];
    int i = 0;
    for (Score s : row) {
      scores[0][i] = configuration.getScore(graph.getNode(s.getIndex()).getValue(), characters[0]);
      indexes[0][i] = s.getIndex();
      backPointers[0][i] = "-1:-1";
      i++;
    }
    int tenPercent = alignmentScores.length / 10;
    int status = 0;

    for (i = 1; i < alignmentScores.length; i++) {
      row = (SortedSet<Score>) alignmentScores[i];
      if (tenPercent > 0 && i % tenPercent == 0) {
        System.out.println(status++ * 10 + " percent done");
      }
      scores[i] = new int[row.size()];
      indexes[i] = new int[row.size()];
      backPointers[i] = new String[row.size()];
      int j = 0;
      for (Score s : row) {
        scores[i][j] = (-2 * configuration.getErrorMargin()) - 1;
        indexes[i][j] = -1;
        backPointers[i][j] = "-1:-1";
        int baseScore = configuration
            .getScore(graph.getNode(s.getIndex()).getValue(), characters[i]);
        for (int k = Math.max(0, i - maxDistance); k < i; k++) {
          for (int l = 0; l < scores[k].length; l++) {
            int score = baseScore + scores[k][l] - configuration
                .getGapPenalty(graph.getDistance(indexes[k][l], s.getIndex(), maxDistance))
                - configuration.getGapPenalty(i - k);

            if (score > scores[i][j]) {
              scores[i][j] = score;
              indexes[i][j] = s.getIndex();
              backPointers[i][j] = k + ":" + l;
            }
          }
        }
        j++;
      }
    }

    int rowNr = scores.length - 1;
    int initialGapLength = 1;
    while (scores[rowNr] == null || scores[rowNr].length == 0) {
      initialGapLength += 1;
      rowNr -= 1;
      if (rowNr == -1) {
        Alignment alignment = new Alignment();
        alignment.setAlignment(new int[sequence.length()]);
        alignment.setScore(Double.MIN_VALUE);
        return alignment;
      }
    }
    int colNr = ArrayUtils.findHighestIndex(scores[rowNr]);
    int max = scores[rowNr][colNr];

    int[] alignmentSequence = new int[scores.length];
    while (rowNr >= 0) {
      alignmentSequence[rowNr] = indexes[rowNr][colNr];
      String backPointer = backPointers[rowNr][colNr];
      rowNr = Integer.parseInt(backPointer.split(":")[0]);
      colNr = Integer.parseInt(backPointer.split(":")[1]);
    }

    long searchTime = System.nanoTime() - startTime;
    Alignment alignment = new Alignment();
    if (max < configuration.getMaxAlignmentScore(sequence) - configuration.getErrorMargin()) {
      alignment.setScore(
          configuration.getMaxAlignmentScore(sequence) - (2 * configuration.getErrorMargin()) - 1);
      alignment.setAlignment(new int[sequence.length()]);
    } else {
      alignment.setScore(max - configuration.getGapPenalty(initialGapLength));
      alignment.setAlignment(alignmentSequence);
    }
    alignment.setTime(searchTime);
    alignment.setType("Fuzzy search");
    alignment.setSequenceLength(characters.length);
    alignment.setGraphSize(graph.getCurrentSize());

    return alignment;
  }

  private void setGraph(Graph graph) {
    this.graph = graph;
  }

  private void setLeftContexts(SuffixTree leftContexts) {
    this.leftContexts = leftContexts;
  }

  private void setRightContexts(SuffixTree rightContexts) {
    this.rightContexts = rightContexts;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
    if (graph != null) {
      graph.setConfiguration(configuration);
    }
    if (leftContexts != null) {
      leftContexts.setConfiguration(configuration);
    }
    if (rightContexts != null) {
      rightContexts.setConfiguration(configuration);
    }
  }

  public Graph getGraph() {
    return graph;
  }

  public Alignment align(String sequence) {
    System.out
        .println("Aligning " + sequence + " with error-margin " + configuration.getErrorMargin());
    long start = System.nanoTime();
    Object[] alignmentScores = improvedFuzzyContextSearch(sequence);
    long time = System.nanoTime() - start;
    System.out.println("context time: " + time);

    Alignment alignment = findMostProbablePath(alignmentScores, sequence, start);
    return alignment;
  }

  public void writeToFile(String filename) throws IOException {
    System.out.println("Storing index to file " + filename);
    long start = System.nanoTime();
    FileOutputStream fos = new FileOutputStream(filename);
    ObjectOutputStream stream = new ObjectOutputStream(fos);
    stream.writeObject(this);
    stream.close();
    System.out.println("Time used writing the index: " + (System.nanoTime() - start));
  }
}
