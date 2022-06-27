package de.heisluft.classiclauncher;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class TransformationService implements ITransformationService {

  private static final Logger LOGGER = LogManager.getLogger("ClassicLauncher");
  private static final Marker MARKER = MarkerManager.getMarker("TRANSFORMATION_SERVICE");

  @Override
  public @NotNull String name() {
    return "test";
  }

  @Override
  public void initialize(IEnvironment environment) {}

  @Override
  public void onLoad(IEnvironment env, Set<String> otherServices) {
    // TODO: Check why this is not working
    try(InputStream is = getClass().getClassLoader().getResourceAsStream("logging.properties")) {
      java.util.logging.LogManager.getLogManager().readConfiguration(is);
    } catch(IOException e) {
      LOGGER.warn(MARKER, "Could not enable JUL Bridge", e);
    }
  }

  private Set<String> getPkgs(Path p) {
    try {
      return Files.walk(p).filter(Files::isDirectory).filter(d -> {
        try {
          return !p.equals(d) && Files.walk(d, 1).anyMatch(Files::isRegularFile);
        } catch(IOException e) {
          throw new UncheckedIOException(e);
        }}).map(p::relativize).map(Path::toString).map(s -> s.replace(p.getFileSystem().getSeparator(), ".")).collect(Collectors.toSet());
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private record MCJarMetadata(Set<String> pkgs) implements JarMetadata {
    @Override
    public String name() {
      return "minecraft";
    }

    @Override
    public String version() {
      return "classic";
    }

    @Override
    public ModuleDescriptor descriptor() {
      return ModuleDescriptor.newAutomaticModule(name()).packages(pkgs).build();
    }
  }

  @Override
  public List<Resource> completeScan(IModuleLayerManager layerManager) {
    String[] ignores = System.getProperty("ignoreList", "").split(",");
    List<SecureJar> jars = new ArrayList<>();
    for (String path : System.getProperty("legacyClassPath", "").split(File.pathSeparator)) {
      String fname = path.substring(path.lastIndexOf(File.separatorChar) + 1);
      if(fname.equals("main") || Arrays.stream(ignores).anyMatch(fname::startsWith)) continue;
      jars.add(SecureJar.from(Path.of(path)));
    }
    Path mcPath = Path.of("build/classes/java/main/");
    jars.add(SecureJar.from(jar -> new MCJarMetadata(getPkgs(mcPath)), mcPath));
    return List.of(new Resource(IModuleLayerManager.Layer.GAME, jars));
  }

  @Override
  public @NotNull List<ITransformer> transformers() {
    return new ArrayList<>();
  }
}
