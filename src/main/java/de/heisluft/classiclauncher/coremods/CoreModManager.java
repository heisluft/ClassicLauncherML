package de.heisluft.classiclauncher.coremods;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class CoreModManager implements ILaunchPluginService {
  private final Set<CoreMod> coreMods = new HashSet<>();
  private static final Logger LOGGER = LogManager.getLogger();
  private static final Marker MARKER = MarkerManager.getMarker("LAUNCHPLUGIN");

  public CoreModManager() {
    coreMods.add(new ComparatorFixer());
    coreMods.add(new URLTransformer());
    coreMods.add(new AssetReflux());
    coreMods.add(new GameDirChanger());
    coreMods.add(new AuthCheckBypass());
    coreMods.add(new PrintTransformer());
    String postMods = coreMods.stream().map(CoreMod::name).collect(Collectors.joining(", ", "[", "]"));
    LOGGER.info(MARKER, "Launching with the following coremods: {}", postMods);
  }

  @Override
  public String name() {
    return "coremodmanager";
  }

  @Override
  public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
    return EnumSet.of(Phase.AFTER);
  }

  @Override
  public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
    boolean dirty = false;
    for(CoreMod c : coreMods) {
      dirty |= c.processClass(classNode, classType);
    }
    return dirty;
  }
}
