package biscuitius.blueprints.client;

import com.mojang.nbt.NbtIo;
import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlueprintsCacheManager {
   private static final Logger LOGGER = LoggerFactory.getLogger("blueprints-cache");
   private static final String CACHE_DIR = "config/blueprints-cache";
   private static boolean dirty;
   private static String lastServerHost;
   private static int lastServerPort;

   private BlueprintsCacheManager() {
   }

   public static void setLastServerAddress(String host, int port) {
      lastServerHost = host;
      lastServerPort = port;
   }

   public static void markDirty() {
      dirty = true;
   }

   public static void tickAutoSave(Minecraft mc) {
      if (dirty && mc != null && mc.currentWorld != null) {
         dirty = false;
         save(mc);
      }
   }

   public static void save(Minecraft mc) {
      if (mc != null && mc.currentWorld != null) {
         String cacheKey = buildCacheKey(mc);
         if (cacheKey != null) {
            File dir = new File(mc.getMinecraftDir(), "config/blueprints-cache");
            if (!dir.exists() && !dir.mkdirs()) {
               LOGGER.warn("Failed to create cache directory: {}", dir);
            } else {
               File file = new File(dir, cacheKey + ".dat");
               CompoundTag root = new CompoundTag();
               writeInventory(root);
               writeGhostBlocks(root, mc.currentWorld);
               writeSignTexts(root);
               if (!root.containsKey("Inventory") && !root.containsKey("GhostBlocks") && !root.containsKey("SignTexts")) {
                  if (file.exists()) {
                     file.delete();
                  }
               } else {
                  try (FileOutputStream fos = new FileOutputStream(file)) {
                     NbtIo.writeCompressed(root, fos);
                  } catch (Exception var18) {
                     LOGGER.warn("Failed to save cache to {}: {}", file.getName(), var18.getMessage());
                  }

                  dirty = false;
               }
            }
         }
      }
   }

   private static void writeInventory(CompoundTag root) {
      PlayerLocal designPlayer = DesignModeState.getDesignPlayer();
      if (designPlayer != null) {
         CompoundTag inv = new CompoundTag();
         ListTag mainList = new ListTag();

         for (int i = 0; i < designPlayer.inventory.mainInventory.length; i++) {
            ItemStack stack = designPlayer.inventory.mainInventory[i];
            if (stack != null) {
               CompoundTag slot = new CompoundTag();
               slot.putByte("Slot", (byte)i);
               stack.writeToNBT(slot);
               mainList.addTag(slot);
            }
         }

         inv.putList("Main", mainList);
         ListTag armorList = new ListTag();

         for (int ix = 0; ix < designPlayer.inventory.armorInventory.length; ix++) {
            ItemStack stack = designPlayer.inventory.armorInventory[ix];
            if (stack != null) {
               CompoundTag slot = new CompoundTag();
               slot.putByte("Slot", (byte)ix);
               stack.writeToNBT(slot);
               armorList.addTag(slot);
            }
         }

         inv.putList("Armor", armorList);
         inv.putInt("CurrentItem", designPlayer.inventory.getCurrentItemIndex());
         inv.putInt("HotbarOffset", designPlayer.inventory.getHotbarOffset());
         root.putCompound("Inventory", inv);
      }
   }

   private static void writeGhostBlocks(CompoundTag root, World world) {
      if (GhostBlockState.hasEntries(world)) {
         ListTag blockList = new ListTag();
         GhostBlockState.forEachEntry(world, (x, y, z, serverId, serverMeta, ghostId, ghostMeta) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", x);
            entry.putInt("y", y);
            entry.putInt("z", z);
            entry.putShort("sId", (short)serverId);
            entry.putShort("sMeta", (short)serverMeta);
            entry.putShort("gId", (short)ghostId);
            entry.putShort("gMeta", (short)ghostMeta);
            blockList.addTag(entry);
         });
         root.putList("GhostBlocks", blockList);
      }
   }

   public static void load(Minecraft mc) {
      if (mc != null && mc.currentWorld != null) {
         String cacheKey = buildCacheKey(mc);
         if (cacheKey != null) {
            File dir = new File(mc.getMinecraftDir(), "config/blueprints-cache");
            File file = new File(dir, cacheKey + ".dat");
            if (file.exists()) {
               CompoundTag root;
               try (FileInputStream fis = new FileInputStream(file)) {
                  root = NbtIo.readCompressed(fis);
               } catch (Exception var18) {
                  LOGGER.warn("Failed to load cache from {}: {}", file.getName(), var18.getMessage());
                  return;
               }

               readInventory(root);
               readGhostBlocks(root, mc.currentWorld);
               readSignTexts(root);
               dirty = false;
            }
         }
      }
   }

   private static void readInventory(CompoundTag root) {
      if (root.containsKey("Inventory")) {
         CompoundTag inv = root.getCompound("Inventory");
         ItemStack[] main = new ItemStack[36];
         ListTag mainList = inv.getList("Main");

         for (int i = 0; i < mainList.tagCount(); i++) {
            CompoundTag slot = (CompoundTag)mainList.tagAt(i);
            int idx = slot.getByte("Slot") & 255;
            if (idx < main.length) {
               ItemStack stack = ItemStack.readItemStackFromNbt(slot);
               if (stack != null) {
                  main[idx] = stack;
               }
            }
         }

         ItemStack[] armor = new ItemStack[4];
         ListTag armorList = inv.getList("Armor");

         for (int ix = 0; ix < armorList.tagCount(); ix++) {
            CompoundTag slot = (CompoundTag)armorList.tagAt(ix);
            int idx = slot.getByte("Slot") & 255;
            if (idx < armor.length) {
               ItemStack stack = ItemStack.readItemStackFromNbt(slot);
               if (stack != null) {
                  armor[idx] = stack;
               }
            }
         }

         int currentItem = inv.getInteger("CurrentItem");
         int hotbarOffset = inv.getInteger("HotbarOffset");
         DesignModeState.setPendingInventory(main, armor, currentItem, hotbarOffset);
      }
   }

   private static void readGhostBlocks(CompoundTag root, World world) {
      if (root.containsKey("GhostBlocks")) {
         ListTag blockList = root.getList("GhostBlocks");
         if (blockList.tagCount() != 0) {
            List<int[]> entries = new ArrayList<>(blockList.tagCount());

            for (int i = 0; i < blockList.tagCount(); i++) {
               CompoundTag entry = (CompoundTag)blockList.tagAt(i);
               entries.add(
                  new int[]{
                     entry.getInteger("x"),
                     entry.getInteger("y"),
                     entry.getInteger("z"),
                     entry.getShort("sId"),
                     entry.getShort("sMeta"),
                     entry.getShort("gId"),
                     entry.getShort("gMeta")
                  }
               );
            }

            GhostBlockState.setPendingEntries(entries);
            GhostBlockState.applyPendingEntries(world);
         }
      }
   }

   private static void writeSignTexts(CompoundTag root) {
      if (SignTextCache.hasEntries()) {
         ListTag list = new ListTag();
         SignTextCache.forEachEntry((x, y, z, text, picture, color) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", x);
            entry.putInt("y", y);
            entry.putInt("z", z);
            entry.putString("Text1", text[0]);
            entry.putString("Text2", text[1]);
            entry.putString("Text3", text[2]);
            entry.putString("Text4", text[3]);
            entry.putInt("Picture", picture);
            entry.putInt("Color", color);
            list.addTag(entry);
         });
         root.putList("SignTexts", list);
      }
   }

   private static void readSignTexts(CompoundTag root) {
      if (root.containsKey("SignTexts")) {
         ListTag list = root.getList("SignTexts");
         if (list.tagCount() != 0) {
            List<SignTextCache.SignData> entries = new ArrayList<>(list.tagCount());

            for (int i = 0; i < list.tagCount(); i++) {
               CompoundTag entry = (CompoundTag)list.tagAt(i);
               String[] text = new String[]{entry.getString("Text1"), entry.getString("Text2"), entry.getString("Text3"), entry.getString("Text4")};
               entries.add(
                  new SignTextCache.SignData(
                     entry.getInteger("x"),
                     entry.getInteger("y"),
                     entry.getInteger("z"),
                     text,
                     entry.getIntegerOrDefault("Picture", 0),
                     entry.getIntegerOrDefault("Color", 15)
                  )
               );
            }

            SignTextCache.setPendingEntries(entries);
            SignTextCache.applyPendingEntries();
         }
      }
   }

   static String buildCacheKey(Minecraft mc) {
      if (mc.isMultiplayerWorld()) {
         return lastServerHost != null && !lastServerHost.isEmpty() ? "mp_" + sanitize(lastServerHost) + "_" + lastServerPort : null;
      } else {
         World world = mc.currentWorld;
         if (world != null && world.getLevelData() != null) {
            String worldName = world.getLevelData().getWorldName();
            long seed = world.getLevelData().getRandomSeed();
            return "sp_" + sanitize(worldName) + "_" + seed;
         } else {
            return null;
         }
      }
   }

   private static String sanitize(String s) {
      return s == null ? "unknown" : s.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
   }
}
