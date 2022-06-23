package de.heisluft.classiclauncher;

import cpw.mods.modlauncher.api.ILaunchHandlerService;
import cpw.mods.modlauncher.api.ITransformingClassLoaderBuilder;
import cpw.mods.modlauncher.api.ServiceRunner;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Paths;

import de.heisluft.classiclauncher.awt.MCAppletStub;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LaunchHandlerService implements ILaunchHandlerService {

  private final Logger logger = LogManager.getLogger("ClassicLauncher");

  @Override
  public String name() {
    return "classiclauncher";
  }

  @Override
  public void configureTransformationClassLoader(ITransformingClassLoaderBuilder builder) {

  }

  @Override
  public ServiceRunner launchService(String[] arguments, ModuleLayer gameLayer) {

    return () -> {
      String libspath = Paths.get("build/natives").toAbsolutePath().toString();
      System.setProperty("org.lwjgl.librarypath", libspath);
      System.setProperty("net.java.games.input.librarypath", libspath);
      final Frame launcherFrameFake = new Frame();
      launcherFrameFake.setTitle("Minecraft");
      launcherFrameFake.setBackground(Color.BLACK);
      launcherFrameFake.setSize(1280, 720);

      launcherFrameFake.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          System.exit(1);
        }
      });

      logger.info("Starting minecraft");
      Module mcModule = gameLayer.findModule("minecraft").orElseThrow();
      Class<?> clazz = Class.forName(mcModule, "net.minecraft.client.MinecraftApplet");
      if(clazz == null) {
        clazz = Class.forName(mcModule, "com.mojang.minecraft.MinecraftApplet");
      }
      System.out.println(clazz.getClassLoader());

      Applet applet = (Applet) clazz.getConstructor().newInstance();
      applet.setPreferredSize(launcherFrameFake.getSize());
      applet.setStub(new MCAppletStub());
      launcherFrameFake.add(applet);
      launcherFrameFake.pack();
      launcherFrameFake.validate();


      launcherFrameFake.setLocationRelativeTo(null);
      launcherFrameFake.setVisible(true);

      launcherFrameFake.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
          applet.setSize(e.getComponent().getSize());
        }
      });

      applet.init();
      applet.start();
    };
  }
}
