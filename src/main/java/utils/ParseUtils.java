package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import configuration.Configuration;
import data.Graph;
import data.Node;

public class ParseUtils {
  public static Graph stringToGraph(Configuration configuration, String s) {
    System.out.println("Creating graph from string " + s);
    Graph graph = new Graph(configuration, GraphUtils.getGraphSize(s.length() * 2));
    Node prev = graph.getHead();
    char[] chars = s.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      Node curr = new Node(chars[i]);
      int index = graph.addNode(curr);
      prev.addOutgoing(index);
      curr.addIncoming(prev.getIndex());
      prev = curr;
    }
    Node tail = graph.getTail();
    tail.addIncoming(prev.getIndex());
    prev.addOutgoing(tail.getIndex());

    return graph;
  }

  public static String fastaToSequence(String fileName) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
    String header = reader.readLine();
    String sequence = "";
    String line;
    int read = 0;
    while ((line = reader.readLine()) != null) {
      read++;
      if (read % 1000 == 0) {
        System.out.println("Read " + read + " lines");
      }
      for (Character c : line.trim().toCharArray()) {
        sequence += Character.toUpperCase(c);
      }
    }
    reader.close();

    return sequence;
  }

  public static Graph fastaToGraph(Configuration configuration, String fileName)
      throws IOException {
    System.out.println("Reading file " + fileName);
    return stringToGraph(configuration, fastaToSequence(fileName));
  }
}
