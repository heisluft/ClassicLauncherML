package de.heisluft.classiclauncher;

import cpw.mods.jarhandling.JarContentsBuilder;
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
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UnzippedMCModuleBuilder implements ITransformationService {

  private String mcVersion;
  private Path mcClassesPath;
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
      return;
    }
    String mcClassDir = System.getProperty("mccl.mcClasses.dir");
    if(mcClassDir == null)
      throw new IncompatibleEnvironmentException("MC Classes Dir is not set - we wont be able to load in minecraft as a module");
    if(!Files.isDirectory(mcClassesPath = Path.of(mcClassDir)))
      throw new IncompatibleEnvironmentException("MC Classes Property does not point to an existing Directory");
    LOGGER.info(MARKER, "Setting MC classes path to {}", mcClassesPath);
  }

  @Override
  public @NotNull List<? extends ITransformer<?>> transformers() {
    if(isNoop) return List.of();
    return List.of();
  }

  private static Set<String> getPackages(Path p) throws IOException {
    try {
      return Files.walk(p).filter(Predicate.not(p::equals)).filter(Files::isDirectory).filter(p2 -> {
        try {
          return Files.walk(p2, 1).anyMatch(Files::isRegularFile);
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      }).map(p::relativize).map(Path::toString).map(s -> s.replace('\\', '.').replace('/', '.')).collect(Collectors.toSet());
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  @Override
  public List<Resource> completeScan(IModuleLayerManager layerManager) {
    if(isNoop) return List.of();
    String legacyCP = Objects.requireNonNull(System.getProperty("legacyClassPath"), "legacy Classpath property is not set???");
    Path assetsJarPath = Arrays.stream(legacyCP.split(File.pathSeparator))
        .filter(s -> s.substring(s.lastIndexOf(File.separatorChar) + 1).startsWith("minecraft-assets-"))
        .map(Path::of).findAny().orElseThrow(() -> new RuntimeException("Cannot find assets jar on the classpath"));
    return List.of(new Resource(IModuleLayerManager.Layer.GAME, List.of(SecureJar.from(new JarContentsBuilder().paths(mcClassesPath, assetsJarPath).build(), new JarMetadata() {
      private final Set<String> packages;

      {
        try {
          packages = getPackages(mcClassesPath);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }

      @Override
      public String name() {
        return "minecraft";
      }

      @Override
      public String version() {
        return mcVersion;
      }

      @Override
      public ModuleDescriptor descriptor() {
        return ModuleDescriptor.newAutomaticModule(name()).packages(packages).build();
      }}))));
  }
}
