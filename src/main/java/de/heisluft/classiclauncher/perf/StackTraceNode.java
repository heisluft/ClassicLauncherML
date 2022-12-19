package de.heisluft.classiclauncher.perf;

class StackTraceNode extends StackNode {

  StackTraceNode(String className, String methodName) {
    super(className + "." + methodName + "()");
  }

  @Override
  public int compareTo(StackNode o) {
    return Long.compare(o.totalTime, totalTime);
  }
}
