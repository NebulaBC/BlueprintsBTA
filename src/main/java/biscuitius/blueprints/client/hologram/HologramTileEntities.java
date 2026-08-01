package biscuitius.blueprints.client.hologram;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.gui.Screen;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.entity.TileEntityDispatcher;
import net.minecraft.core.block.entity.TileEntitySign;
import net.minecraft.core.block.motion.CarriedBlock;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;

public final class HologramTileEntities {
   private static World sessionWorld;
   private static int sessionX;
   private static int sessionY;
   private static int sessionZ;
   private static TileEntity sessionEntity;
   private static Screen sessionScreen;

   private HologramTileEntities() {
   }

   public static boolean isEntityBlock(int blockId) {
      if (blockId > 0 && blockId < Blocks.blocksList.length) {
         Block<?> block = Blocks.blocksList[blockId];
         return block != null && block.isEntityTile && block.entitySupplier != null;
      } else {
         return false;
      }
   }

   public static CompoundTag getTag(HologramBlock h) {
      return h != null && h.nbt instanceof CompoundTag ? (CompoundTag)h.nbt : null;
   }

   public static TileEntity instantiate(int x, int y, int z, HologramBlock h) {
      if (h != null && isEntityBlock(h.blockId)) {
         Block<?> block = Blocks.blocksList[h.blockId];
         TileEntity entity = null;
         CompoundTag tag = getTag(h);
         if (tag != null) {
            try {
               entity = TileEntityDispatcher.createAndLoadEntity(tag);
            } catch (Throwable ignored) {
               entity = null;
            }
         }

         if (entity == null) {
            try {
               entity = (TileEntity)block.entitySupplier.get();
            } catch (Throwable ignored) {
               return null;
            }
         }

         if (entity == null) {
            return null;
         }

         entity.worldObj = null;
         entity.tilePos = new TilePos(x, y, z);
         entity.validate();
         return entity;
      } else {
         return null;
      }
   }

   public static CompoundTag save(TileEntity entity) {
      CompoundTag tag = new CompoundTag();
      entity.writeToNBT(tag);
      return tag;
   }

   public static boolean tryOpenEditor(World world, PlayerLocal player, int x, int y, int z, HologramBlock h) {
      if (world != null && player != null) {
         TileEntity entity = instantiate(x, y, z, h);
         if (entity == null) {
            return false;
         }

         if (!isEditable(entity)) {
            return false;
         }

         Block<?> block = Blocks.blocksList[h.blockId];
         if (block != null) {
            entity.carriedBlock = new CarriedBlock(player, block, h.metadata, null);
         }

         sessionWorld = world;
         sessionX = x;
         sessionY = y;
         sessionZ = z;
         sessionEntity = entity;
         if (entity instanceof TileEntitySign) {
            player.displaySignEditorScreen((TileEntitySign)entity);
            Minecraft mc = Minecraft.getMinecraft();
            sessionScreen = mc != null ? mc.currentScreen : null;
            return true;
         } else {
            clearSession();
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isEditable(TileEntity entity) {
      return entity instanceof TileEntitySign;
   }

   public static void flushOpenSession() {
      if (sessionEntity != null && sessionWorld != null) {
         HologramBlock current = HologramStore.get(sessionWorld, sessionX, sessionY, sessionZ);
         if (current != null) {
            try {
               CompoundTag tag = save(sessionEntity);
               HologramStore.put(sessionWorld, sessionX, sessionY, sessionZ, current.withNbt(tag));
            } catch (Throwable var2) {
            }
         }

         clearSession();
      } else {
         clearSession();
      }
   }

   public static boolean hasOpenSession() {
      return sessionEntity != null;
   }

   public static boolean isSessionScreen(Screen screen) {
      return sessionScreen != null && sessionScreen == screen;
   }

   private static void clearSession() {
      sessionWorld = null;
      sessionEntity = null;
      sessionScreen = null;
      sessionZ = 0;
      sessionY = 0;
      sessionX = 0;
   }
}
