import org.junit.Test;

import data.Graph;
import data.Node;
import utils.GraphUtils;
import utils.ParseUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UtilTest {
  @Test
  public void getSize() {
    assertEquals(8, GraphUtils.getGraphSize(5));
    assertEquals(64, GraphUtils.getGraphSize(47));
  }

  @Test
  public void doubleNodeArr() {
    Node[] nodes = new Node[8];
    for (int i = 0; i < nodes.length; i++) {
      nodes[i] = new Node(Integer.toString(i).charAt(0));
    }

    nodes = GraphUtils.doubleNodeArray(nodes);
    assertNotNull(nodes);
    assertEquals(16, nodes.length);
    assertEquals('0', nodes[0].getValue());
    assertEquals('1', nodes[1].getValue());
    assertNull(nodes[7]);
    assertNull(nodes[8]);
    assertNotNull(nodes[15]);
    assertEquals('7', nodes[15].getValue());
  }

  @Test
  public void parseString() {
    Graph graph = ParseUtils.stringToGraph("ACTGGCT");
    assertNotNull(graph);
    assertEquals(8, graph.getCurrentSize());
    Node first = graph.getNode(1);
    assertNotNull(first);
    assertEquals('A', first.getValue());
    assertEquals(1, first.getIndex());
    assertEquals(1, first.getIncoming().size());
    assertTrue(first.getIncoming().contains(0));
    assertEquals(1, first.getOutgoing().size());
    assertTrue(first.getOutgoing().contains(2));
  }
}
