package de.heisluft.classiclauncher;

import cpw.mods.modlauncher.api.ILaunchHandlerService;
import cpw.mods.modlauncher.api.ITransformingClassLoaderBuilder;
import cpw.mods.modlauncher.api.ServiceRunner;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import de.heisluft.classiclauncher.awt.MCAppletStub;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import joptsimple.OptionParser;
import org.apache.logging.log4j.jul.Log4jBridgeHandler;

public class LaunchHandlerService implements ILaunchHandlerService {

  public static final Logger LOGGER = LogManager.getLogger("ClassicLauncher");
  public static final Map<String, String> APPLET_PARAMS = new HashMap<>();
  private static final Marker MARKER = MarkerManager.getMarker("SERVICE");
  public static Path gameDir, assetsDir;


  static {
    Log4jBridgeHandler.install(true, "", false);
  }

  @Override
  public String name() {
    return "classiclauncher";
  }

  @Override
  public void configureTransformationClassLoader(ITransformingClassLoaderBuilder builder) {}

  @Override
  public ServiceRunner launchService(String[] arguments, ModuleLayer gameLayer) {
    OptionParser parser = new OptionParser();
    OptionSpec<String> versionSpec = parser.accepts("version").withRequiredArg().defaultsTo("N/A");
    OptionSpec<Path> gameDirSpec = parser.accepts("gameDir").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.DIRECTORY_EXISTING)).defaultsTo(Path.of("."));
    OptionSpec<Path> assetsDirSpec = parser.accepts("assetsDir").withRequiredArg().withValuesConvertedBy(new PathConverter());
    OptionSet optionSet = parser.parse(arguments);
    String gameVersion = optionSet.valueOf(versionSpec);
    gameDir = optionSet.valueOf(gameDirSpec);
    assetsDir = optionSet.has(assetsDirSpec) ? optionSet.valueOf(assetsDirSpec) : gameDir.resolve("resources");
    List<?> s = optionSet.nonOptionArguments();
    for(int i = 0; i < s.size() / 2; i++) APPLET_PARAMS.put(s.get(i*2).toString(), s.get(i*2+1).toString());

    return () -> {
      Path libsDir = gameDir.resolve("libs");
      LOGGER.info(MARKER, "Extracting libraries to " + libsDir.toAbsolutePath());
      if(!Files.isDirectory(libsDir)) Files.createDirectories(libsDir);
      List<Path> children = Files.walk(libsDir).filter(Predicate.not(libsDir::equals)).toList();
      String osName = System.getProperty("os.name").toLowerCase();
      Natives natives = osName.contains("win") ? Natives.WIN : osName.contains("mac") ? Natives.MAC : Natives.LINUX;
      if(children.size() < natives.fileCount){
        for(URL url : natives.getURLs()) {
          String urlString = url.toString();
          Files.write(libsDir.resolve(urlString.substring(urlString.lastIndexOf('/') + 1)), url.openStream().readAllBytes());
        }
      }
      String libspath = libsDir.toAbsolutePath().toString();
      System.setProperty("org.lwjgl.librarypath", libspath);
      System.setProperty("net.java.games.input.librarypath", libspath);

      Frame frame = new Frame("minecraft " + gameVersion);
      frame.setSize(1280, 720);
      frame.setResizable(false);
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          System.exit(1);
        }
      });

      LOGGER.info(MARKER, "Starting minecraft");
      Module mcModule = gameLayer.findModule("minecraft").orElseThrow();
      Class<?> appletClass = Class.forName(mcModule, "net.minecraft.client.MinecraftApplet");
      if(appletClass == null) {
        appletClass = Class.forName(mcModule, "com.mojang.minecraft.MinecraftApplet");
      }

      Applet applet = (Applet) appletClass.getConstructor().newInstance();
      applet.setStub(new MCAppletStub());
      applet.setPreferredSize(frame.getSize());

      frame.add(applet);
      frame.pack();
      frame.setVisible(true);

      applet.init();
      applet.start();
    };
  }
}
