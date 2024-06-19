package de.heisluft.classiclauncher.coremods;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Arrays;

/**
 * Ad-hoc core mod to bypass the authentication check.
 */
// TODO: Check whether Auth actually exists instead of blindly setting the level field
public class UserCheckBypass  implements CoreMod{

  private String mcClassName;
  private static final Logger LOG = LogManager.getLogger();

  @Override
  public boolean processClass(ILaunchPluginService.Phase phase, ClassNode node, Type classType) {
    if(mcClassName == null && !"net/minecraft/client/MinecraftApplet".equals(node.name)) return false;
    if(mcClassName == null) {
      for(FieldNode field : node.fields) {
        if(field.desc.startsWith("Ljava/")) continue;
        mcClassName = field.desc.substring(1, field.desc.length()-1);
        LOG.info(MARKER, "found the MC class name whithin the applet, its " + mcClassName);
        return false;
      }
      LOG.warn(MARKER, "Could not find MC Class name from Applet. Mod will not run.");
      return false;
    }
    if(!mcClassName.equals(node.name)) return false;

    for(MethodNode method : node.methods) {
      // filter out instance void methods
      if(!method.desc.endsWith(")V") || (method.access | Opcodes.ACC_STATIC) == method.access) continue;

      Type[] argTypes = Type.getArgumentTypes(method.desc);
      // with one arg of a type in the net.minecraft package but not in the client package
      if(argTypes.length != 1) continue;
      String maybeLevelClassName = argTypes[0].getInternalName();
      if(!maybeLevelClassName.startsWith("net/minecraft/") || maybeLevelClassName.startsWith("net/minecraft/client/")) continue;
      LOG.info(MARKER, "Found likely level class " + maybeLevelClassName + ", checking if field of type exists");
      FieldNode levelField = null;
      for(FieldNode fieldNode : node.fields) if(("L" + maybeLevelClassName + ";").equals(fieldNode.desc)) levelField = fieldNode;
      if(levelField == null) {
        LOG.info(MARKER, "No such field exists, continuing search");
        continue;
      }
      LOG.info(MARKER, "Field " + levelField.name + " matches, transforming");
      method.instructions.insert(new FieldInsnNode(Opcodes.PUTFIELD, mcClassName, levelField.name, levelField.desc));
      method.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 1));
      method.instructions.insert(new VarInsnNode(Opcodes.ALOAD, 0));
      return true;
    }
    return false;
  }
}
