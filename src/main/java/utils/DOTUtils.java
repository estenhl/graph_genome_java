package utils;

import data.Node;

public class DOTUtils {
  public static final String ALIGNMENT_COLOR = "red";
  public static final String STANDARD_COLOR = "black";

  public static String getNodeRepresentation(Node n) {
    return getNodeRepresentation(Integer.toString(n.getIndex()), n.getValue());
  }

  public static String getNodeRepresentation(String index, char value) {
    return index + " [label=\"" + value + "\", xlabel=\"" + index + "\"];\n";
  }

  public static String getEdgeRepresentation(String src, String dest) {
    return getEdgeRepresentation(src, dest, STANDARD_COLOR);
  }

  public static String getEdgeRepresentation(String src, String dest, String color) {
    return src + " -> " + dest + "[color=" + color + "];\n";
  }
}
