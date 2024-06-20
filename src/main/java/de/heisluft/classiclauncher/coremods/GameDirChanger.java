package de.heisluft.classiclauncher.coremods;

import de.heisluft.classiclauncher.LaunchHandlerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class GameDirChanger implements CoreMod {
  private static final Logger LOGGER = LogManager.getLogger();
  private boolean didWork;

  @Override
  public boolean processClass(ClassNode node, Type classType) {
    if(didWork || !node.interfaces.contains("java/lang/Runnable")) return false;

    MethodNode runMethod = null;
    for(final MethodNode methodNode : node.methods) {
      if("run".equals(methodNode.name)) {
        runMethod = methodNode;
        break;
      }
    }
    if(runMethod == null) {
      // WTF? We got no run method in a runnable lol
      return false;
    }

    LabelNode lastJump = null;
    int var = -1;
    {
      boolean foundTSwitch = false;
      for(AbstractInsnNode instruction : runMethod.instructions) {
        if(instruction.getOpcode() == Opcodes.TABLESWITCH) {
          foundTSwitch = true;
        }
        if(foundTSwitch) {
          if(instruction.getOpcode() == Opcodes.GOTO) {
            lastJump = ((JumpInsnNode) instruction).label;
            AbstractInsnNode ain = lastJump.getPrevious();
            while(ain.getOpcode() != Opcodes.ASTORE) ain = ain.getPrevious();
            var = ((VarInsnNode) ain).var;
            break;
          }
        }
      }
    }
    if(lastJump == null) return false; // Was not the desired class

    LOGGER.info(MARKER, "Found the workDir switch within class " + node.name);
    LOGGER.debug(MARKER, "Last Label is " + lastJump.getLabel() + ", instruction number " + runMethod.instructions.indexOf(lastJump));
    LOGGER.debug(MARKER, "File var is " + var);
    LOGGER.info(MARKER, "Inserting call to Callbacks.gameDir (currently: '" + LaunchHandlerService.gameDir.toAbsolutePath() + "')");

    runMethod.instructions.insert(lastJump, new VarInsnNode(Opcodes.ASTORE, var));
    runMethod.instructions.insert(lastJump, new MethodInsnNode(Opcodes.INVOKESTATIC, CALLBACK_CLASSNAME, "gameDir", "()Ljava/io/File;"));

    didWork = true;
    return true;
  }
}