package de.heisluft.classiclauncher.coremods;

import de.heisluft.classiclauncher.LaunchHandlerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class AssetReflux implements CoreMod {

  private static final Logger LOGGER = LogManager.getLogger();
  private boolean didWork = false;

  @Override
  public boolean processClass(ClassNode node, Type classType) {
    if(didWork || !"java/lang/Thread".equals(node.superName)) return false;

    // Check if we have exactly 3 fields, one being Boolean, one being File to increase hit chance
    if(node.fields.size() != 3) return false;
    boolean foundBoolean = false, foundFile = false;
    for(FieldNode fn : node.fields) {
      if(fn.desc.equals("Ljava/io/File;")) {
        if(foundFile) return false; // Exactly one File field
        foundFile = true;
      }
      if(fn.desc.equals("Z")) {
        if(foundBoolean) return false; // Exactly one boolean field
        foundBoolean = true;
      }
    }
    if(!foundBoolean || !foundFile) return false;
    LOGGER.info(MARKER, "Found the BackgroundDownload Thread");
    String mcVersion = LaunchHandlerService.mcVersion;
    boolean newSoundSystem = mcVersion.startsWith("in-2010");
    // We can do this because indev timestamps make strings comparable and 'c' prefix is before 'i'
    boolean loadNewSounds = mcVersion.compareTo("in-20100111-1") >= 0;

    // The name of the soundManager field within Minecraft.class
    String soundManagerFieldName = null;
    // The name of the SoundManager#addSound(File, String) method
    String soundManagerAddSoundMethodName = null;
    // The name of the SoundManager#addSound(String, File) method
    String soundManagerAddMusicMethodName = null;

    // We need those to push them onto the stack for our callback invocation
    String mcFieldName = "", mcFieldDesc = "";
    // This is simply a safeguard to validate our functions.
    String smClassName = "";

    //The remaining field is the minecraft field, but we neither know the Minecraft class nor the
    //mc field name, so we look them up directly
    for(FieldNode fn : node.fields) {
      if(!fn.desc.equals("Ljava/io/File;") && !fn.desc.equals("Z")) {
        mcFieldDesc = fn.desc;
        mcFieldName = fn.name;
      }
    }

    for(MethodNode mn : node.methods) {
      if(mn.name.equals("<init>")) {
        AbstractInsnNode start = null;
        for(AbstractInsnNode ain : mn.instructions) {
          if(ain.getOpcode() != Opcodes.INVOKESPECIAL) continue;
          MethodInsnNode min = (MethodInsnNode) ain;
          if(!min.owner.equals("java/io/File")) continue;
          if(!min.name.equals("<init>")) continue;
          if(!min.desc.equals("(Ljava/io/File;Ljava/lang/String;)V")) continue;
          AbstractInsnNode ldc = ain.getPrevious();
          if(ldc.getOpcode() != Opcodes.LDC) continue;
          if(!"resources/".equals(((LdcInsnNode)ldc).cst)) continue;
          AbstractInsnNode aload1 = ldc.getPrevious();
          if(aload1.getOpcode() != Opcodes.ALOAD) continue;
          if(((VarInsnNode)aload1).var != 1) continue;
          AbstractInsnNode dup = aload1.getPrevious();
          if(dup.getOpcode() != Opcodes.DUP) continue;
          AbstractInsnNode _new = dup.getPrevious();
          if(_new.getOpcode() != Opcodes.NEW) continue;
          if(!((TypeInsnNode)_new).desc.equals("java/io/File")) continue;
          start = _new;
          break;
        }
        if(start == null) continue;
        AbstractInsnNode next = start;
        LOGGER.info(MARKER, "Found the workdir Field set, changing to Callbacks.assetsDir (currently '" + LaunchHandlerService.assetsDir.toAbsolutePath() + "')");
        for(int i = 0; i < 5; i++) {
          start = next;
          next = start.getNext();
          mn.instructions.remove(start);
        }
        mn.instructions.insertBefore(next, new MethodInsnNode(Opcodes.INVOKESTATIC, CALLBACK_CLASSNAME, "assetsDir", "()Ljava/io/File;"));
      }
      if(mn.name.equals("run")) {
        for(AbstractInsnNode ain : mn.instructions) {
          // There are two getfield references, one for the addMusic and one for the addSound call
          if(ain.getOpcode() == Opcodes.GETFIELD) {
            FieldInsnNode fin = (FieldInsnNode) ain;
            if(fin.owner.equals(mcFieldDesc.substring(1, mcFieldDesc.length() -1))) {
              // we only need to set this once. This is the soundManager field of the Minecraft class
              // we cache it so that we can obtain an instance to invoke the methods on
              if(soundManagerFieldName == null) {
                soundManagerFieldName = fin.name;
                LOGGER.info(MARKER, "Found the Sound manager field get.");
                LOGGER.info(MARKER, "MC field name: " + mcFieldName);
                LOGGER.info(MARKER, "MC field desc: " + mcFieldDesc);
                LOGGER.info(MARKER, "SoundManager field name: " + fin.name);
                LOGGER.info(MARKER, "SoundManager field desc: " + fin.desc);
                smClassName = fin.desc.substring(1, fin.desc.length() - 1);
              }
              // Skip all those other aloads
              AbstractInsnNode next = ain.getNext();
              while(!(next instanceof MethodInsnNode theCall)) next = next.getNext();

              //sanity check that the called method belongs to SoundManager
              if(!smClassName.equals(theCall.owner)) continue;

              if("(Ljava/io/File;Ljava/lang/String;)V".equals(theCall.desc)) {
                LOGGER.info(MARKER, "Found the addSound method. (" + theCall.name + ")");
                soundManagerAddSoundMethodName = theCall.name;
              } else if(soundManagerAddSoundMethodName == null && newSoundSystem) {
                LOGGER.info(MARKER, "Found the addSound method. (" + theCall.name + ")");
                soundManagerAddSoundMethodName = theCall.name;
              } else {
                LOGGER.info(MARKER, "Found the addMusic method. (" + theCall.name + ")");
                soundManagerAddMusicMethodName = theCall.name;
              }
            }
          }
        }
        // If our heuristics failed, its better not to modify the bytecode at all
        if(soundManagerFieldName != null && soundManagerAddSoundMethodName != null && soundManagerAddMusicMethodName != null) {
          LOGGER.info(MARKER, "CoreMod Setup successful! now inserting the proxy call");
          mn.instructions.clear();
          mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
          mn.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, mcFieldName, mcFieldDesc));
          mn.instructions.add(new LdcInsnNode(soundManagerFieldName));
          mn.instructions.add(new LdcInsnNode(soundManagerAddSoundMethodName));
          mn.instructions.add(new LdcInsnNode(soundManagerAddMusicMethodName));
          mn.instructions.add(new InsnNode(newSoundSystem ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
          mn.instructions.add(new InsnNode(loadNewSounds ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
          mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CALLBACK_CLASSNAME, "callback", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V"));
          mn.instructions.add(new InsnNode(Opcodes.RETURN));
          mn.tryCatchBlocks.clear();
          mn.localVariables = null;
        }
      }
    }
    didWork = true;
    return true;
  }
}