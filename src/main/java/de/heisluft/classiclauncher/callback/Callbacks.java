package de.heisluft.classiclauncher.callback;

import de.heisluft.classiclauncher.LaunchHandlerService;

import java.io.File;
import java.io.InputStream;

public class Callbacks {

  //TODO: Check if we have an alternative, classloader leakage should be avoided
  public static InputStream getResourceAsStream(Class<?> caller, String resName) {
    return Callbacks.class.getClassLoader().getResourceAsStream(resName.substring(1));
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
