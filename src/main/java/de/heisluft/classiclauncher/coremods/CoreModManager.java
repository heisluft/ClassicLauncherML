package de.heisluft.classiclauncher.coremods;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CoreModManager implements ILaunchPluginService {

  private final Map<Phase, Set<CoreMod>> coreMods = new HashMap<>();

  public CoreModManager() {
    coreMods.put(Phase.BEFORE, new HashSet<>());
    coreMods.put(Phase.AFTER, new HashSet<>());
    coreMods.get(Phase.AFTER).add(new ResourceStreamFixer());
    coreMods.get(Phase.BEFORE).add(new ComparatorFixer());
    coreMods.get(Phase.BEFORE).add(new URLTransformer());
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
