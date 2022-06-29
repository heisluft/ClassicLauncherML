package de.heisluft.classiclauncher.coremods;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public interface CoreMod {
  Logger LOGGER = LogManager.getLogger("ClassicFixer");
  String CALLBACK_CLASSNAME = "de/heisluft/classiclauncher/callback/Callbacks";

  boolean processClass(ILaunchPluginService.Phase phase, ClassNode node, Type classType);

  default String name() {
    return getClass().getSimpleName();
  }
}
