package de.heisluft.classiclauncher.coremods;

import de.heisluft.classiclauncher.LaunchHandlerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;


public class Callbacks {

  private static final Logger LOGGER = LogManager.getLogger("AssetReflux");
  private static final Marker MARKER = MarkerManager.getMarker("CALLBACK");

  private static Method addMusicMethod, addSoundMethod;
  private static boolean newSoundSystem;
  private static Object soundManager;

  private static void addMusic(Path path, String name) {
    try {
      addMusicMethod.invoke(soundManager, name, path.toFile());
    } catch (IllegalAccessException | InvocationTargetException e) {
      LOGGER.error(MARKER, "Error invoking addMusic!", e);
    }
  }

  private static void addSound(Path path, String name) {
    try {
      if(newSoundSystem) addSoundMethod.invoke(soundManager, name, path.toFile());
      else addSoundMethod.invoke(soundManager, path.toFile(), name);
    } catch (IllegalAccessException | InvocationTargetException e) {
      LOGGER.error(MARKER, "Error invoking addSound!", e);
    }
  }
  public static void callback(Object mc, String soundManagerFName, String addSoundMName, String addMusicMName, boolean newSoundSystem) throws Exception {
    soundManager = mc.getClass().getField(soundManagerFName).get(mc);
    Class<?> smc = soundManager.getClass();
    Callbacks.newSoundSystem = newSoundSystem;
    addMusicMethod = smc.getMethod(addMusicMName, String.class, File.class);
    addSoundMethod = newSoundSystem ? smc.getMethod(addSoundMName, String.class, File.class) : smc.getMethod(addSoundMName, File.class, String.class);
    LOGGER.info(MARKER, "Setup Finished, downloading assets");
    run();
  }

  public static void run() throws IOException, ParseException {
    URL assetsIndex = new URL("https://launchermeta.mojang.com/v1/packages/4759bad2824e419da9db32861fcdc3a274336532/pre-1.6.json");
    InputStream is = assetsIndex.openStream();
    JSONObject object = (JSONObject) new JSONParser().parse(new InputStreamReader(is));
    is.close();
    //http://resources.download.minecraft.net/<first 2 hex letters of hash>/<whole hash>
    JSONObject objects = object.getObject("objects");
    for(Map.Entry<String, ?> e : objects.entrySet()) {
      String resName = e.getKey();
      if(resName.contains("/")) Files.createDirectories(LaunchHandlerService.assetsDir.resolve(resName.substring(0, resName.lastIndexOf('/'))));
      String hash = ((JSONObject) e.getValue()).getString("hash");
      Path outFile = LaunchHandlerService.assetsDir.resolve(resName);
      byte[] buf = new byte[4096];
      InputStream is2 = new URL("https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash).openStream();
      OutputStream fos = Files.newOutputStream(outFile);
      int read;
      while((read = is2.read(buf)) != -1) fos.write(buf, 0, read);
      is2.close();
      fos.close();
      if(resName.startsWith("music/")) addMusic(outFile, resName.substring(resName.indexOf('/') + 1));
      if(resName.startsWith("sound/")) addSound(outFile, resName.substring(resName.indexOf('/') + 1));
    }
    LOGGER.info(MARKER, "Successfully loaded in all assets!");
  }

  public static int compare(float dist1, float dist2, boolean v1, boolean v2, boolean invert) {
    if(invert) {
      if(v1 && !v2) return 1;
      if(v2 && !v1) return -1;
      return Float.compare(dist2, dist1);
    }
    if(v1 && !v2) return -1;
    if(v2 && !v1) return 1;
    return Float.compare(dist1, dist2);
  }

  public static File assetsDir() {
    return LaunchHandlerService.assetsDir.toFile();
  }

  public static File gameDir() {
    return LaunchHandlerService.gameDir.toFile();
  }
}
