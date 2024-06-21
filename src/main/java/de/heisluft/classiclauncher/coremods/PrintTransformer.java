package de.heisluft.classiclauncher.coremods;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

public class PrintTransformer implements CoreMod {
  private static final Logger LOG = LogManager.getLogger();
  @Override
  public boolean processClass(ClassNode node, Type classType) {
    boolean dirty = false;
    outer:
    for(MethodNode method : node.methods) {
      for(AbstractInsnNode ain : method.instructions) {
        if(ain.getOpcode() != GETSTATIC) continue;
        FieldInsnNode fin = (FieldInsnNode) ain;
        if(!"java/lang/System".equals(fin.owner)) continue;
        if("err".equals(fin.name) || "out".equals(fin.name)) {
          AbstractInsnNode next = ain.getNext();
          while(next.getOpcode() != INVOKEVIRTUAL || !"java/io/PrintStream".equals(((MethodInsnNode) next).owner)) {
            if(next.getNext() == null) {
              break outer;
            }
            next = next.getNext();
          }
          MethodInsnNode min =(MethodInsnNode)next;
          if(!"println".equals(min.name)) continue;
          dirty = true;
          LOG.debug(MARKER, "found call in '{}#{}'", node.name, method.name);
          if(!"(Ljava/lang/String;)V".equals(min.desc)) method.instructions.insertBefore(min, new MethodInsnNode(INVOKESTATIC, "java/lang/String", "valueOf", min.desc.replaceAll("\\)V$", ")Ljava/lang/String;"), false));
          method.instructions.set(fin, new MethodInsnNode(INVOKESTATIC, "org/apache/logging/log4j/LogManager", "getLogger", "()Lorg/apache/logging/log4j/Logger;", false));
          min.desc = "(Ljava/lang/Object;)V";
          min.itf = true;
          min.name = "err".equals(fin.name) ? "warn" : "info";
          min.owner = "org/apache/logging/log4j/Logger";
          min.setOpcode(INVOKEINTERFACE);
        }
      }
    }
    if(dirty) LOG.info(MARKER, "Transforming println calls in {}", classType.getInternalName());
    return dirty;
  }
}
