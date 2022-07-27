package de.heisluft.classiclauncher;

import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;

import java.util.Arrays;

public enum MCDistType {
  UNZIPPED, OBFUSCATED, SEPARATED_ASSETS;

  private static MCDistType active;

  public static MCDistType getFromEnvironment() throws IncompatibleEnvironmentException {
    if(active != null) return active;
    String mcDistType = System.getProperty("hcl.mcDistType");
    if(mcDistType == null) throw new IncompatibleEnvironmentException("MC Distribution Type Property not set");
    try {
      return active = valueOf(mcDistType);
    } catch (IllegalArgumentException e) {
      throw new IncompatibleEnvironmentException("Invalid Distribution Type Property value: '" + mcDistType + "'. valid are only" + Arrays.toString(values()));
    }
  }
}
