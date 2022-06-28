package de.heisluft.classiclauncher.coremods;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ResourceStreamFixer implements CoreMod {

  private static final Marker MARKER = MarkerManager.getMarker("RESOURCESTREAMFIXER");

  @Override
  public boolean processClass(ILaunchPluginService.Phase phase, ClassNode node, Type classType) {
    boolean didWork = false;
    for(MethodNode mn : node.methods) {
      for(AbstractInsnNode ain : mn.instructions) {
        if(ain.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
        MethodInsnNode min = ((MethodInsnNode) ain);
        if(!min.owner.equals("java/lang/Class")) continue;
        if(min.name.equals("getResourceAsStream")) {
          min.owner = CALLBACK_CLASSNAME;
          min.desc = "(Ljava/lang/Class;Ljava/lang/String;)Ljava/io/InputStream;";
          min.setOpcode(Opcodes.INVOKESTATIC);
          LOGGER.info(MARKER, "Inserted Proxy call into {}#{}{}", node.name, mn.name, mn.desc);
          didWork = true;
        }
      }
    }
    return didWork;
  }
}
