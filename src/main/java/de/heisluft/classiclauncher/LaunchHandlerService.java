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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import joptsimple.OptionParser;

public class LaunchHandlerService implements ILaunchHandlerService {

  public static final Logger LOGGER = LogManager.getLogger("ClassicLauncher");
  public static final Map<String, String> APPLET_PARAMS = new HashMap<>();
  private static final Marker MARKER = MarkerManager.getMarker("SERVICE");
  public static Path gameDir, assetsDir;

  public static final Map<String, ?> PROP_MAP = Map.of("create", "true");

  @Override
  public String name() {
    return "classiclauncher";
  }

  @Override
  public void configureTransformationClassLoader(ITransformingClassLoaderBuilder builder) {}

  @Override
  public ServiceRunner launchService(String[] arguments, ModuleLayer gameLayer) {
    OptionParser optionParser = new OptionParser();
    optionParser.accepts("version").withRequiredArg();
    OptionSpec<Path> gameDirOption = optionParser.accepts("gameDir").withRequiredArg().withValuesConvertedBy(new PathConverter()).defaultsTo(Path.of("."));
    OptionSpec<Path> assetsDirOption = optionParser.accepts("assetsDir").withRequiredArg().withValuesConvertedBy(new PathConverter());
    OptionSet os = optionParser.parse(arguments);

    List<?> s = os.nonOptionArguments();
    for(int i = 0; i < s.size() / 2; i++) APPLET_PARAMS.put(s.get(i*2).toString(), s.get(i*2+1).toString());
    gameDir = gameDirOption.value(os);
    assetsDir = os.valueOf(assetsDirOption);
    if(assetsDir == null) assetsDir = gameDir.resolve("resources");
    return () -> {
      Path libsDir = Path.of("libs");
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

      final Frame launcherFrameFake = new Frame();
      launcherFrameFake.setTitle("Minecraft");
      launcherFrameFake.setBackground(Color.BLACK);
      launcherFrameFake.setSize(1280, 720);
      launcherFrameFake.setResizable(false);

      launcherFrameFake.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          System.exit(1);
        }
      });

      LOGGER.info(MARKER, "Starting minecraft");
      Module mcModule = gameLayer.findModule("minecraft").orElseThrow();
      Class<?> clazz = Class.forName(mcModule, "net.minecraft.client.MinecraftApplet");
      if(clazz == null) {
        clazz = Class.forName(mcModule, "com.mojang.minecraft.MinecraftApplet");
      }

      Applet applet = (Applet) clazz.getConstructor().newInstance();
      applet.setPreferredSize(launcherFrameFake.getSize());
      applet.setStub(new MCAppletStub());
      launcherFrameFake.add(applet);
      launcherFrameFake.pack();
      launcherFrameFake.validate();


      launcherFrameFake.setLocationRelativeTo(null);
      launcherFrameFake.setVisible(true);

      applet.init();
      applet.start();
    };
  }
}
