package de.heisluft.classiclauncher.awt;

import java.applet.AppletContext;
import java.applet.AppletStub;
import java.net.MalformedURLException;
import java.net.URL;

public class MCAppletStub implements AppletStub {

  private static final long serialVersionUID = 1L;


  public void appletResize(int width, int height) {}

  @Override
  public boolean isActive() {
    return true;
  }

  @Override
  public URL getDocumentBase() {
    try {
      return new URL("http://minecraft.net/");
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public URL getCodeBase() {
    try {
      return new URL("http://minecraft.net/");
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public String getParameter(String paramName) {
    if(paramName.equals("username")) return "User1234";
    if(paramName.equals("sessionid")) return "1234";
    if(paramName.equals("haspaid")) return "true";
    if(paramName.equals("fullscreen"))return "false";
    System.out.println("Client asked for non-existent parameter: " + paramName);
    return null;
  }

  @Override
  public AppletContext getAppletContext() {
    return null;
  }
}