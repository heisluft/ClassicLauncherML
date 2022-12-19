package de.heisluft.classiclauncher.perf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class Profiler {
  private static final Logger LOGGER = LogManager.getLogger("Profiler");
  private final int timerInterval;
  private static Profiler instance;

  private final Timer timer = new Timer("Profiler", false);
  private final SortedMap<String, StackNode> nodes = new TreeMap<>();
  private Thread timerThread;

  private class UpdateTask extends TimerTask {
    private static final ThreadMXBean THE_BEAN = ManagementFactory.getThreadMXBean();

    @Override
    public void run() {
      if(timerThread == null) timerThread = Thread.currentThread();
      long millis = System.currentTimeMillis();
      ThreadInfo[] dump = THE_BEAN.dumpAllThreads(false, false);
      for (ThreadInfo ti : dump) {
        String threadName = ti.getThreadName();
        StackTraceElement[] stack = ti.getStackTrace();
        if (threadName == null || stack == null) continue;

        acquireNode(threadName).update(stack, timerInterval);
      }
      int diff = (int) (System.currentTimeMillis() - millis);
      if(diff > timerInterval) LOGGER.warn("Profiling took longer than timer interval (took {} ms, specified {} ms)", diff, timerInterval);
    }
  }

  private Profiler(int timerInterval) {
    this.timerInterval = timerInterval;
    LOGGER.info("Starting Profiler with an interval of {}", timerInterval);
    timer.scheduleAtFixedRate(new UpdateTask(), 0, this.timerInterval);
  }

  private StackNode acquireNode(String name) {
    StackNode node = nodes.get(name);
    if (node == null) {
      node = new StackNode(name);
      nodes.put(name, node);
    }
    return node;
  }

  public static void start(int timerInterval) {
    if(instance != null) throw new IllegalStateException("Profiler already running!");
    else instance = new Profiler(timerInterval);
  }

  public static void stop() {
    if(instance == null) return;
    instance.timer.cancel();
    try {
     instance.timerThread.join();
    } catch(InterruptedException e) {
      throw new Error("Main Thread interrupted???", e);
    }
    try {
      Path outPath = Path.of("profiler/perf.json");
      Files.createDirectories(outPath.getParent());
      try(BufferedWriter writer = Files.newBufferedWriter(outPath)) {
        writer.write('{');
        for(Iterator<StackNode> iterator = instance.nodes.values().iterator(); iterator.hasNext();) {
          iterator.next().writeJSON(writer);
          if(iterator.hasNext()) writer.write(',');
        }
        writer.write('}');
      }
    } catch(IOException e) {
      LOGGER.error("Could not write Performance data!", e);
    }
  };
}
