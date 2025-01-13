package de.heisluft.classiclauncher.awt;

import de.heisluft.classiclauncher.LaunchHandlerService;
import org.apache.logging.log4j.LogManager;

import java.awt.Container;
import java.net.URI;
import java.net.URL;

/**
 * A class providing all Applet methods minecraft accesses.
 * As Applet is marked for removal since Java 17 we have to substitute it.
 */
public abstract class AppletFake extends Container {
  /** the spoofed url */
  private static final URL url;

  static {
    URL t = null;
    try {
      t = new URI("http://localhost/").toURL();
    } catch(Exception ex) {
      //will never happen
      ex.printStackTrace();
    }
    url = t;
  }

  /** Proxy Method for LaunchHandlerService */
  public abstract void init();
  /** Proxy Method for LaunchHandlerService */
  public abstract void start();

  /** Proxy Method for the Applet, implementation allows us to not use AppletStub */
  public String getParameter(String name) {
    if(LaunchHandlerService.APPLET_PARAMS.containsKey(name)) return LaunchHandlerService.APPLET_PARAMS.get(name);
    LogManager.getLogger(getClass()).warn("Client asked for nonexistent parameter: " + name);
    return null;
  }

  /** Proxy Method for the Applet, implementation allows us to not use AppletStub */
  public URL getCodeBase() {
    return url;
  }

  /** Proxy Method for the Applet, implementation allows us to not use AppletStub */
  public URL getDocumentBase() {
    return url;
  }
}
