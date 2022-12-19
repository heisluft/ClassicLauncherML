package de.heisluft.classiclauncher.perf;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class StackNode implements Comparable<StackNode> {

  final String name;
  private final Map<String, StackNode> children = new HashMap<>();
  long totalTime;

  StackNode(String name) {
    this.name = name;
  }

  private Collection<StackNode> getChildren() {
    List<StackNode> list = new ArrayList<>(children.values());
    Collections.sort(list);
    return list;
  }

  private StackNode acquireChild(String className, String methodName) {
    StackTraceNode node = new StackTraceNode(className, methodName);
    StackNode child = children.get(node.name);
    if (child == null) {
      child = node;
      children.put(node.name, node);
    }
    return child;
  }

  private void update(StackTraceElement[] elements, int skip, long time) {
    totalTime += time;
    if (elements.length - skip == 0) return;
    StackTraceElement bottom = elements[elements.length - (skip + 1)];
    acquireChild(bottom.getClassName(), bottom.getMethodName()).update(elements, skip + 1, time);
  }

  void update(StackTraceElement[] elements, long time) {
    update(elements, 0, time);
  }

  @Override
  public int compareTo(StackNode o) {
    return name.compareTo(o.name);
  }

  void writeJSON(Writer out) throws IOException {
    writeJSON(out, totalTime);
  }

  private void writeJSON(Writer out, long totalTime) throws IOException {
    out.write("\"");
    out.write(name.replace("\\", "\\\\").replace("\"", "\\\""));
    out.write("\":{\"timeMs\":");
    out.write(Long.toString(this.totalTime));
    out.write(",\"percent\":");
    out.write(String.format("%.2f", this.totalTime / (double) totalTime * 100));
    out.write(",\"children\":{");
    for(Iterator<StackNode> it = getChildren().iterator(); it.hasNext();) {
      it.next().writeJSON(out, totalTime);
      if(it.hasNext()) out.write(',');
    }
    out.write("}}");
  }

  private void writeString(StringBuilder builder, int indent) {
    String padding = " ".repeat(Math.max(0, indent));

    for (StackNode child : getChildren()) {
      builder.append(padding).append(child.name).append(" ").append(totalTime).append("ms\n");
      child.writeString(builder, indent + 1);
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    writeString(builder, 0);
    return builder.toString();
  }
}
