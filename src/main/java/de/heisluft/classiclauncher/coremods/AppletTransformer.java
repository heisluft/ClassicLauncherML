package de.heisluft.classiclauncher.coremods;

import de.heisluft.classiclauncher.awt.AppletFake;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class AppletTransformer implements CoreMod {

  boolean hasTransformed = false;

  @Override
  public boolean processClass(ClassNode node, Type classType) {
    if (hasTransformed || !node.name.endsWith("/MinecraftApplet")) return false;
    hasTransformed = true;
    LogManager.getLogger(name()).info("Overriding Applet superclass");
    node.superName = Type.getInternalName(AppletFake.class);
    for (MethodNode method : node.methods) {
      if(!method.name.equals("<init>")) continue;
      for(AbstractInsnNode insn : method.instructions) {
        if(insn.getOpcode() != Opcodes.INVOKESPECIAL) continue;
        ((MethodInsnNode) insn).owner = Type.getInternalName(AppletFake.class);
      }
    }
    return true;
  }
}
