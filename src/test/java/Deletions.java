import org.junit.Test;

import configuration.Configuration;
import configuration.EditDistanceConfiguration;
import data.Alignment;
import data.Graph;
import index.FuzzySearchIndex;
import utils.ParseUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class Deletions {
  @Test
  public void singleDeletionNoMargin() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(5);
    String sequence = "ACGGAATAAGCA";
    String deletion = "ACGGAAAAGCA";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    Alignment alignment = index.align(deletion);
    assertTrue(0 > alignment.getScore());
  }

  @Test
  public void singleDeletion() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(4);
    configuration.setErrorMargin(1);
    String sequence = "ACGTATTAC";
    String deletion = "ACGTTTAC";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    int before = graph.getCurrentSize();
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    Alignment alignment = index.align(deletion);
    System.out.println(alignment);
    for (int i = 0; i < alignment.getAlignment().length; i++) {
      assertNotEquals(0, alignment.getAlignment()[i]);
    }
    graph.mergeSequence(deletion, alignment.getAlignment());
    assertEquals(-1.0, alignment.getScore(), 0.0);
    assertEquals(before, graph.getCurrentSize());
    assertEquals(2, graph.getNode(4).getOutgoing().size());
    assertEquals(2, graph.getNode(6).getIncoming().size());
  }

  @Test
  public void multipleSeparateDeletions() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(3);
    configuration.setErrorMargin(2);
    String sequence = "ACGGAATAAGCA";
    String deletion = "AGGAATAAGA";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    int before = graph.getCurrentSize();
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    Alignment alignment = index.align(deletion);
    System.out.println(alignment);
    assertEquals(-2.0, alignment.getScore(), 0.0);
  }

  @Test
  public void multipleContinuousDeletions() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(3);
    configuration.setErrorMargin(2);
    String sequence = "ACGGAATAAGCA";
    String deletion = "ACGGAAAGCA";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    int before = graph.getCurrentSize();
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    Alignment alignment = index.align(deletion);
    System.out.println(alignment);
    assertEquals(-2.0, alignment.getScore(), 0.0);
  }
}
