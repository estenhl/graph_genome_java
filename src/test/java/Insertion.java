import org.junit.Test;

import configuration.Configuration;
import configuration.EditDistanceConfiguration;
import data.Graph;
import index.FuzzySearchIndex;
import utils.ParseUtils;

import static org.junit.Assert.assertEquals;

public class Insertion {
  @Test
  public void singleInsertion() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(5);
    configuration.setErrorMargin(1);
    Graph graph = ParseUtils.stringToGraph(configuration, "ACGTATTAC");
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    int before = graph.getCurrentSize();
    String sequence = "ACGTAATTAC";
    graph.mergeSequence(sequence, index.align(sequence).getAlignment());
    assertEquals(before + 1, graph.getCurrentSize());
  }
}
