import org.junit.Test;

import configuration.Configuration;
import configuration.EditDistanceConfiguration;
import data.Alignment;
import data.Graph;
import index.FuzzySearchIndex;
import utils.ParseUtils;

import static org.junit.Assert.assertEquals;

public class EqualSequences {
  @Test
  public void align() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(5);
    String sequence = "ACGTATTAC";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    Alignment alignment = index.align(sequence);
    assertEquals(0.0, alignment.getScore(), 0.0);
    for (int i = 0; i < alignment.getAlignment().length; i++) {
      assertEquals(i + 1, alignment.getAlignment()[i]);
    }
  }

  @Test
  public void merge() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(5);
    String sequence = "ACGTATTAC";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    int original = graph.getCurrentSize();
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    Alignment alignment = index.align(sequence);
    graph.mergeSequence(sequence, alignment.getAlignment());
    assertEquals(original, graph.getCurrentSize());
  }
}
