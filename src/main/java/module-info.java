import cpw.mods.modlauncher.api.ILaunchHandlerService;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import de.heisluft.classiclauncher.LaunchHandlerService;
import de.heisluft.classiclauncher.coremods.CoreModManager;
import de.heisluft.classiclauncher.GameLayerBuilder;

module de.heisluft.classiclauncher {
  requires cpw.mods.modlauncher;
  requires java.desktop;
  requires java.base;
  requires org.apache.logging.log4j;
  requires cpw.mods.securejarhandler;
  requires static org.jetbrains.annotations;
  requires java.logging;
  requires org.objectweb.asm.tree;
  requires jopt.simple;
  provides ILaunchHandlerService with LaunchHandlerService;
  provides ITransformationService with GameLayerBuilder;
  provides ILaunchPluginService with CoreModManager;
  exports de.heisluft.classiclauncher.callback;
  exports de.heisluft.classiclauncher.jul;
}