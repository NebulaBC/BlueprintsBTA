package biscuitius.blueprints.client;

import biscuitius.blueprints.client.hologram.HologramBlock;
import biscuitius.blueprints.client.hologram.HologramStore;
import biscuitius.blueprints.client.hologram.HologramTileEntities;
import com.mojang.nbt.NbtIo;
import com.mojang.nbt.tags.CompoundTag;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlueprintIO {
   private static final Logger LOGGER = LoggerFactory.getLogger("blueprints-io");
   private static final String DIR_NAME = "blueprints";
   private static final String EXT = ".blueprint";
   private static final byte[] MAGIC = new byte[]{66, 80, 84};
   private static final byte VERSION_MIN = 1;
   private static final byte VERSION_MAX = 2;
   private static final byte WRITE_VERSION = 2;

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
            TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

            for (File f : files) {
               String n = f.getName();
               names.add(n.substring(0, n.length() - ".blueprint".length()));
            }

            return new ArrayList<>(names);
         } else {
            return Collections.emptyList();
         }
      } else {
         return Collections.emptyList();
      }
   }

   public static boolean exists(String name) {
      File file = resolveFile(name);
      return file != null && file.isFile();
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

   public static boolean delete(String name) {
      File file = resolveFile(name);
      return file != null && file.isFile() && file.delete();
   }

   public static boolean save(World world, String name) {
      if (world == null) {
         return false;
      } else {
         File file = resolveFile(name);
         if (file == null) {
            return false;
         } else {
            int[] bounds = HologramStore.getBounds(world);
            if (bounds == null) {
               LOGGER.warn("Refusing to save empty blueprint '{}'", name);
               return false;
            } else {
               int minX = bounds[0];
               int minY = bounds[1];
               int minZ = bounds[2];
               List<BlueprintIO.Entry> entries = new ArrayList<>();
               HologramStore.forEach(world, (x, y, z, h) -> {
                  if (!isExcludedFromBlueprint(h.blockId)) {
                     entries.add(new BlueprintIO.Entry(x - minX, y - minY, z - minZ, h.blockId, h.metadata, HologramTileEntities.getTag(h)));
                  }
               });
               if (entries.isEmpty()) {
                  LOGGER.warn("Refusing to save empty blueprint '{}' (only air/liquid entries)", name);
                  return false;
               } else {
                  return writeBlueprint(file, entries, name);
               }
            }
         }
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
      if (loY <= hiY && minX <= maxX && minZ <= maxZ) {
         List<BlueprintIO.Entry> entries = new ArrayList<>();

         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               for (int y = loY; y <= hiY; y++) {
                  HologramBlock block = HologramStore.sanitize(world.getBlockId(x, y, z), world.getBlockMetadata(x, y, z));
                  if (block != null && !isExcludedFromBlueprint(block.blockId)) {
                     CompoundTag teTag = null;

                     try {
                        TileEntity te = world.getTileEntity(new TilePos(x, y, z));
                        if (te != null) {
                           teTag = new CompoundTag();
                           te.writeToNBT(teTag);
                        }
                     } catch (Throwable ignored) {
                        teTag = null;
                     }

                     entries.add(new BlueprintIO.Entry(x - minX, y - minY, z - minZ, block.blockId, block.metadata, teTag));
                  }
               }
            }
         }

         if (entries.isEmpty()) {
            LOGGER.warn("Refusing to save empty region blueprint '{}' (only air/liquid in selection)", name);
            return false;
         } else {
            return writeBlueprint(file, entries, name);
         }
      } else {
         return false;
      }
   }

   private static boolean writeBlueprint(File file, List<BlueprintIO.Entry> entries, String name) {
      try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
         out.write(MAGIC);
         out.writeByte(2);
         out.writeInt(entries.size());

         for (BlueprintIO.Entry e : entries) {
            out.writeInt(e.rx());
            out.writeInt(e.ry());
            out.writeInt(e.rz());
            out.writeShort(e.blockId());
            out.writeShort(e.metadata());
            if (e.teTag() != null) {
               out.writeByte(1);
               NbtIo.write(e.teTag(), out);
            } else {
               out.writeByte(0);
            }
         }

         return true;
      } catch (Exception e) {
         LOGGER.warn("Failed to save blueprint '{}' to {}: {}", new Object[]{name, file, e.getMessage()});
         file.delete();
         return false;
      }
   }

   public static boolean load(World world, Player anchor, String name) {
      if (world != null && anchor != null) {
         int anchorX = floor(anchor.x);
         int anchorY = floor(anchor.y) - 1;
         int anchorZ = floor(anchor.z);
         File file = resolveFile(name);
         if (file != null && file.isFile()) {
            return loadBlueprint(world, file, name, anchorX, anchorY, anchorZ);
         }

         LOGGER.warn("No blueprint file found for '{}'", name);
         return false;
      } else {
         return false;
      }
   }

   private static boolean loadBlueprint(World world, File file, String name, int anchorX, int anchorY, int anchorZ) {
      try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
         byte[] magic = new byte[3];
         in.readFully(magic);
         if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1] || magic[2] != MAGIC[2]) {
            LOGGER.warn("Bad blueprint magic in {}", file);
            return false;
         }

         int version = in.readByte() & 255;
         if (version < 1 || version > 2) {
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
            CompoundTag teTag = null;
            if (version >= 2) {
               boolean hasNbt = in.readByte() != 0;
               if (hasNbt) {
                  teTag = NbtIo.read(in);
               }
            }

            int x = anchorX + rx;
            int y = anchorY + ry;
            int z = anchorZ + rz;
            if (y >= 0 && y < 256) {
               HologramStore.put(world, x, y, z, HologramStore.sanitize(new HologramBlock(blockId, metadata, teTag)));
            }
         }

         HologramStore.recomputeBounds(world);
         return true;
      } catch (Exception e) {
         LOGGER.warn("Failed to load blueprint '{}' from {}: {}", new Object[]{name, file, e.getMessage()});
         return false;
      }
   }

   private static int floor(double v) {
      int fi = (int)v;
      return v < fi ? fi - 1 : fi;
   }

   private record Entry(int rx, int ry, int rz, int blockId, int metadata, CompoundTag teTag) {
   }
}
