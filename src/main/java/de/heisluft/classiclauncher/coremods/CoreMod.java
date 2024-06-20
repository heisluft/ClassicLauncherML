package de.heisluft.classiclauncher.coremods;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public interface CoreMod {

  String CALLBACK_CLASSNAME = "de/heisluft/classiclauncher/coremods/Callbacks";
  Marker MARKER = MarkerManager.getMarker("COREMOD");

  boolean processClass(ClassNode node, Type classType);

  default String name() {
    return getClass().getSimpleName();
  }

}