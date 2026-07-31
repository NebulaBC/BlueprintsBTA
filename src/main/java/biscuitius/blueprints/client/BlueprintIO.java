package biscuitius.blueprints.client;

import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramStore;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlueprintIO {
   private static final Logger LOGGER = LoggerFactory.getLogger("blueprints-io");
   private static final String DIR_NAME = "blueprints";
   private static final String EXTENSION = ".blueprint";
   private static final byte[] MAGIC = new byte[]{66, 80, 84};
   private static final byte VERSION = 1;

   private BlueprintIO() {
   }

   private static boolean isExcludedFromBlueprint(int blockId) {
      if (blockId == 0) {
         return true;
      } else if (blockId >= 0 && blockId < Blocks.blocksList.length) {
         Block<?> block = Blocks.blocksList[blockId];
         return block != null && block.getMaterial().isLiquid();
      } else {
         return false;
      }
   }

   public static File getDirectory() {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc == null) {
         return null;
      }

      File dir = new File(mc.getMinecraftDir(), "blueprints");
      if (!dir.exists() && !dir.mkdirs()) {
         LOGGER.warn("Failed to create blueprint directory: {}", dir);
      }

      return dir;
   }

   public static List<String> listNames() {
      File dir = getDirectory();
      if (dir != null && dir.isDirectory()) {
         File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".blueprint"));
         if (files != null && files.length != 0) {
            List<String> names = new ArrayList<>(files.length);

            for (File f : files) {
               String n = f.getName();
               names.add(n.substring(0, n.length() - ".blueprint".length()));
            }

            names.sort(String.CASE_INSENSITIVE_ORDER);
            return names;
         } else {
            return Collections.emptyList();
         }
      } else {
         return Collections.emptyList();
      }
   }

   public static boolean exists(String name) {
      File f = resolveFile(name);
      return f != null && f.isFile();
   }

   public static File resolveFile(String name) {
      if (name == null) {
         return null;
      }

      String trimmed = name.trim();
      if (trimmed.isEmpty()) {
         return null;
      }

      String safe = trimmed.replaceAll("[\\\\/:*?\"<>|]", "_");
      if (safe.isEmpty()) {
         return null;
      }

      File dir = getDirectory();
      return dir == null ? null : new File(dir, safe + ".blueprint");
   }

   public static boolean save(World world, String name) {
      if (world == null) {
         return false;
      }

      File file = resolveFile(name);
      if (file == null) {
         return false;
      }

      int[] bounds = HologramStore.getBounds(world);
      if (bounds == null) {
         LOGGER.warn("Refusing to save empty blueprint '{}'", name);
         return false;
      }

      int originX = bounds[0];
      int originY = bounds[1];
      int originZ = bounds[2];
      int[] counter = new int[]{0};
      HologramStore.forEach(world, (x, y, z, h) -> {
         if (!isExcludedFromBlueprint(h.blockId)) {
            counter[0]++;
         }
      });
      int count = counter[0];
      if (count == 0) {
         LOGGER.warn("Refusing to save empty blueprint '{}' (only air/liquid entries)", name);
         return false;
      }

      try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
         out.write(MAGIC);
         out.writeByte(1);
         out.writeInt(count);
         HologramStore.forEach(world, (x, y, z, h) -> {
            if (!isExcludedFromBlueprint(h.blockId)) {
               try {
                  out.writeInt(x - originX);
                  out.writeInt(y - originY);
                  out.writeInt(z - originZ);
                  out.writeShort((short)h.blockId);
                  out.writeShort((short)h.metadata);
               } catch (IOException ex) {
                  throw new RuntimeException(ex);
               }
            }
         });
         return true;
      } catch (Exception e) {
         LOGGER.warn("Failed to save blueprint '{}' to {}: {}", new Object[]{name, file, e.getMessage()});
         file.delete();
         return false;
      }
   }

   public static boolean saveRegion(World world, String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      if (world == null) {
         return false;
      }

      File file = resolveFile(name);
      if (file == null) {
         return false;
      }

      int loY = Math.max(0, minY);
      int hiY = Math.min(world.getHeightBlocks() - 1, maxY);
      if (loY > hiY) {
         return false;
      }

      int count = 0;

      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            for (int y = loY; y <= hiY; y++) {
               if (!isExcludedFromBlueprint(world.getBlockId(x, y, z))) {
                  count++;
               }
            }
         }
      }

      if (count == 0) {
         LOGGER.warn("Refusing to save empty region blueprint '{}' (only air/liquid in selection)", name);
         return false;
      }

      try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
         out.write(MAGIC);
         out.writeByte(1);
         out.writeInt(count);

         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               for (int y = loY; y <= hiY; y++) {
                  int id = world.getBlockId(x, y, z);
                  if (!isExcludedFromBlueprint(id)) {
                     int meta = world.getBlockMetadata(x, y, z);
                     out.writeInt(x - minX);
                     out.writeInt(y - minY);
                     out.writeInt(z - minZ);
                     out.writeShort((short)id);
                     out.writeShort((short)meta);
                  }
               }
            }
         }

         return true;
      } catch (Exception e) {
         LOGGER.warn("Failed to save region blueprint '{}' to {}: {}", new Object[]{name, file, e.getMessage()});
         file.delete();
         return false;
      }
   }

   public static boolean load(World world, Player anchor, String name) {
      if (world != null && anchor != null) {
         File file = resolveFile(name);
         if (file != null && file.isFile()) {
            int anchorX = floor(anchor.x);
            int anchorY = floor(anchor.y) - 1;
            int anchorZ = floor(anchor.z);

            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
               byte[] magic = new byte[3];
               in.readFully(magic);
               if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1] || magic[2] != MAGIC[2]) {
                  LOGGER.warn("Bad blueprint magic in {}", file);
                  return false;
               }

               int version = in.readByte() & 255;
               if (version != 1) {
                  LOGGER.warn("Unsupported blueprint version {} in {}", version, file);
                  return false;
               }

               int count = in.readInt();
               if (count < 0 || count > 16000000) {
                  LOGGER.warn("Bad blueprint entry count {} in {}", count, file);
                  return false;
               }

               HologramStore.clearWorld(world);

               for (int i = 0; i < count; i++) {
                  int rx = in.readInt();
                  int ry = in.readInt();
                  int rz = in.readInt();
                  int blockId = in.readShort() & '\uffff';
                  int metadata = in.readShort() & '\uffff';
                  int x = anchorX + rx;
                  int y = anchorY + ry;
                  int z = anchorZ + rz;
                  if (y >= 0 && y < 256) {
                     HologramBlock block = new HologramBlock(blockId, metadata);
                     HologramStore.put(world, x, y, z, block);
                  }
               }

               HologramStore.recomputeBounds(world);
               return true;
            } catch (Exception e) {
               LOGGER.warn("Failed to load blueprint '{}' from {}: {}", new Object[]{name, file, e.getMessage()});
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static int floor(double v) {
      int fi = (int)v;
      return v < fi ? fi - 1 : fi;
   }
}
