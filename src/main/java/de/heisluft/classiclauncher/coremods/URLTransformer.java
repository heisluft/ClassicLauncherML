package de.heisluft.classiclauncher.coremods;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class URLTransformer implements CoreMod {

  private static final Logger LOGGER = LogManager.getLogger();

  @Override
  public boolean processClass(ClassNode node, Type classType) {
    boolean dirty = false;
    for(MethodNode mn : node.methods) {
      for(AbstractInsnNode ain : mn.instructions) {
        if(!(ain instanceof LdcInsnNode) && !(ain instanceof InvokeDynamicInsnNode)) continue;
        if(ain instanceof InvokeDynamicInsnNode idin) {
          Handle bsm = idin.bsm;
          if(!"java/lang/invoke/StringConcatFactory".equals(bsm.getOwner())) continue;
          if(!"makeConcatWithConstants".equals(bsm.getName())) continue;
          if(idin.bsmArgs.length > 1) continue;
          if(!(idin.bsmArgs[0] instanceof String recipe)) continue;
          if(!recipe.startsWith("http://")) continue;
          String[] split = recipe.split("\u0001", -1);

          if("/listmaps.jsp?user=".equals(split[1]) || "/level/load.html?id=".equals(split[1]) ||
              "/level/save.html".equals(split[1])) {
            LOGGER.info(MARKER, "Redirecting Level IO URL to mcspoof in " + node.name + "#" + mn.name + mn.desc);
            LOGGER.debug(MARKER, "Matched String: " + split[1]);
            split[1] = "/mcspoof" + split[1];
            idin.bsmArgs[0] = String.join("\u0001", split);
            dirty = true;
          }
          else if("http://www.minecraft.net/skin/".equals(split[0])) {
            LOGGER.info(MARKER, "Proxying Skin URL in " + node.name + "#" + mn.name + mn.desc);
            split[0] = "http://localhost/mcspoof/skin.php?skin=";
            idin.bsmArgs[0] = String.join("\u0001", split);
            dirty = true;
          }
          continue;
        }
        LdcInsnNode lin = ((LdcInsnNode) ain);
        if(!(lin.cst instanceof String)) continue;
        if("minecraft.net".equals(lin.cst)) {
          if(node.name.endsWith("/MinecraftApplet") && mn.name.equals("init") || mn.desc.equals("(Lcom/mojang/minecraft/level/Level;)V")) {
            LOGGER.info(MARKER, "Overriding documentHost check within " + node.name + "#" + mn.name + mn.desc);
            lin.cst = "localhost";
            dirty = true;
          }
        }
        if("/listmaps.jsp?user=".equals(lin.cst) || "/level/load.html?id=".equals(lin.cst) ||
            "/level/save.html".equals(lin.cst)) {
          LOGGER.info(MARKER, "Redirecting Level IO URL to mcspoof in " + node.name + "#" + mn.name + mn.desc);
          LOGGER.debug(MARKER, "Matched String: " + lin.cst);
          lin.cst = "/mcspoof" + lin.cst;
          dirty = true;
        }
        if("http://www.minecraft.net/skin/".equals(lin.cst)) {
          LOGGER.info(MARKER, "Proxying Skin URL in " + node.name + "#" + mn.name + mn.desc);
          lin.cst = "http://localhost/mcspoof/skin.php?skin=";
          dirty = true;
        }
        if("http://www.minecraft.net/".equals(lin.cst)) {
          LOGGER.info(MARKER, "Proxying Applet base URL in " + node.name + "#" + mn.name + mn.desc);
          lin.cst = "http://localhost";
          dirty = true;
        }
      }
    }
    return dirty;
  }
}