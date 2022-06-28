package de.heisluft.classiclauncher.coremods;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.HashMap;

public class ComparatorFixer implements CoreMod {

  private static final Marker MARKER = MarkerManager.getMarker("COMPAREFIXER");

  @Override
  public boolean processClass(ILaunchPluginService.Phase phase, ClassNode node, Type classType) {
    if(node.interfaces.size() != 1 || !node.interfaces.get(0).equals("java/util/Comparator"))
      return false;

    MethodNode cmpNode = null;
    for(MethodNode method : node.methods) {
      if(method.name.equals("compare") && method.desc.equals("(Ljava/lang/Object;Ljava/lang/Object;)I")) {
        cmpNode = method;
        break;
      }
    }
    if(cmpNode == null) return false;
    String classDesc = "";
    FieldInsnNode playerFieldGet = null;
    FieldInsnNode chunkVisibleGet = null;
    AbstractInsnNode invokeInsn = null;
    boolean iconstM1 = false;
    for(AbstractInsnNode ain : cmpNode.instructions) {
      if(ain.getOpcode() == Opcodes.CHECKCAST && classDesc.length() == 0) {
        classDesc = ((TypeInsnNode) ain).desc;
      }
      if(ain.getOpcode() == Opcodes.GETFIELD) {
        FieldInsnNode fin = ((FieldInsnNode) ain);
        if(playerFieldGet == null && ain.getPrevious() instanceof VarInsnNode && ((VarInsnNode)ain.getPrevious()).var == 0) playerFieldGet = (FieldInsnNode) fin.clone(new HashMap<>());
        else if(chunkVisibleGet == null && fin.owner.equals(classDesc)) chunkVisibleGet = (FieldInsnNode) fin.clone(new HashMap<>());
      }
      if(ain.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode) ain).owner.equals(classDesc)) {
        invokeInsn = ain.clone(new HashMap<>());
      }
      if(ain.getOpcode() == Opcodes.ICONST_M1) iconstM1 = true;
    }
    if(classDesc.length() == 0 || playerFieldGet == null|| invokeInsn == null) return false;

    LOGGER.info(MARKER, "Fixing compare method of " + node.name);

    cmpNode.instructions.clear();

    cmpNode.instructions.insert(new InsnNode(Opcodes.IRETURN));
    cmpNode.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, CALLBACK_CLASSNAME, "compare", "(FFZZZ)I"));
    // since survival-test, the output is to be inverted - we detect that by checking if -1 or 1 are loaded
    // passing the result in as a boolean flag
    cmpNode.instructions.insert(new InsnNode(iconstM1 ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
    if(chunkVisibleGet == null) {
      cmpNode.instructions.insert(new InsnNode(Opcodes.ICONST_0));
      cmpNode.instructions.insert(new InsnNode(Opcodes.ICONST_0));
    } else {
      cmpNode.instructions.insert(chunkVisibleGet.clone(new HashMap<LabelNode, LabelNode>()));
      cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 4));
      cmpNode.instructions.insert(chunkVisibleGet.clone(new HashMap<LabelNode, LabelNode>()));
      cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 3));
    }
    cmpNode.instructions.insert(invokeInsn.clone(new HashMap<LabelNode, LabelNode>()));
    cmpNode.instructions.insert(playerFieldGet.clone(new HashMap<LabelNode, LabelNode>()));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 0));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 4));
    cmpNode.instructions.insert(invokeInsn.clone(new HashMap<LabelNode, LabelNode>()));
    cmpNode.instructions.insert(playerFieldGet.clone(new HashMap<LabelNode, LabelNode>()));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 0));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 3));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ASTORE, 4));
    cmpNode.instructions.insert(new TypeInsnNode(Opcodes.CHECKCAST, classDesc));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 2));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ASTORE, 3));
    cmpNode.instructions.insert(new TypeInsnNode(Opcodes.CHECKCAST, classDesc));
    cmpNode.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 1));
    return true;
  }
}
