package de.heisluft.classiclauncher;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.JarContentsBuilder;
import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class UnzippedMCModuleBuilder implements ITransformationService {

  private String mcVersion;
  private boolean isNoop;

  private static final Logger LOGGER = LogManager.getLogger();
  private static final Marker MARKER = MarkerManager.getMarker("TRANSFORMERSERVICE");

  @Override
  public @NotNull String name() {
    return "hcl-unzipped-mc-module-builder";
  }

  @Override
  public void initialize(IEnvironment environment) {
    if(isNoop) return;
    mcVersion = environment.getProperty(IEnvironment.Keys.VERSION.get()).orElseThrow();
  }

  @Override
  public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
    if(MCDistType.getFromEnvironment() != MCDistType.UNZIPPED) {
      isNoop = true;
      LOGGER.info(MARKER, "Setting to noop because MC distribution is not UNZIPPED");
    }
  }

  @Override
  public @NotNull List<? extends ITransformer<?>> transformers() {
    if(isNoop) return List.of();
    return List.of();
  }

  private static SecureJar customMerged(String customName, String customVersion, JarContents contents) {
    return SecureJar.from(contents, new JarMetadata() {
      final String version = customVersion == null ? JarMetadata.from(contents).version() : customVersion;

      @Override
      public String name() {
        return customName;
      }

      @Override
      public @Nullable String version() {
        return version;
      }

      @Override
      public ModuleDescriptor descriptor() {
        return ModuleDescriptor.newAutomaticModule(name()).version(version()).packages(contents.getPackages()).build();
      }
    });
  }

  @Override
  public List<Resource> completeScan(IModuleLayerManager layerManager) {
    if(isNoop) return List.of();
    List<Path> mcCP;
    try {
      mcCP = Files.readAllLines(Path.of(Objects.requireNonNull(System.getProperty("gameClassPath.file"), "Game Classpath property is not set???"))).stream().map(Path::of).collect(Collectors.toList());
    } catch(IOException e) {
      throw new RuntimeException(e);
    }

    Path mcClassesPath = mcCP.stream()
        .filter(Files::isDirectory).findAny().orElseThrow(() -> new RuntimeException("Cannot find minecraft classes"));
    mcCP.remove(mcClassesPath);
    Path assetsJarPath = mcCP.stream()
        .filter(s -> s.getFileName().toString().startsWith("minecraft-assets-"))
        .findAny().orElseThrow(() -> new RuntimeException("Cannot find assets jar on the classpath"));
    mcCP.remove(assetsJarPath);

    Path[] paulsPaths = mcCP.stream().filter(a -> a.toString().contains("paulscode")).toArray(Path[]::new);
    for(Path paulsPath : paulsPaths) mcCP.remove(paulsPath);

    List<SecureJar> jars = new ArrayList<>();
    String mcModuleVersion = mcVersion.startsWith("inf-") ?
        mcVersion.substring(3) + "-infdev" :
        mcVersion.startsWith("in-") ?
            mcVersion.substring(3) + "-indev" :
            mcVersion.startsWith("c") ?
                mcVersion.substring(1) + "-classic" :
                mcVersion.matches("\\d") ?
                    mcVersion.substring(mcVersion.indexOf(mcVersion.chars().filter(
                                i -> i >= '0' && i <= '9'
                            ).findFirst().orElseThrow())) :
                    null;
    jars.add(customMerged("paulscode", null, new JarContentsBuilder().paths(paulsPaths).build()));
    jars.add(customMerged("minecraft", mcModuleVersion, new JarContentsBuilder().paths(mcClassesPath, assetsJarPath).build()));
    for(Path path : mcCP) jars.add(SecureJar.from(path));
    return List.of(new Resource(IModuleLayerManager.Layer.GAME, jars));
  }
}
