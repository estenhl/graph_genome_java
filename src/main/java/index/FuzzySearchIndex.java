package index;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import configuration.Configuration;
import context_search.SuffixTree;
import data.Graph;
import data.Score;
import utils.ArrayUtils;
import utils.StringUtils;

public class FuzzySearchIndex implements Index {
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

  public Object[] improvedFuzzyContextSearch(String s) {
    Object[] leftContextScores = new Object[s.length()];
    Object[] rightContextScores = new Object[s.length()];
    int tenPercent = s.length() / 10;
    int status = 0;
    for (int i = 0; i < s.length(); i++) {
      if (s.length() > 10 && i % tenPercent == 0) {
        System.out.println(status++ * 10 + " percent done");
      }
      leftContextScores[i] = leftContexts.improvedSearch(StringUtils.reverse(
          s.substring(Math.max(0, i - (configuration.getSuffixLength() * 2)), i)));
      rightContextScores[i] = rightContexts.improvedSearch(
          s.substring(i + 1, Math.min(s.length(), i + (configuration.getSuffixLength() * 2))));
    }

    return combineScores(leftContextScores, rightContextScores, s);
  }

  private Object[] combineScores(Object[] scores1, Object[] scores2, String s) {
    char[] characters = s.toCharArray();
    Object[] combined = new Object[scores1.length];
    for (int i = 0; i < scores1.length; i++) {
      SortedSet<Score> combinedForPosition = new TreeSet<Score>();
      HashMap<Integer, Integer> map1 = (HashMap<Integer, Integer>) scores1[i];
      HashMap<Integer, Integer> map2 = (HashMap<Integer, Integer>) scores2[i];
      double maxScore = Double.MIN_VALUE;
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
        } else if (score.getScore() >= maxScore - configuration.getSuffixScoreThreshold()) {
          combinedForPosition.add(score);
        }
      }
      for (Map.Entry<Integer, Integer> entry : map2.entrySet()) {
        if (!map1.containsKey(entry.getKey())) {
          Score score = new Score(entry.getValue(), entry.getKey());
          if (score.getScore() >= maxScore) {
            combinedForPosition.add(score);
            maxScore = score.getScore();
          } else if (score.getScore() >= maxScore - configuration.getSuffixScoreThreshold()) {
            combinedForPosition.add(score);
          }
        }
      }
      combined[i] = combinedForPosition.subSet(new Score(maxScore + 1, -1),
          new Score(maxScore - configuration.getSuffixScoreThreshold() - 1, -1));
    }

    for (int i = 0; i < combined.length; i++) {
      System.out.println("i: " + i);
      for (Score sc : (Set<Score>) combined[i]) {
        System.out.println(sc.getIndex() + ": " + sc.getScore());
      }
    }
    return combined;
  }

  public int[] findMostProbablePath(Object[] alignmentScores, String sequence) {
    System.out.println("Finding most probable path");

    double[][] scores = new double[alignmentScores.length][0];
    int[][] indexes = new int[alignmentScores.length][0];
    String[][] backPointers = new String[alignmentScores.length][0];
    char[] characters = sequence.toCharArray();

    SortedSet<Score> row = (SortedSet<Score>) alignmentScores[0];
    scores[0] = new double[row.size()];
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
      scores[i] = new double[row.size()];
      indexes[i] = new int[row.size()];
      backPointers[i] = new String[row.size()];
      int j = 0;
      for (Score s : row) {
        scores[i][j] = Double.MIN_VALUE;
        indexes[i][j] = -1;
        backPointers[i][j] = "-1:-1";
        int baseScore = configuration
            .getScore(graph.getNode(s.getIndex()).getValue(), characters[i]);
        for (int k = 0; k < i; k++) {
          for (int l = 0; l < scores[k].length; l++) {
            double score = baseScore + scores[k][l] - configuration.getGapPenalty(
                graph.getDistance(indexes[k][l], s.getIndex(), configuration.getMaxGapLength()))
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
    int colNr = ArrayUtils.findHighesIndex(scores[rowNr]);

    int[] alignment = new int[scores.length];
    while (rowNr >= 0) {
      alignment[rowNr] = indexes[rowNr][colNr];
      String backPointer = backPointers[rowNr][colNr];
      rowNr = Integer.parseInt(backPointer.split(":")[0]);
      colNr = Integer.parseInt(backPointer.split(":")[1]);
    }

    for (i = 0; i < alignment.length; i++) {
      System.out.println(i + ": " + alignment[i]);
    }
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

  private void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public int[] align(String sequence) {
    System.out.println("Aligning sequence " + sequence);
    Object[] alignmentScores = improvedFuzzyContextSearch(sequence);
    int[] alignment = findMostProbablePath(alignmentScores, sequence);

    return alignment;
  }
}
