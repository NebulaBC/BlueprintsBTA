package biscuitius.blueprints.client.hologram;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.enums.LightLayer;
import net.minecraft.core.world.World;
import net.minecraft.core.world.chunk.ChunkCache;

public final class HologramChunkCache extends ChunkCache {
   private final World world;

   public HologramChunkCache(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      super(world, minX, minY, minZ, maxX, maxY, maxZ);
      this.world = world;
   }

   private HologramBlock visibleHologram(int x, int y, int z) {
      return !HologramAppearance.isYVisible(y) ? null : HologramStore.get(this.world, x, y, z);
   }

   public int getBlockId(int x, int y, int z) {
      HologramBlock h = this.visibleHologram(x, y, z);
      return h != null ? h.blockId : super.getBlockId(x, y, z);
   }

   public int getBlockMetadata(int x, int y, int z) {
      HologramBlock h = this.visibleHologram(x, y, z);
      return h != null ? h.metadata : super.getBlockMetadata(x, y, z);
   }

   public Block getBlock(int x, int y, int z) {
      HologramBlock h = this.visibleHologram(x, y, z);
      return h != null ? Blocks.getBlock(h.blockId) : super.getBlock(x, y, z);
   }

   public Material getBlockMaterial(int x, int y, int z) {
      HologramBlock h = this.visibleHologram(x, y, z);
      if (h != null) {
         Block<?> b = Blocks.blocksList[h.blockId];
         return b == null ? Material.air : b.getMaterial();
      } else {
         return super.getBlockMaterial(x, y, z);
      }
   }

   public boolean isBlockOpaqueCube(int x, int y, int z) {
      HologramBlock h = this.visibleHologram(x, y, z);
      if (h == null) {
         return super.isBlockOpaqueCube(x, y, z);
      } else {
         Block<?> b = Blocks.blocksList[h.blockId];
         return b != null && b.isSolidRender();
      }
   }

   public boolean isBlockNormalCube(int x, int y, int z) {
      HologramBlock h = this.visibleHologram(x, y, z);
      if (h == null) {
         return super.isBlockNormalCube(x, y, z);
      } else {
         Block<?> b = Blocks.blocksList[h.blockId];
         return b != null && b.getMaterial().blocksMotion() && b.isCubeShaped();
      }
   }

   public int getLightmapCoord(int x, int y, int z, int blockLightValue) {
      return this.getLightmapCoord(15, 15);
   }

   public int getLightmapCoord(int skylight, int blocklight) {
      return super.getLightmapCoord(15, 15);
   }

   public float getBrightness(int x, int y, int z, int blockLightValue) {
      return super.getBrightness(x, y, z, 15);
   }

   public float getLightBrightness(int x, int y, int z) {
      return super.getBrightness(x, y, z, 15);
   }

   public int getLightValue(int x, int y, int z) {
      return 15;
   }

   public int getLightValueExt(int x, int y, int z, boolean first) {
      return 15;
   }

   public int getSavedLightValue(LightLayer layer, int x, int y, int z) {
      return 15;
   }
}
