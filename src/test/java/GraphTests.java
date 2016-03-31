import org.junit.Test;

import configuration.Configuration;
import configuration.EditDistanceConfiguration;
import data.Graph;
import utils.ParseUtils;

import static org.junit.Assert.assertEquals;

public class GraphTests {
  @Test
  public void buildFromString() {
    Configuration configuration = new EditDistanceConfiguration();
    String sequence = "ACGTTTCACATGG";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    assertEquals(sequence.length() + 1, graph.getCurrentSize());
    for (int i = 0; i < sequence.length(); i++) {
      assertEquals(sequence.charAt(i), graph.getNode(i + 1).getValue());
      assertEquals(1, graph.getNode(i + 1).getOutgoing().size());
    }
  }

  @Test
  public void buildFromFasta() {

  }

  @Test
  public void insertVcf() {

  }
}
