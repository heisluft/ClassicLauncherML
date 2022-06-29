package de.heisluft.classiclauncher.coremods;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CoreModManager implements ILaunchPluginService {

  private final Map<Phase, Set<CoreMod>> coreMods = new HashMap<>();
  private static final Marker MARKER = MarkerManager.getMarker("SERVICE");

  public CoreModManager() {
    coreMods.put(Phase.BEFORE, new HashSet<>());
    coreMods.put(Phase.AFTER, new HashSet<>());
    coreMods.get(Phase.AFTER).add(new ResourceStreamFixer());
    coreMods.get(Phase.BEFORE).add(new ComparatorFixer());
    coreMods.get(Phase.BEFORE).add(new URLTransformer());
    coreMods.get(Phase.BEFORE).add(new AssetReflux());
    coreMods.get(Phase.BEFORE).add(new GameDirChanger());
    String preMods = coreMods.get(Phase.BEFORE).stream().map(CoreMod::name).collect(Collectors.joining(", ", "[", "]"));
    String postMods = coreMods.get(Phase.AFTER).stream().map(CoreMod::name).collect(Collectors.joining(", ", "[", "]"));
    CoreMod.LOGGER.info(MARKER, "Launching with the following coremods: BEFORE: {}, AFTER: {}", preMods, postMods);
  }

  @Override
  public String name() {
    return "coremodmanager";
  }

  @Override
  public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
    return EnumSet.of(Phase.BEFORE, Phase.AFTER);
  }

  @Override
  public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
    return coreMods.get(phase).stream().anyMatch(c -> c.processClass(phase, classNode, classType));
  }
}
