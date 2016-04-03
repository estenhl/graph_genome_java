import org.junit.Test;

import configuration.Configuration;
import configuration.EditDistanceConfiguration;
import data.Alignment;
import data.Graph;
import index.FuzzySearchIndex;
import utils.ParseUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SNPs {
  @Test
  public void singleSNPnoMargin() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(5);
    String sequence = "ACGGAATAAGCA";
    String SNP = "ACGGAAGAAGCA";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    int before = graph.getCurrentSize();
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    Alignment alignment = index.align(SNP);
    assertTrue(0 > alignment.getScore());
    for (int i = 0; i < alignment.getAlignment().length; i++) {
      assertEquals(0, alignment.getAlignment()[i]);
    }

    graph.mergeSequence(sequence, alignment.getAlignment());
    assertEquals(before + SNP.length(), graph.getCurrentSize());
  }

  @Test
  public void singleSNPmiddle() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(5);
    configuration.setErrorMargin(1);
    String sequence = "ACGGAATAAGCA";
    String SNP = "ACGGAAGAAGCA";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    int before = graph.getCurrentSize();
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    Alignment alignment = index.align(SNP);
    assertEquals(-1.0, alignment.getScore(), 0.0);

    graph.mergeSequence(SNP, alignment.getAlignment());
    assertEquals(before + 1, graph.getCurrentSize());
    assertEquals(2, graph.getNode(6).getOutgoing().size());
    assertEquals(2, graph.getNode(8).getIncoming().size());
  }

  @Test
  public void singleSNPstart() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(5);
    configuration.setErrorMargin(1);
    String sequence = "ACGGAATAAGCA";
    String SNP = "CCGGAATAAGCA";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    int before = graph.getCurrentSize();
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    Alignment alignment = index.align(SNP);
    assertEquals(-1.0, alignment.getScore(), 0.0);

    graph.mergeSequence(SNP, alignment.getAlignment());
    assertEquals(before + 1, graph.getCurrentSize());
  }

  @Test
  public void singleSNPend() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(5);
    configuration.setErrorMargin(1);
    String sequence = "ACGGAATAAGCA";
    String SNP = "ACGGAATAAGCT";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    int before = graph.getCurrentSize();
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    Alignment alignment = index.align(SNP);
    assertEquals(-1.0, alignment.getScore(), 0.0);

    graph.mergeSequence(SNP, alignment.getAlignment());
    assertEquals(before + 1, graph.getCurrentSize());
  }

  @Test
  public void multipleSeparateSNPs() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(5);
    configuration.setErrorMargin(2);
    String sequence = "ACGGAATAAGCA";
    String SNP = "ACTGAATAACCA";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    int before = graph.getCurrentSize();
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    Alignment alignment = index.align(SNP);
    assertEquals(-2.0, alignment.getScore(), 0.0);

    graph.mergeSequence(SNP, alignment.getAlignment());
    assertEquals(before + 2, graph.getCurrentSize());
  }

  @Test
  public void multipleContinuousSNPs() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(5);
    configuration.setErrorMargin(2);
    String sequence = "ACGGAATAAGCA";
    String SNP = "ACGGTTTAAGCA";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    int before = graph.getCurrentSize();
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    Alignment alignment = index.align(SNP);
    assertEquals(-2.0, alignment.getScore(), 0.0);

    graph.mergeSequence(SNP, alignment.getAlignment());
    assertEquals(before + 2, graph.getCurrentSize());
  }

  @Test
  public void multiwayBranching() {
    Configuration configuration = new EditDistanceConfiguration();
    configuration.setContextLength(5);
    configuration.setErrorMargin(1);
    String sequence = "ACGGAATAAGCA";
    String SNP1 = "ACGGAAGAAGCA";
    String SNP2 = "ACGGAACAAGCA";
    Graph graph = ParseUtils.stringToGraph(configuration, sequence);
    int before = graph.getCurrentSize();
    FuzzySearchIndex index = FuzzySearchIndex.buildIndex(graph, configuration);
    Alignment alignment1 = index.align(SNP1);
    Alignment alignment2 = index.align(SNP2);

    graph.mergeSequence(SNP1, alignment1.getAlignment());
    graph.mergeSequence(SNP2, alignment2.getAlignment());
    assertEquals(before + 2, graph.getCurrentSize());
    assertEquals(3, graph.getNode(6).getOutgoing().size());
    assertEquals(3, graph.getNode(8).getIncoming().size());
  }
}
