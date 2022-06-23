package de.heisluft.classiclauncher;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cpw.mods.modlauncher.api.LamdbaExceptionUtils.*;

public class TransformationService implements ITransformationService {
  @Override
  public @NotNull String name() {
    return "test";
  }

  @Override
  public void initialize(IEnvironment environment) {}

  @Override
  public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {}

  @Override
  public List<Resource> completeScan(IModuleLayerManager layerManager) {
    Path p = Path.of("build/classes/java/main/");
    Set<String> dirs = new HashSet<>();
    try {
      Files.walk(p).filter(Files::isDirectory).filter(d -> {
        try {
          return Files.walk(d, 1).anyMatch(Files::isRegularFile);
        } catch(IOException e) {
          throw new UncheckedIOException(e);
        }}).map(p::relativize).map(Path::toString).map(s -> s.replace(File.separatorChar, '.')).forEach(dirs::add);
    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
    var mc = SecureJar.from(jar-> new JarMetadata() {
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
        ModuleDescriptor.Builder b = ModuleDescriptor.newAutomaticModule(name()).packages(dirs);
        return b.build();
      }
    },p);
    return List.of(new Resource(IModuleLayerManager.Layer.GAME, List.of(mc)));
  }

  @Override
  public @NotNull List<ITransformer> transformers() {
    return new ArrayList<>();
  }
}
