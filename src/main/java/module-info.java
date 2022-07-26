module de.heisluft.classiclauncher {
  requires java.base;
  requires java.desktop;
  requires cpw.mods.modlauncher;
  requires cpw.mods.securejarhandler;
  requires jopt.simple;
  requires json.simple;
  requires static org.jetbrains.annotations;
  requires java.logging;
  requires org.apache.logging.log4j;
  requires org.objectweb.asm.tree;
  provides cpw.mods.modlauncher.api.ILaunchHandlerService with de.heisluft.classiclauncher.LaunchHandlerService;
  provides cpw.mods.modlauncher.api.ITransformationService with de.heisluft.classiclauncher.UnzippedMCModuleBuilder;
  provides cpw.mods.modlauncher.serviceapi.ILaunchPluginService with de.heisluft.classiclauncher.coremods.CoreModManager;
  exports de.heisluft.classiclauncher.callback;
  exports de.heisluft.classiclauncher.jul;
}