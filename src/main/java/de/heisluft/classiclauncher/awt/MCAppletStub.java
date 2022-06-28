package de.heisluft.classiclauncher.awt;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.applet.AppletContext;
import java.applet.AppletStub;
import java.net.MalformedURLException;
import java.net.URL;

import static de.heisluft.classiclauncher.LaunchHandlerService.*;

public class MCAppletStub implements AppletStub {

  private static final long serialVersionUID = 1L;
  private static final Marker MARKER = MarkerManager.getMarker("APPLET");

  public void appletResize(int width, int height) {}

  @Override
  public boolean isActive() {
    return true;
  }

  @Override
  public URL getDocumentBase() {
    try {
      return new URL("http://localhost/");
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public URL getCodeBase() {
    try {
      return new URL("http://localhost/");
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public String getParameter(String paramName) {
    if(APPLET_PARAMS.containsKey(paramName)) return APPLET_PARAMS.get(paramName);
    LOGGER.warn(MARKER, "Client asked for non-existent parameter: " + paramName);
    return null;
  }

  @Override
  public AppletContext getAppletContext() {
    return null;
  }
}