package biscuitius.blueprints.client.hologram;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.block.material.Materials;
import net.minecraft.core.enums.LightLayer;
import net.minecraft.core.world.World;
import net.minecraft.core.world.chunk.ChunkCache;
import net.minecraft.core.world.pos.TilePosc;

public abstract class OverlayChunkCache extends ChunkCache {
   protected final World world;
   private static final byte FULLBRIGHT = -1;

   protected OverlayChunkCache(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      super(world, minX, minY, minZ, maxX, maxY, maxZ);
      this.world = world;
   }

   protected abstract HologramBlock overlay(int var1, int var2, int var3);

   private HologramBlock overlay(TilePosc pos) {
      return this.overlay(pos.x(), pos.y(), pos.z());
   }

   public Block<?> getBlockType(TilePosc pos) {
      HologramBlock h = this.overlay(pos);
      return h != null ? Blocks.getBlock(h.blockId) : super.getBlockType(pos);
   }

   public int getBlockData(TilePosc pos) {
      HologramBlock h = this.overlay(pos);
      return h != null ? h.metadata : super.getBlockData(pos);
   }

   public Material getBlockMaterial(TilePosc pos) {
      HologramBlock h = this.overlay(pos);
      if (h != null) {
         Block<?> b = Blocks.blocksList[h.blockId];
         return b == null ? Materials.AIR : b.getMaterial();
      } else {
         return super.getBlockMaterial(pos);
      }
   }

   public boolean isBlockOpaqueCube(TilePosc pos) {
      HologramBlock h = this.overlay(pos);
      if (h == null) {
         return super.isBlockOpaqueCube(pos);
      }

      Block<?> b = Blocks.blocksList[h.blockId];
      return b != null && b.isSolidRender();
   }

   public boolean isBlockNormalCube(TilePosc pos) {
      HologramBlock h = this.overlay(pos);
      if (h == null) {
         return super.isBlockNormalCube(pos);
      }

      Block<?> b = Blocks.blocksList[h.blockId];
      return b != null && b.getMaterial().blocksMotion() && b.isCubeShaped();
   }

   public byte getLightIndex(TilePosc pos, int blockLightValue) {
      return -1;
   }

   public byte getSavedLightIndex(TilePosc pos) {
      return -1;
   }

   public float getBrightness(TilePosc pos, int blockLightValue) {
      return super.getBrightness(pos, 15);
   }

   public float getLightBrightness(TilePosc pos) {
      return super.getBrightness(pos, 15);
   }

   public int getSavedLightValue(LightLayer layer, TilePosc pos) {
      return 15;
   }
}
