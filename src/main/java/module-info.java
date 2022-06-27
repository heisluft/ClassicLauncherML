import cpw.mods.modlauncher.api.ILaunchHandlerService;
import cpw.mods.modlauncher.api.ITransformationService;
import de.heisluft.classiclauncher.LaunchHandlerService;
import de.heisluft.classiclauncher.TransformationService;

module de.heisluft.classiclauncher {
  requires cpw.mods.modlauncher;
  requires java.desktop;
  requires org.apache.logging.log4j;
  requires cpw.mods.securejarhandler;
  requires static org.jetbrains.annotations;
  requires java.logging;
  provides ILaunchHandlerService with LaunchHandlerService;
  provides ITransformationService with TransformationService;
}